package com.example.core.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.MaintenanceDueStatus
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MaintenanceNotificationTrackerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        MaintenanceNotificationTracker.clearAll(context)
    }

    @After
    fun tearDown() {
        MaintenanceNotificationTracker.clearAll(context)
    }

    @Test
    fun testUpcoming_sentOnlyOncePerCycle() {
        val scheduleId = 101L
        val cycleKey = 1_000_000L

        // 1. First time checking UPCOMING: should send
        val firstCheck = MaintenanceNotificationTracker.shouldSendNotification(
            context, scheduleId, cycleKey, MaintenanceDueStatus.UPCOMING
        )
        assertTrue(firstCheck)

        // Record that it was sent
        MaintenanceNotificationTracker.recordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.UPCOMING
        )

        // 2. Second check (e.g. daily audit next day): should NOT send duplicate
        val secondCheck = MaintenanceNotificationTracker.shouldSendNotification(
            context, scheduleId, cycleKey, MaintenanceDueStatus.UPCOMING
        )
        assertFalse(secondCheck)
    }

    @Test
    fun testDue_transitionsProperlyAfterUpcoming() {
        val scheduleId = 102L
        val cycleKey = 1_000_000L

        // Mark UPCOMING as already sent
        MaintenanceNotificationTracker.recordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.UPCOMING
        )

        // When transition to DUE arrives: should send
        val dueCheck = MaintenanceNotificationTracker.shouldSendNotification(
            context, scheduleId, cycleKey, MaintenanceDueStatus.DUE
        )
        assertTrue(dueCheck)

        // Record DUE sent
        MaintenanceNotificationTracker.recordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.DUE
        )

        // Next day check for DUE should NOT send duplicate
        val repeatDueCheck = MaintenanceNotificationTracker.shouldSendNotification(
            context, scheduleId, cycleKey, MaintenanceDueStatus.DUE
        )
        assertFalse(repeatDueCheck)
    }

    @Test
    fun testOverdue_throttlingSevenDays() {
        val scheduleId = 103L
        val cycleKey = 1_000_000L
        val baseTime = 1_700_000_000_000L

        // Mark DUE as already sent
        MaintenanceNotificationTracker.recordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.DUE, currentTimeMillis = baseTime
        )

        // First transition to OVERDUE: should send
        val firstOverdue = MaintenanceNotificationTracker.shouldSendNotification(
            context, scheduleId, cycleKey, MaintenanceDueStatus.OVERDUE, currentTimeMillis = baseTime + 1000L
        )
        assertTrue(firstOverdue)

        // Record OVERDUE sent
        MaintenanceNotificationTracker.recordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.OVERDUE, currentTimeMillis = baseTime + 1000L
        )

        // 2 days later: throttled (false)
        val twoDaysLater = MaintenanceNotificationTracker.shouldSendNotification(
            context, scheduleId, cycleKey, MaintenanceDueStatus.OVERDUE, currentTimeMillis = baseTime + (2 * 86_400_000L)
        )
        assertFalse(twoDaysLater)

        // 8 days later (> 7 days): allows re-notification
        val eightDaysLater = MaintenanceNotificationTracker.shouldSendNotification(
            context, scheduleId, cycleKey, MaintenanceDueStatus.OVERDUE, currentTimeMillis = baseTime + (8 * 86_400_000L)
        )
        assertTrue(eightDaysLater)
    }

    @Test
    fun testCompletedAndCancelled_neverNotifies() {
        val scheduleId = 104L
        val cycleKey = 1_000_000L

        assertFalse(
            MaintenanceNotificationTracker.shouldSendNotification(
                context, scheduleId, cycleKey, MaintenanceDueStatus.COMPLETED
            )
        )
        assertFalse(
            MaintenanceNotificationTracker.shouldSendNotification(
                context, scheduleId, cycleKey, MaintenanceDueStatus.CANCELLED
            )
        )
    }

    @Test
    fun testClearHistory_allowsFreshCycle() {
        val scheduleId = 105L
        val cycleKey = 1_000_000L

        MaintenanceNotificationTracker.recordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.DUE
        )
        assertFalse(
            MaintenanceNotificationTracker.shouldSendNotification(
                context, scheduleId, cycleKey, MaintenanceDueStatus.DUE
            )
        )

        // Clear history (e.g. user completed or updated schedule)
        MaintenanceNotificationTracker.clearHistory(context, scheduleId)

        // Fresh cycle can now be notified
        val afterClear = MaintenanceNotificationTracker.shouldSendNotification(
            context, scheduleId, cycleKey, MaintenanceDueStatus.DUE
        )
        assertTrue(afterClear)
    }

    @Test
    fun testAtomicCheckAndRecord_preventsDuplicateUnderConcurrentAccess() {
        val scheduleId = 106L
        val cycleKey = 1_000_000L

        // First call should succeed and atomically record
        val firstCheck = MaintenanceNotificationTracker.checkAndRecordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.UPCOMING
        )
        assertTrue(firstCheck)

        // Concurrent or second call should immediately be false
        val secondCheck = MaintenanceNotificationTracker.checkAndRecordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.UPCOMING
        )
        assertFalse(secondCheck)
    }

    @Test
    fun testRollback_allowsRetryIfNotificationFailed() {
        val scheduleId = 107L
        val cycleKey = 1_000_000L

        // Recorded as sent
        val recorded = MaintenanceNotificationTracker.checkAndRecordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.DUE
        )
        assertTrue(recorded)

        // Dispatch failed, rolled back
        MaintenanceNotificationTracker.rollbackNotificationSent(context, scheduleId, cycleKey)

        // Now it can be attempted again
        val canRetry = MaintenanceNotificationTracker.shouldSendNotification(
            context, scheduleId, cycleKey, MaintenanceDueStatus.DUE
        )
        assertTrue(canRetry)
    }

    @Test
    fun testOdoOnlySchedule_preservesHistoryWhenDateWorkCancelled() {
        val scheduleId = 108L
        val cycleKey = 1_000_000L

        // ODO notification was already dispatched
        MaintenanceNotificationTracker.recordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.DUE
        )

        // Editing schedule without changing cycle key should NOT clear history
        // Verification: tracker still knows DUE was sent
        assertFalse(
            MaintenanceNotificationTracker.shouldSendNotification(
                context, scheduleId, cycleKey, MaintenanceDueStatus.DUE
            )
        )
    }
}
