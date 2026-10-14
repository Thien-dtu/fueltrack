package com.example.core.model

import java.util.Calendar

/**
 * Lifecycle status of a maintenance schedule
 */
enum class ScheduleLifecycleStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}

/**
 * Calculated due status (derived dynamically, not statically in database)
 */
enum class MaintenanceDueStatus {
    UPCOMING, // Far ahead
    DUE,      // Due today or within reminder threshold (<= 7 days or <= 200 km)
    OVERDUE,  // Overdue past end of due date or past due odometer
    COMPLETED,// Lifecycle completed
    CANCELLED // Lifecycle cancelled
}

object MaintenancePolicy {
    const val DUE_THRESHOLD_DAYS = 7L
    const val DUE_THRESHOLD_METERS = 200_000L // 200 km in meters

    /**
     * Notification phase for scheduling and dispatching notifications.
     */
    enum class NotificationPhase {
        NONE,     // Far ahead, do not notify
        UPCOMING, // Within upcoming threshold (<= 7 days or <= 200 km) but not yet reached due date/ODO
        DUE,      // Reached due date (within due day) or reached due odometer
        OVERDUE   // Past due date or strictly exceeded due odometer
    }

    /**
     * Returns the start of day (00:00:00.000) in local timezone for a given epoch timestamp.
     */
    fun getStartOfDayMillis(timeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Returns the end of day (23:59:59.999) in local timezone for a given epoch timestamp.
     * Prevents marking a schedule overdue at 00:01 AM on the due date.
     */
    fun getEndOfDayMillis(timeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    /**
     * Determines the computed due status for a schedule in the UI.
     * Order of precedence:
     * 1. Lifecycle (COMPLETED / CANCELLED).
     * 2. OVERDUE check: if currentTimeMillis > end of due day OR (currentOdo > dueOdo).
     *    Note: exactly equal currentOdo == dueOdo is considered DUE, not OVERDUE.
     * 3. DUE check: if within 7 days before due date (including the entire due date) OR within 200 km.
     * 4. UPCOMING: otherwise.
     */
    fun calculateDueStatus(
        lifecycleStatus: ScheduleLifecycleStatus,
        dueAt: Long?,
        dueOdometerMeters: Long?,
        currentOdometerMeters: Long?,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): MaintenanceDueStatus {
        if (lifecycleStatus == ScheduleLifecycleStatus.COMPLETED) return MaintenanceDueStatus.COMPLETED
        if (lifecycleStatus == ScheduleLifecycleStatus.CANCELLED) return MaintenanceDueStatus.CANCELLED

        // 1. OVERDUE check: strictly past end of due day OR strictly exceeded due odometer
        val isOverdueByDate = dueAt != null && currentTimeMillis > getEndOfDayMillis(dueAt)
        val isOverdueByOdo = dueOdometerMeters != null && currentOdometerMeters != null && currentOdometerMeters > dueOdometerMeters

        if (isOverdueByDate || isOverdueByOdo) {
            return MaintenanceDueStatus.OVERDUE
        }

        // 2. DUE check: within threshold (<= 7 days or <= 200 km, or exactly at due point)
        val isDueByDate = dueAt != null && (dueAt - currentTimeMillis) <= (DUE_THRESHOLD_DAYS * 86_400_000L)
        val isDueByOdo = dueOdometerMeters != null && currentOdometerMeters != null &&
                (dueOdometerMeters - currentOdometerMeters) <= DUE_THRESHOLD_METERS

        if (isDueByDate || isDueByOdo) {
            return MaintenanceDueStatus.DUE
        }

        return MaintenanceDueStatus.UPCOMING
    }

    /**
     * Determines the notification phase for a schedule.
     * Distinguishes:
     * - OVERDUE: currentTime > end of due day OR currentOdo > dueOdo.
     * - DUE: reached due date (>= start of due date) OR reached due ODO (currentOdo >= dueOdo).
     * - UPCOMING: within 7 days (>= dueAt - 7 days) OR within 200 km (>= dueOdo - 200 km).
     * - NONE: far ahead or not active.
     */
    fun determineNotificationPhase(
        lifecycleStatus: ScheduleLifecycleStatus,
        dueAt: Long?,
        dueOdometerMeters: Long?,
        currentOdometerMeters: Long?,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): NotificationPhase {
        if (lifecycleStatus != ScheduleLifecycleStatus.ACTIVE) return NotificationPhase.NONE

        // Overdue check
        val isOverdueByDate = dueAt != null && currentTimeMillis > getEndOfDayMillis(dueAt)
        val isOverdueByOdo = dueOdometerMeters != null && currentOdometerMeters != null && currentOdometerMeters > dueOdometerMeters

        if (isOverdueByDate || isOverdueByOdo) {
            return NotificationPhase.OVERDUE
        }

        // Due check (on the due date or reached due ODO)
        val isDueByDate = dueAt != null && currentTimeMillis >= getStartOfDayMillis(dueAt)
        val isDueByOdo = dueOdometerMeters != null && currentOdometerMeters != null && currentOdometerMeters >= dueOdometerMeters

        if (isDueByDate || isDueByOdo) {
            return NotificationPhase.DUE
        }

        // Upcoming check (within upcoming threshold)
        val isUpcomingByDate = dueAt != null && (dueAt - currentTimeMillis) <= (DUE_THRESHOLD_DAYS * 86_400_000L)
        val isUpcomingByOdo = dueOdometerMeters != null && currentOdometerMeters != null &&
                (dueOdometerMeters - currentOdometerMeters) <= DUE_THRESHOLD_METERS

        if (isUpcomingByDate || isUpcomingByOdo) {
            return NotificationPhase.UPCOMING
        }

        return NotificationPhase.NONE
    }
}
