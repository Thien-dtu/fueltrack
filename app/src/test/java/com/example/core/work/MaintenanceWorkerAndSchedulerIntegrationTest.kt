package com.example.core.work

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.example.core.database.AppDatabase
import com.example.core.database.entity.MaintenanceScheduleEntity
import com.example.core.database.entity.VehicleEntity
import com.example.core.model.MaintenanceDueStatus
import com.example.core.model.MaintenancePolicy
import com.example.core.model.ScheduleLifecycleStatus
import com.example.core.notification.MaintenanceNotificationTracker
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MaintenanceWorkerAndSchedulerIntegrationTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        MaintenanceNotificationTracker.clearAll(context)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        MaintenanceNotificationTracker.clearAll(context)
    }

    @Test
    fun testFarAheadSchedule_doesNotSendPrematureNotification() {
        val dueFarAhead = now + (30 * 86_400_000L) // 30 days ahead (> 7 days)
        val phase = MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = dueFarAhead,
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = 5_000_000L, // 5,000 km remaining (> 200 km)
            currentTimeMillis = now
        )
        // Must be NONE, not UPCOMING or DUE
        assertEquals(MaintenancePolicy.NotificationPhase.NONE, phase)
    }

    @Test
    fun testUpcomingThreshold_within7Days_triggersUpcomingNotification() {
        val dueIn5Days = now + (5 * 86_400_000L) // within 7-day threshold
        val phase = MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = dueIn5Days,
            dueOdometerMeters = null,
            currentOdometerMeters = null,
            currentTimeMillis = now
        )
        assertEquals(MaintenancePolicy.NotificationPhase.UPCOMING, phase)
    }

    @Test
    fun testUpcomingThreshold_within200Km_triggersUpcomingNotification() {
        // Due ODO 10,000 km, current ODO 9,850 km -> diff 150 km (<= 200 km threshold)
        val phase = MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = now + (30 * 86_400_000L),
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = 9_850_000L,
            currentTimeMillis = now
        )
        assertEquals(MaintenancePolicy.NotificationPhase.UPCOMING, phase)
    }

    @Test
    fun testDueDate_triggersDueNotification() {
        // On due date
        val phase = MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = now,
            dueOdometerMeters = null,
            currentOdometerMeters = null,
            currentTimeMillis = now
        )
        assertEquals(MaintenancePolicy.NotificationPhase.DUE, phase)
    }

    @Test
    fun testExactOdometerBoundary_isDueNotOverdue() {
        val phase = MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = null,
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = 10_000_000L, // exact match
            currentTimeMillis = now
        )
        assertEquals(MaintenancePolicy.NotificationPhase.DUE, phase)

        // Strictly exceeded is OVERDUE
        val overduePhase = MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = null,
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = 10_000_100L,
            currentTimeMillis = now
        )
        assertEquals(MaintenancePolicy.NotificationPhase.OVERDUE, overduePhase)
    }

    @Test
    fun testRepeatedPeriodicAudit_doesNotSendDuplicateNotification() {
        val scheduleId = 201L
        val cycleKey = now

        // Check if UPCOMING should send on first audit run
        val shouldSend1 = MaintenanceNotificationTracker.checkAndRecordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.UPCOMING, now
        )
        assertTrue(shouldSend1)

        // Next audit run (e.g. 1 hour or 1 day later without state progression)
        val shouldSend2 = MaintenanceNotificationTracker.checkAndRecordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.UPCOMING, now + 3600_000L
        )
        assertFalse(shouldSend2)
    }

    @Test
    fun testOdoOnlySchedule_doesNotLoseHistoryWhenDateWorkIsCancelled() {
        val scheduleId = 202L
        val cycleKey = now

        // ODO triggered DUE notification
        MaintenanceNotificationTracker.recordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.DUE, now
        )

        // Cancel date work (called when schedule is edited with dueAt = null)
        val scheduler = MaintenanceWorkScheduler(context, db)
        scheduler.cancelDateWorkOnly(scheduleId)

        // Anti-spam history MUST remain intact!
        val canResend = MaintenanceNotificationTracker.shouldSendNotification(
            context, scheduleId, cycleKey, MaintenanceDueStatus.DUE, now + 1000L
        )
        assertFalse("ODO-only schedule should not lose notification history after date-work cancel", canResend)
    }

    @Test
    fun testCompletedCancelledOrArchived_preventsNotification() {
        val completedPhase = MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = ScheduleLifecycleStatus.COMPLETED,
            dueAt = now - 1000L,
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = 20_000_000L,
            currentTimeMillis = now
        )
        assertEquals(MaintenancePolicy.NotificationPhase.NONE, completedPhase)

        val cancelledPhase = MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = ScheduleLifecycleStatus.CANCELLED,
            dueAt = now - 1000L,
            dueOdometerMeters = 10_000_000L,
            currentOdometerMeters = 20_000_000L,
            currentTimeMillis = now
        )
        assertEquals(MaintenancePolicy.NotificationPhase.NONE, cancelledPhase)
    }

    @Test
    fun testMissingDateOrOdo_doesNotCrashOrSendErroneousNotification() {
        // Schedule with no date and no ODO
        val phase = MaintenancePolicy.determineNotificationPhase(
            lifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
            dueAt = null,
            dueOdometerMeters = null,
            currentOdometerMeters = 5_000_000L,
            currentTimeMillis = now
        )
        assertEquals(MaintenancePolicy.NotificationPhase.NONE, phase)
    }

    @Test
    fun testConcurrentEvaluations_handledIdempotently() {
        val scheduleId = 203L
        val cycleKey = now

        // Simulate two near-simultaneous triggers: Worker and ODO update
        val workerSent = MaintenanceNotificationTracker.checkAndRecordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.DUE, now
        )
        val odoSent = MaintenanceNotificationTracker.checkAndRecordNotificationSent(
            context, scheduleId, cycleKey, MaintenanceDueStatus.DUE, now
        )

        assertTrue(workerSent)
        assertFalse(odoSent) // Second one throttled atomically
    }
}
