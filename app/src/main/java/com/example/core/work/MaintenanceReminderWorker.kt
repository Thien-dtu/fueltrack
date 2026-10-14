package com.example.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.core.database.AppDatabase
import com.example.core.model.MaintenanceDueStatus
import com.example.core.model.MaintenancePolicy
import com.example.core.model.ScheduleLifecycleStatus
import com.example.core.notification.MaintenanceNotificationHelper

class MaintenanceReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_SCHEDULE_ID = "key_schedule_id"
        const val KEY_REMINDER_TYPE = "key_reminder_type" // "UPCOMING" or "DUE"
    }

    override suspend fun doWork(): Result {
        val scheduleId = inputData.getLong(KEY_SCHEDULE_ID, -1L)
        val db = AppDatabase.getInstance(applicationContext)

        if (scheduleId == -1L) {
            // Periodic audit of all active schedules (e.g. daily audit)
            val activeSchedules = db.maintenanceScheduleDao().getAllActiveSchedulesSync()
            val now = System.currentTimeMillis()

            for (scheduleEntity in activeSchedules) {
                if (scheduleEntity.isArchived || scheduleEntity.status != ScheduleLifecycleStatus.ACTIVE.name) {
                    continue
                }

                val vehicleEntity = db.vehicleDao().getVehicleById(scheduleEntity.vehicleId) ?: continue
                if (vehicleEntity.isArchived) continue

                val schedule = scheduleEntity.toDomain()
                val vehicle = vehicleEntity.toDomain()

                val phase = MaintenancePolicy.determineNotificationPhase(
                    lifecycleStatus = schedule.status,
                    dueAt = schedule.dueAt,
                    dueOdometerMeters = schedule.dueOdometerMeters,
                    currentOdometerMeters = vehicle.currentOdometerMeters,
                    currentTimeMillis = now
                )

                // Only notify if actually in an alert phase (UPCOMING, DUE, or OVERDUE)
                val targetStatus = when (phase) {
                    MaintenancePolicy.NotificationPhase.OVERDUE -> MaintenanceDueStatus.OVERDUE
                    MaintenancePolicy.NotificationPhase.DUE -> MaintenanceDueStatus.DUE
                    MaintenancePolicy.NotificationPhase.UPCOMING -> MaintenanceDueStatus.UPCOMING
                    MaintenancePolicy.NotificationPhase.NONE -> null
                }

                if (targetStatus != null) {
                    MaintenanceNotificationHelper.sendScheduleNotification(
                        applicationContext,
                        vehicle,
                        schedule,
                        targetStatus
                    )
                }
            }
            return Result.success()
        }

        val reminderType = inputData.getString(KEY_REMINDER_TYPE) ?: "DUE"
        val scheduleEntity = db.maintenanceScheduleDao().getScheduleById(scheduleId) ?: return Result.success()

        if (scheduleEntity.isArchived || scheduleEntity.status != ScheduleLifecycleStatus.ACTIVE.name) {
            return Result.success()
        }

        val vehicleEntity = db.vehicleDao().getVehicleById(scheduleEntity.vehicleId) ?: return Result.success()
        if (vehicleEntity.isArchived) {
            return Result.success()
        }

        val schedule = scheduleEntity.toDomain()
        val vehicle = vehicleEntity.toDomain()
        val now = System.currentTimeMillis()

        val phase = MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = schedule.status,
            dueAt = schedule.dueAt,
            dueOdometerMeters = schedule.dueOdometerMeters,
            currentOdometerMeters = vehicle.currentOdometerMeters,
            currentTimeMillis = now
        )

        // For one-time scheduled workers, check if the schedule has reached the scheduled reminder tier
        if (reminderType == "DUE") {
            if (phase == MaintenancePolicy.NotificationPhase.DUE) {
                MaintenanceNotificationHelper.sendScheduleNotification(
                    applicationContext,
                    vehicle,
                    schedule,
                    MaintenanceDueStatus.DUE
                )
            } else if (phase == MaintenancePolicy.NotificationPhase.OVERDUE) {
                MaintenanceNotificationHelper.sendScheduleNotification(
                    applicationContext,
                    vehicle,
                    schedule,
                    MaintenanceDueStatus.OVERDUE
                )
            }
        } else if (reminderType == "UPCOMING") {
            if (phase == MaintenancePolicy.NotificationPhase.UPCOMING) {
                MaintenanceNotificationHelper.sendScheduleNotification(
                    applicationContext,
                    vehicle,
                    schedule,
                    MaintenanceDueStatus.UPCOMING
                )
            } else if (phase == MaintenancePolicy.NotificationPhase.DUE) {
                // If it already reached DUE by the time UPCOMING worker ran
                MaintenanceNotificationHelper.sendScheduleNotification(
                    applicationContext,
                    vehicle,
                    schedule,
                    MaintenanceDueStatus.DUE
                )
            } else if (phase == MaintenancePolicy.NotificationPhase.OVERDUE) {
                MaintenanceNotificationHelper.sendScheduleNotification(
                    applicationContext,
                    vehicle,
                    schedule,
                    MaintenanceDueStatus.OVERDUE
                )
            }
        }

        return Result.success()
    }
}
