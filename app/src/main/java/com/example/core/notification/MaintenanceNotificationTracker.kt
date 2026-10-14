package com.example.core.notification

import android.content.Context
import com.example.core.model.MaintenanceDueStatus

/**
 * Ensures notification idempotency and spam prevention for vehicle maintenance.
 *
 * Rules:
 * - UPCOMING: Dispatched at most ONCE per schedule cycle.
 * - DUE: Dispatched at most ONCE per schedule cycle.
 * - OVERDUE: Dispatched once when entering overdue, then throttled to at least 7 days between repeats.
 * - COMPLETED / CANCELLED: Never dispatched.
 */
object MaintenanceNotificationTracker {
    private const val PREFS_NAME = "fueltrack_maintenance_notifications"
    const val OVERDUE_THROTTLE_MS = 7 * 86_400_000L // 7 days

    private val lock = Any()

    /**
     * Atomically checks whether a notification should be sent and, if so, records it immediately.
     * Prevents race conditions when periodic worker, one-time worker, and ODO updates trigger concurrently.
     * Returns true if the notification is permitted to send, false otherwise.
     */
    fun checkAndRecordNotificationSent(
        context: Context,
        scheduleId: Long,
        cycleKey: Long,
        status: MaintenanceDueStatus,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        synchronized(lock) {
            val shouldSend = shouldSendNotification(context, scheduleId, cycleKey, status, currentTimeMillis)
            if (shouldSend) {
                recordNotificationSent(context, scheduleId, cycleKey, status, currentTimeMillis)
                return true
            }
            return false
        }
    }

    fun shouldSendNotification(
        context: Context,
        scheduleId: Long,
        cycleKey: Long,
        status: MaintenanceDueStatus,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        synchronized(lock) {
            if (status != MaintenanceDueStatus.UPCOMING &&
                status != MaintenanceDueStatus.DUE &&
                status != MaintenanceDueStatus.OVERDUE
            ) {
                return false
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val statusKey = "notif_${scheduleId}_${cycleKey}_status"
            val timestampKey = "notif_${scheduleId}_${cycleKey}_time"

            val lastStatusName = prefs.getString(statusKey, null)
            val lastTimestamp = prefs.getLong(timestampKey, 0L)

            return when (status) {
                MaintenanceDueStatus.UPCOMING -> {
                    // If not yet notified for this cycle, send UPCOMING
                    lastStatusName == null
                }
                MaintenanceDueStatus.DUE -> {
                    // Send DUE if not yet notified DUE or OVERDUE
                    lastStatusName != MaintenanceDueStatus.DUE.name &&
                    lastStatusName != MaintenanceDueStatus.OVERDUE.name
                }
                MaintenanceDueStatus.OVERDUE -> {
                    if (lastStatusName != MaintenanceDueStatus.OVERDUE.name) {
                        true // First time entering OVERDUE status
                    } else {
                        // Re-notify overdue only after throttling window
                        currentTimeMillis - lastTimestamp >= OVERDUE_THROTTLE_MS
                    }
                }
                else -> false
            }
        }
    }

    fun recordNotificationSent(
        context: Context,
        scheduleId: Long,
        cycleKey: Long,
        status: MaintenanceDueStatus,
        currentTimeMillis: Long = System.currentTimeMillis()
    ) {
        synchronized(lock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val statusKey = "notif_${scheduleId}_${cycleKey}_status"
            val timestampKey = "notif_${scheduleId}_${cycleKey}_time"

            prefs.edit()
                .putString(statusKey, status.name)
                .putLong(timestampKey, currentTimeMillis)
                .apply()
        }
    }

    /**
     * Rolls back a recorded notification if system notification dispatch failed (e.g. SecurityException).
     */
    fun rollbackNotificationSent(
        context: Context,
        scheduleId: Long,
        cycleKey: Long
    ) {
        synchronized(lock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val statusKey = "notif_${scheduleId}_${cycleKey}_status"
            val timestampKey = "notif_${scheduleId}_${cycleKey}_time"
            prefs.edit()
                .remove(statusKey)
                .remove(timestampKey)
                .apply()
        }
    }

    fun clearHistory(context: Context, scheduleId: Long) {
        synchronized(lock) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val prefix = "notif_${scheduleId}_"
            for (key in prefs.all.keys) {
                if (key.startsWith(prefix)) {
                    editor.remove(key)
                }
            }
            editor.apply()
        }
    }

    fun clearAll(context: Context) {
        synchronized(lock) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        }
    }
}
