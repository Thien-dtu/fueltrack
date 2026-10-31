package com.example.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.core.database.dao.MaintenanceScheduleDao
import com.example.core.database.dao.ScheduleWithVehicle
import com.example.core.model.MaintenanceDueStatus
import com.example.core.model.MaintenancePolicy
import com.example.core.notification.MaintenanceNotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Sends maintenance notifications for date-based reminders.
 *
 * Dependencies arrive through Hilt instead of AppDatabase.getInstance(), so the
 * worker can be constructed with a test database. The audit path also joins the
 * vehicle in the same query: fetching schedules and then calling getVehicleById()
 * per schedule was N+1 queries on every daily run.
 */
@HiltWorker
class MaintenanceReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val scheduleDao: MaintenanceScheduleDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_SCHEDULE_ID = "key_schedule_id"
        const val KEY_REMINDER_TYPE = "key_reminder_type" // "UPCOMING" or "DUE"
    }

    override suspend fun doWork(): Result {
        val scheduleId = inputData.getLong(KEY_SCHEDULE_ID, -1L)
        return if (scheduleId == -1L) {
            auditAllSchedules()
        } else {
            handleScheduledReminder(scheduleId)
        }
    }

    /** Periodic audit across every active schedule and vehicle. */
    private suspend fun auditAllSchedules(): Result {
        val now = System.currentTimeMillis()

        for (row in scheduleDao.getActiveSchedulesWithVehicle()) {
            val status = statusFor(row, now) ?: continue
            notify(row, status)
        }
        return Result.success()
    }

    /** One-time reminder for a single schedule. */
    private suspend fun handleScheduledReminder(scheduleId: Long): Result {
        val row = scheduleDao.getScheduleWithVehicle(scheduleId) ?: return Result.success()
        val reminderType = inputData.getString(KEY_REMINDER_TYPE) ?: "DUE"
        val phase = phaseFor(row, System.currentTimeMillis())

        // The worker may fire slightly early/late relative to the tier it was queued for,
        // so escalate to whatever the schedule has actually reached by now.
        val status = when (reminderType) {
            "UPCOMING" -> when (phase) {
                MaintenancePolicy.NotificationPhase.UPCOMING -> MaintenanceDueStatus.UPCOMING
                MaintenancePolicy.NotificationPhase.DUE -> MaintenanceDueStatus.DUE
                MaintenancePolicy.NotificationPhase.OVERDUE -> MaintenanceDueStatus.OVERDUE
                MaintenancePolicy.NotificationPhase.NONE -> null
            }
            else -> when (phase) {
                MaintenancePolicy.NotificationPhase.DUE -> MaintenanceDueStatus.DUE
                MaintenancePolicy.NotificationPhase.OVERDUE -> MaintenanceDueStatus.OVERDUE
                else -> null
            }
        } ?: return Result.success()

        notify(row, status)
        return Result.success()
    }

    private fun phaseFor(row: ScheduleWithVehicle, now: Long): MaintenancePolicy.NotificationPhase {
        val schedule = row.schedule
        return MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = schedule.toDomain().status,
            dueAt = schedule.dueAt,
            dueOdometerMeters = schedule.dueOdometerMeters,
            currentOdometerMeters = row.vehicleCurrentOdometerMeters,
            currentTimeMillis = now
        )
    }

    private fun statusFor(row: ScheduleWithVehicle, now: Long): MaintenanceDueStatus? =
        when (phaseFor(row, now)) {
            MaintenancePolicy.NotificationPhase.OVERDUE -> MaintenanceDueStatus.OVERDUE
            MaintenancePolicy.NotificationPhase.DUE -> MaintenanceDueStatus.DUE
            MaintenancePolicy.NotificationPhase.UPCOMING -> MaintenanceDueStatus.UPCOMING
            MaintenancePolicy.NotificationPhase.NONE -> null
        }

    private fun notify(row: ScheduleWithVehicle, status: MaintenanceDueStatus) {
        MaintenanceNotificationHelper.sendScheduleNotification(
            applicationContext,
            vehicle = com.example.core.model.Vehicle(
                id = row.vehicleId,
                name = row.vehicleName,
                type = com.example.core.model.VehicleType.MOTORCYCLE,
                currentOdometerMeters = row.vehicleCurrentOdometerMeters
            ),
            schedule = row.schedule.toDomain(),
            status = status
        )
    }
}
