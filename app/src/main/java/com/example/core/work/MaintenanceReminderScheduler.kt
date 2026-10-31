package com.example.core.work

import com.example.core.model.MaintenanceSchedule

/**
 * Reminder scheduling as seen by the data layer.
 *
 * Repositories need to react to a write (for example, a new odometer reading may
 * make a maintenance schedule due) but they should not depend on WorkManager, an
 * Android framework component. Depending on this interface keeps the repository
 * contract testable and lets the implementation stay in the work package.
 */
interface MaintenanceReminderScheduler {
    /** Re-evaluates odometer-triggered reminders after the vehicle odometer moved. */
    fun evaluateOdometerReminders(vehicleId: Long, newOdometerMeters: Long)

    /** Queues upcoming/due work for a schedule's current cycle. */
    fun scheduleDateReminders(schedule: MaintenanceSchedule)

    /** Cancels pending work for a schedule and clears its posted notification. */
    fun cancelReminders(scheduleId: Long)
}
