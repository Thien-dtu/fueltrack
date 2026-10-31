package com.example.core.util

import com.example.core.model.FuelEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/**
 * Regression tests for the monthly-distance baseline defect.
 *
 * Bug: [StatisticsCalculator.computeMonthlyStats] picked the baseline entry with
 * "the last entry whose odometer is smaller than this month's first odometer".
 * That is an odometer-based comparison standing in for a chronological one, and it
 * breaks in two ways:
 *
 *  - After an odometer reset the pre-reset entries have the larger readings, so the
 *    baseline is taken from the wrong record and the month reports a phantom trip.
 *  - Entries are also grouped after sorting by odometer, so month buckets mix
 *    pre-reset and post-reset readings.
 *
 * Correct behaviour: distance is measured chronologically, and a stretch that spans
 * an odometer reset contributes only the part that is still measurable.
 */
class MonthlyStatsBaselineTest {

    @Test
    fun monthlyDistanceUsesChronologicalPriorEntryAsBaseline() {
        val entries = listOf(
            fuelEntry(1, at(2026, 1, 1), 10_000_000L),
            fuelEntry(2, at(2026, 1, 15), 10_250_000L),
            fuelEntry(3, at(2026, 2, 10), 10_580_400L)
        )

        val stats = StatisticsCalculator.computeMonthlyStats(entries)

        val january = stats.first { it.yearMonthKey == "2026-01" }
        val february = stats.first { it.yearMonthKey == "2026-02" }

        assertEquals(250_000L, january.totalDistanceMeters)
        // 10,580.4 km minus the last January reading of 10,250 km.
        assertEquals(330_400L, february.totalDistanceMeters)
    }

    @Test
    fun monthlyDistanceDoesNotInventDistanceAcrossOdometerReset() {
        // The odometer is replaced between the January and February fill-ups.
        val entries = listOf(
            fuelEntry(1, at(2026, 1, 10), 10_000_000L),
            fuelEntry(2, at(2026, 2, 5), 10_500_000L),
            fuelEntry(3, at(2026, 2, 20), 500_000L)
        )

        val stats = StatisticsCalculator.computeMonthlyStats(entries)

        val february = stats.first { it.yearMonthKey == "2026-02" }
        // 500 km were driven on the old meter before it was replaced. The jump down to
        // 500 km is not a distance, so it must not be added as ~10,000 km.
        assertEquals(500_000L, february.totalDistanceMeters)
    }

    @Test
    fun monthlyDistanceAccumulatesNormallyAfterReset() {
        val entries = listOf(
            fuelEntry(1, at(2026, 1, 10), 10_000_000L),
            fuelEntry(2, at(2026, 2, 5), 500_000L),  // first reading on the new meter
            fuelEntry(3, at(2026, 2, 20), 900_000L)  // 400 km later on the same meter
        )

        val stats = StatisticsCalculator.computeMonthlyStats(entries)

        val february = stats.first { it.yearMonthKey == "2026-02" }
        assertEquals(400_000L, february.totalDistanceMeters)
    }

    @Test
    fun monthBucketsFollowTimestampsNotOdometerOrder() {
        // Entry 2 happened later but reads lower, because the meter was replaced.
        val entries = listOf(
            fuelEntry(1, at(2026, 1, 5), 10_000_000L),
            fuelEntry(2, at(2026, 1, 25), 300_000L)
        )

        val stats = StatisticsCalculator.computeMonthlyStats(entries)

        val january = stats.single { it.yearMonthKey == "2026-01" }
        assertEquals(2, january.entriesCount)
        // Only the pre-reset part is measurable; the post-reset reading is a fresh start.
        assertEquals(0L, january.totalDistanceMeters)
    }

    private fun at(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, 12, 0, 0)
    }.timeInMillis

    private fun fuelEntry(
        id: Long,
        timestamp: Long,
        odometerMeters: Long,
        liters: Double = 4.0,
        amountPaid: Long = 100_000L
    ) = FuelEntry(
        id = id,
        vehicleId = 1L,
        timestamp = timestamp,
        odometerMeters = odometerMeters,
        fuelTypeId = 1L,
        pricePerLiter = 25_000L,
        amountPaid = amountPaid,
        liters = liters,
        isFullTank = true
    )
}
