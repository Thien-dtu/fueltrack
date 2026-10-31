package com.example.core.util

import com.example.core.model.FuelEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * Regression tests for "distance since the previous fill-up".
 *
 * The dashboard and history screens pair each entry with the previous one to show
 * "+X km". They used to take the next item in an odometer-ordered list, which
 * silently assumed odometer order equals time order.
 */
class FuelDistanceCalculatorTest {

    @Test
    fun firstEntryHasNoDistance() {
        val entries = listOf(fuelEntry(1, at(2026, 1, 1), 10_000_000L))

        assertNull(FuelDistanceCalculator.distanceByEntryId(entries)[1])
    }

    @Test
    fun distanceIsMeasuredFromTheChronologicallyPreviousEntry() {
        val entries = listOf(
            fuelEntry(1, at(2026, 1, 1), 10_000_000L),
            fuelEntry(2, at(2026, 1, 10), 10_500_000L)
        )

        val distances = FuelDistanceCalculator.distanceByEntryId(entries)

        assertNull(distances[1])
        assertEquals(500_000L, distances[2])
    }

    @Test
    fun odometerResetProducesNoPhantomDistance() {
        // The meter is replaced between entry 2 and entry 3.
        val entries = listOf(
            fuelEntry(1, at(2026, 1, 1), 10_000_000L),
            fuelEntry(2, at(2026, 1, 10), 10_500_000L),
            fuelEntry(3, at(2026, 1, 20), 500_000L)
        )

        val distances = FuelDistanceCalculator.distanceByEntryId(entries)

        assertEquals(500_000L, distances[2])
        // The drop from 10,500 km to 500 km is a reset, not a trip.
        assertNull(distances[3])
    }

    @Test
    fun inputOrderDoesNotChangeTheResult() {
        val chronological = listOf(
            fuelEntry(1, at(2026, 1, 1), 10_000_000L),
            fuelEntry(2, at(2026, 1, 10), 10_500_000L),
            fuelEntry(3, at(2026, 1, 20), 10_900_000L)
        )

        val expected = FuelDistanceCalculator.distanceByEntryId(chronological)
        val shuffled = FuelDistanceCalculator.distanceByEntryId(
            listOf(chronological[2], chronological[0], chronological[1])
        )

        assertEquals(expected, shuffled)
    }

    @Test
    fun distanceAccumulatesNormallyAfterReset() {
        val entries = listOf(
            fuelEntry(1, at(2026, 1, 1), 10_000_000L),
            fuelEntry(2, at(2026, 1, 10), 500_000L),  // first reading on the new meter
            fuelEntry(3, at(2026, 1, 20), 900_000L)   // 400 km later
        )

        val distances = FuelDistanceCalculator.distanceByEntryId(entries)

        assertNull(distances[2])
        assertEquals(400_000L, distances[3])
    }

    @Test
    fun emptyInputReturnsEmptyMap() {
        assertEquals(emptyMap<Long, Long?>(), FuelDistanceCalculator.distanceByEntryId(emptyList()))
    }

    private fun at(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, 12, 0, 0)
    }.timeInMillis

    private fun fuelEntry(id: Long, timestamp: Long, odometerMeters: Long) = FuelEntry(
        id = id,
        vehicleId = 1L,
        timestamp = timestamp,
        odometerMeters = odometerMeters,
        fuelTypeId = 1L,
        pricePerLiter = 25_000L,
        amountPaid = 100_000L,
        liters = 4.0,
        isFullTank = true
    )
}
