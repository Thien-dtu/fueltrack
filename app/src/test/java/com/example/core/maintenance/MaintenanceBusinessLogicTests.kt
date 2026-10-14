package com.example.core.maintenance

import com.example.core.model.MaintenanceDueStatus
import com.example.core.model.MaintenanceEntry
import com.example.core.model.MaintenancePolicy
import com.example.core.model.MaintenanceSchedule
import com.example.core.model.ScheduleLifecycleStatus
import com.example.core.repository.MaintenanceRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MaintenanceBusinessLogicTests {

    private val now = 1_700_000_000_000L // arbitrary fixed timestamp for testing

    @Test
    fun testDueStatus_lifecyclePrecedence() {
        // COMPLETED status takes precedence regardless of date or ODO
        val completedStatus = MaintenancePolicy.calculateDueStatus(
            lifecycleStatus = ScheduleLifecycleStatus.COMPLETED,
            dueAt = now - 1000L,
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = 12_000_000L,
            currentTimeMillis = now
        )
        assertEquals(MaintenanceDueStatus.COMPLETED, completedStatus)

        // CANCELLED status takes precedence
        val cancelledStatus = MaintenancePolicy.calculateDueStatus(
            lifecycleStatus = ScheduleLifecycleStatus.CANCELLED,
            dueAt = now - 1000L,
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = 12_000_000L,
            currentTimeMillis = now
        )
        assertEquals(MaintenanceDueStatus.CANCELLED, cancelledStatus)
    }

    @Test
    fun testDueStatus_overdueByDate() {
        val overdueByDate = MaintenancePolicy.calculateDueStatus(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = now - 86_400_000L, // 1 day ago
            dueOdometerMeters = null,
            currentOdometerMeters = null,
            currentTimeMillis = now
        )
        assertEquals(MaintenanceDueStatus.OVERDUE, overdueByDate)
    }

    @Test
    fun testDueStatus_overdueByOdometer() {
        // Current ODO: 10,200.0 km (10,200,000 m), Due ODO: 10,000.0 km (10,000,000 m)
        val overdueByOdo = MaintenancePolicy.calculateDueStatus(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = now + (30 * 86_400_000L), // Date is still 30 days ahead
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = 10_200_000L,
            currentTimeMillis = now
        )
        assertEquals(MaintenanceDueStatus.OVERDUE, overdueByOdo)
    }

    @Test
    fun testDueStatus_dueSoonThresholds() {
        // Within 7 days threshold (e.g. 3 days remaining) -> DUE
        val dueSoonByDate = MaintenancePolicy.calculateDueStatus(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = now + (3 * 86_400_000L),
            dueOdometerMeters = null,
            currentOdometerMeters = null,
            currentTimeMillis = now
        )
        assertEquals(MaintenanceDueStatus.DUE, dueSoonByDate)

        // Within 200 km threshold (e.g. 50 km remaining) -> DUE
        // Due: 10,000,000 m (10,000 km), Current: 9,950,000 m (9,950 km) -> diff 50,000 m (50 km <= 200 km)
        val dueSoonByOdo = MaintenancePolicy.calculateDueStatus(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = now + (30 * 86_400_000L),
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = 9_950_000L,
            currentTimeMillis = now
        )
        assertEquals(MaintenanceDueStatus.DUE, dueSoonByOdo)
    }

    @Test
    fun testDueStatus_upcoming() {
        // Date is 20 days ahead and ODO is 1,000 km ahead -> UPCOMING
        val upcoming = MaintenancePolicy.calculateDueStatus(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = now + (20 * 86_400_000L),
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = 9_000_000L,
            currentTimeMillis = now
        )
        assertEquals(MaintenanceDueStatus.UPCOMING, upcoming)
    }

    @Test
    fun testDueStatus_missingOdometerData() {
        // If odometer is null, relies on date condition
        val withDateOnly = MaintenancePolicy.calculateDueStatus(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = now + (20 * 86_400_000L),
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = null, // unknown current ODO
            currentTimeMillis = now
        )
        assertEquals(MaintenanceDueStatus.UPCOMING, withDateOnly)

        // If only ODO was specified and current ODO is null
        val odoOnlyMissing = MaintenancePolicy.calculateDueStatus(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = null,
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = null,
            currentTimeMillis = now
        )
        assertEquals(MaintenanceDueStatus.UPCOMING, odoOnlyMissing)
    }

    @Test
    fun testDueStatus_exactOdometerBoundary_isDueNotOverdue() {
        // When currentOdometerMeters == dueOdometerMeters, status is DUE, not OVERDUE
        val exactMatchStatus = MaintenancePolicy.calculateDueStatus(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = null,
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = 10_000_000L,
            currentTimeMillis = now
        )
        assertEquals(MaintenanceDueStatus.DUE, exactMatchStatus)

        // When currentOdometerMeters strictly exceeds dueOdometerMeters, status is OVERDUE
        val exceededStatus = MaintenancePolicy.calculateDueStatus(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = null,
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = 10_000_001L,
            currentTimeMillis = now
        )
        assertEquals(MaintenanceDueStatus.OVERDUE, exceededStatus)
    }

    @Test
    fun testNotificationPhase_distinguishesUpcomingFromFarAhead() {
        // 20 days ahead -> NONE (do not notify)
        val farAheadPhase = MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = now + (20 * 86_400_000L),
            dueOdometerMeters = null,
            currentOdometerMeters = null,
            currentTimeMillis = now
        )
        assertEquals(MaintenancePolicy.NotificationPhase.NONE, farAheadPhase)

        // 5 days ahead (within 7-day window) -> UPCOMING
        val upcomingPhase = MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = now + (5 * 86_400_000L),
            dueOdometerMeters = null,
            currentOdometerMeters = null,
            currentTimeMillis = now
        )
        assertEquals(MaintenancePolicy.NotificationPhase.UPCOMING, upcomingPhase)

        // On the due date -> DUE
        val duePhase = MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = now,
            dueOdometerMeters = null,
            currentOdometerMeters = null,
            currentTimeMillis = now
        )
        assertEquals(MaintenancePolicy.NotificationPhase.DUE, duePhase)

        // Past the due date -> OVERDUE
        val overduePhase = MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = now - 86_400_000L,
            dueOdometerMeters = null,
            currentOdometerMeters = null,
            currentTimeMillis = now
        )
        assertEquals(MaintenancePolicy.NotificationPhase.OVERDUE, overduePhase)
    }
}
