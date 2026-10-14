package com.example.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.core.model.MaintenanceDueStatus
import com.example.core.model.MaintenanceSchedule
import com.example.core.model.Vehicle
import com.example.core.util.Formatters

object MaintenanceNotificationHelper {
    const val CHANNEL_ID = "fueltrack_maintenance_channel"
    const val CHANNEL_NAME = "Nhắc nhở bảo dưỡng xe"
    const val CHANNEL_DESC = "Thông báo định kỳ khi xe sắp đến hạn hoặc quá hạn bảo dưỡng"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Sends a notification for a due or upcoming schedule.
     * Checks POST_NOTIFICATIONS permission safely and handles SecurityException gracefully.
     * Prevents duplicate alerts and spam via MaintenanceNotificationTracker.
     */
    fun sendScheduleNotification(
        context: Context,
        vehicle: Vehicle,
        schedule: MaintenanceSchedule,
        status: MaintenanceDueStatus
    ) {
        val cycleKey = schedule.lastCompletedAt ?: schedule.createdAt
        // Atomically check and record to eliminate race conditions across concurrent evaluations
        if (!MaintenanceNotificationTracker.checkAndRecordNotificationSent(context, schedule.id, cycleKey, status)) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                // Not allowed to post notifications, roll back so user can be notified if permission is granted later
                MaintenanceNotificationTracker.rollbackNotificationSent(context, schedule.id, cycleKey)
                return
            }
        }

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "maintenance")
            putExtra("vehicle_id", vehicle.id)
            putExtra("schedule_id", schedule.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val statusText = when (status) {
            MaintenanceDueStatus.OVERDUE -> "ĐÃ QUÁ HẠN!"
            MaintenanceDueStatus.DUE -> "ĐẾN HẠN BẢO DƯỠNG!"
            MaintenanceDueStatus.UPCOMING -> "SẮP ĐẾN HẠN!"
            else -> "BẢO DƯỠNG"
        }

        val details = buildString {
            append("Phương tiện: ${vehicle.name}. ")
            if (schedule.dueAt != null) {
                append("Hạn ngày: ${Formatters.formatDateOnly(schedule.dueAt)}. ")
            }
            if (schedule.dueOdometerMeters != null) {
                append("Hạn ODO: ${Formatters.formatDistanceKm(schedule.dueOdometerMeters)}. ")
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("[$statusText] ${schedule.title}")
            .setContentText(details)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (schedule.id % Int.MAX_VALUE).toInt()
            notificationManager.notify(notificationId, builder.build())
        } catch (error: SecurityException) {
            Log.e("MaintenanceNotification", "Notification permission was rejected", error)
            // Roll back if OS threw SecurityException so notification isn't permanently silenced
            MaintenanceNotificationTracker.rollbackNotificationSent(context, schedule.id, cycleKey)
        }
    }

    /**
     * Cancels any active notification in the system notification shade.
     * Does NOT wipe the anti-spam notification tracker history so ODO or other periodic checks
     * do not re-spam.
     */
    fun cancelNotificationOnly(context: Context, scheduleId: Long) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (scheduleId % Int.MAX_VALUE).toInt()
            notificationManager.cancel(notificationId)
        } catch (error: SecurityException) {
            Log.e("MaintenanceNotification", "Unable to cancel notification", error)
        }
    }

    /**
     * Cancels active notification and clears notification history.
     * Should only be called when a schedule cycle is reset (e.g. completed, cancelled or deleted).
     */
    fun cancelNotification(context: Context, scheduleId: Long) {
        cancelNotificationOnly(context, scheduleId)
        MaintenanceNotificationTracker.clearHistory(context, scheduleId)
    }
}
