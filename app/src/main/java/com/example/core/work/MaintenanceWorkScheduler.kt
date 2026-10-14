package com.example.core.work

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.core.database.AppDatabase
import com.example.core.model.MaintenanceDueStatus
import com.example.core.model.MaintenancePolicy
import com.example.core.model.MaintenanceSchedule
import com.example.core.model.ScheduleLifecycleStatus
import com.example.core.notification.MaintenanceNotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceWorkScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase
) {

    private fun getWorkManager(): WorkManager? {
        return try {
            WorkManager.getInstance(context)
        } catch (error: IllegalStateException) {
            Log.e("MaintenanceWorkScheduler", "WorkManager is not available", error)
            null
        }
    }

    /**
     * Schedules unique background work for a schedule's upcoming and due dates.
     * Replaces previous work for the schedule's current cycle, preventing duplicates.
     */
    fun scheduleDateReminders(schedule: MaintenanceSchedule) {
        if (schedule.isArchived || schedule.status != ScheduleLifecycleStatus.ACTIVE) {
            cancelReminders(schedule.id)
            return
        }

        if (schedule.dueAt == null) {
            // Schedule has no date due (ODO-only). Cancel existing date work requests without resetting notification history!
            cancelDateWorkOnly(schedule.id)
            return
        }

        val now = System.currentTimeMillis()
        val dueAt = schedule.dueAt
        val cycleKey = schedule.lastCompletedAt ?: schedule.createdAt

        if (dueAt <= now) {
            // Already due or overdue: evaluate immediately
            val immediateWork = OneTimeWorkRequestBuilder<MaintenanceReminderWorker>()
                .setInitialDelay(0, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        MaintenanceReminderWorker.KEY_SCHEDULE_ID to schedule.id,
                        MaintenanceReminderWorker.KEY_REMINDER_TYPE to "DUE"
                    )
                )
                .addTag("schedule_${schedule.id}")
                .build()

            getWorkManager()?.enqueueUniqueWork(
                "maint_due_${schedule.id}_$cycleKey",
                ExistingWorkPolicy.REPLACE,
                immediateWork
            )
        } else {
            // 1. Upcoming reminder (7 days before due date)
            val upcomingThreshold = MaintenancePolicy.DUE_THRESHOLD_DAYS * 86_400_000L
            val upcomingTime = dueAt - upcomingThreshold
            val upcomingDelay = (upcomingTime - now).coerceAtLeast(0L)

            val upcomingWork = OneTimeWorkRequestBuilder<MaintenanceReminderWorker>()
                .setInitialDelay(upcomingDelay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        MaintenanceReminderWorker.KEY_SCHEDULE_ID to schedule.id,
                        MaintenanceReminderWorker.KEY_REMINDER_TYPE to "UPCOMING"
                    )
                )
                .addTag("schedule_${schedule.id}")
                .build()

            getWorkManager()?.enqueueUniqueWork(
                "maint_upcoming_${schedule.id}_$cycleKey",
                ExistingWorkPolicy.REPLACE,
                upcomingWork
            )

            // 2. Due date reminder (on the due date)
            val dueDelay = dueAt - now
            val dueWork = OneTimeWorkRequestBuilder<MaintenanceReminderWorker>()
                .setInitialDelay(dueDelay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        MaintenanceReminderWorker.KEY_SCHEDULE_ID to schedule.id,
                        MaintenanceReminderWorker.KEY_REMINDER_TYPE to "DUE"
                    )
                )
                .addTag("schedule_${schedule.id}")
                .build()

            getWorkManager()?.enqueueUniqueWork(
                "maint_due_${schedule.id}_$cycleKey",
                ExistingWorkPolicy.REPLACE,
                dueWork
            )
        }
    }

    /**
     * Cancels any pending background work requests for date-based triggers without clearing notification tracker history.
     */
    fun cancelDateWorkOnly(scheduleId: Long) {
        getWorkManager()?.cancelAllWorkByTag("schedule_$scheduleId")
    }

    /**
     * Cancels any pending background work and active notifications, resetting anti-spam history for a schedule.
     * Used when schedule is cancelled, archived, completed, or deleted.
     */
    fun cancelReminders(scheduleId: Long) {
        cancelDateWorkOnly(scheduleId)
        MaintenanceNotificationHelper.cancelNotification(context, scheduleId)
    }

    /**
     * Evaluates ODO-based maintenance reminders when the user updates a vehicle's odometer
     * (e.g., logging a fuel entry or updating vehicle details).
     * Does NOT poll continuously; only evaluates on actual user data changes.
     */
    fun evaluateOdometerReminders(vehicleId: Long, newOdometerMeters: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val vehicleEntity = database.vehicleDao().getVehicleById(vehicleId) ?: return@launch
            if (vehicleEntity.isArchived) return@launch

            val schedules = database.maintenanceScheduleDao().getActiveSchedulesForVehicleSync(vehicleId)
            val vehicle = vehicleEntity.toDomain().copy(currentOdometerMeters = newOdometerMeters)
            val now = System.currentTimeMillis()

            for (scheduleEntity in schedules) {
                if (scheduleEntity.dueOdometerMeters == null) continue

                val schedule = scheduleEntity.toDomain()
                val phase = MaintenancePolicy.determineNotificationPhase(
                    lifecycleStatus = schedule.status,
                    dueAt = schedule.dueAt,
                    dueOdometerMeters = schedule.dueOdometerMeters,
                    currentOdometerMeters = newOdometerMeters,
                    currentTimeMillis = now
                )

                val targetStatus = when (phase) {
                    MaintenancePolicy.NotificationPhase.OVERDUE -> MaintenanceDueStatus.OVERDUE
                    MaintenancePolicy.NotificationPhase.DUE -> MaintenanceDueStatus.DUE
                    MaintenancePolicy.NotificationPhase.UPCOMING -> MaintenanceDueStatus.UPCOMING
                    MaintenancePolicy.NotificationPhase.NONE -> null
                }

                if (targetStatus != null) {
                    MaintenanceNotificationHelper.sendScheduleNotification(
                        context,
                        vehicle,
                        schedule,
                        targetStatus
                    )
                }
            }
        }
    }

    /**
     * Enqueues a periodic background audit (every 24 hours) with WorkManager
     * to verify due and upcoming maintenance dates across all active schedules.
     */
    fun scheduleDailyMaintenanceAudit() {
        val dailyAuditWork = PeriodicWorkRequestBuilder<MaintenanceReminderWorker>(24, TimeUnit.HOURS)
            .addTag("maintenance_daily_audit")
            .build()

        getWorkManager()?.enqueueUniquePeriodicWork(
            "maintenance_daily_audit_work",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyAuditWork
        )
    }
}
