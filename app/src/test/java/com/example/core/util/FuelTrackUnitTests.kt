package com.example.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.core.model.FuelEntry

class FuelTrackUnitTests {

    @Test
    fun testFuelConsumptionIncludesTopUpsBetweenFullTanks() {
        val entries = listOf(
            fuelEntry(1, 10_000_000L, 4.0, true, 1L),
            fuelEntry(2, 10_100_000L, 2.0, false, 2L),
            fuelEntry(3, 10_250_000L, 5.0, true, 3L)
        )

        val cycle = FuelConsumptionCalculator.calculateCycles(entries).single()

        assertEquals(7.0, cycle.liters, 0.001)
        assertEquals(2.8, cycle.consumptionLPer100Km!!, 0.01)
    }

    @Test
    fun testFuelCostIncludesTopUpsBetweenFullTanks() {
        val entries = listOf(
            fuelEntry(1, 10_000_000L, 4.0, true, 1L, 100_000L),
            fuelEntry(2, 10_100_000L, 2.0, false, 2L, 50_000L),
            fuelEntry(3, 10_250_000L, 5.0, true, 3L, 125_000L)
        )

        val period = FuelCostCalculator.calculatePeriods(entries).single()

        assertEquals(175_000L, period.costVnd)
        assertEquals(700.0, period.costPerKmVnd!!, 0.01)
    }

    @Test
    fun testFuelConsumptionNeedsTwoValidFullTankMarkers() {
        val entries = listOf(fuelEntry(1, 10_000_000L, 4.0, true, 1L))

        assertTrue(FuelConsumptionCalculator.calculateCycles(entries).isEmpty())
    }

    @Test
    fun testFuelConsumptionSupportsConsecutiveFullTankCycles() {
        val entries = listOf(
            fuelEntry(1, 10_000_000L, 4.0, true, 1L),
            fuelEntry(2, 10_100_000L, 3.0, true, 2L),
            fuelEntry(3, 10_200_000L, 2.0, true, 3L)
        )

        val cycles = FuelConsumptionCalculator.calculateCycles(entries)

        assertEquals(2, cycles.size)
        assertEquals(3.0, cycles[0].liters, 0.001)
        assertEquals(2.0, cycles[1].liters, 0.001)
    }

    @Test
    fun testFuelConsumptionIgnoresDuplicateOdometerAndResetsAfterRollback() {
        val entries = listOf(
            fuelEntry(1, 10_000_000L, 4.0, true, 1L),
            fuelEntry(2, 10_000_000L, 2.0, false, 2L),
            fuelEntry(3, 10_100_000L, 3.0, true, 3L),
            fuelEntry(4, 9_900_000L, 2.0, false, 4L),
            fuelEntry(5, 10_200_000L, 5.0, true, 5L)
        )

        val cycles = FuelConsumptionCalculator.calculateCycles(entries)

        assertEquals(1, cycles.size)
        assertEquals(5.0, cycles.single().liters, 0.001)
        assertEquals(5.0, cycles.single().consumptionLPer100Km!!, 0.01)
    }

    private fun fuelEntry(
        id: Long,
        odometerMeters: Long,
        liters: Double,
        isFullTank: Boolean,
        timestamp: Long,
        amountPaid: Long = 1L
    ) = FuelEntry(
        id = id,
        vehicleId = 1L,
        timestamp = timestamp,
        odometerMeters = odometerMeters,
        fuelTypeId = 1L,
        pricePerLiter = 1L,
        amountPaid = amountPaid,
        liters = liters,
        isFullTank = isFullTank
    )

    @Test
    fun testOdometerParser_validIntegers() {
        val r1 = OdometerParser.parse("1222")
        assertTrue(r1 is OdometerParser.ParseResult.Success)
        assertEquals(1_222_000L, (r1 as OdometerParser.ParseResult.Success).meters)

        val r0 = OdometerParser.parse("0")
        assertTrue(r0 is OdometerParser.ParseResult.Success)
        assertEquals(0L, (r0 as OdometerParser.ParseResult.Success).meters)
    }

    @Test
    fun testOdometerParser_validDecimals() {
        // "1222,0" -> 1,222,000 meters
        val r1 = OdometerParser.parse("1222,0")
        assertTrue(r1 is OdometerParser.ParseResult.Success)
        assertEquals(1_222_000L, (r1 as OdometerParser.ParseResult.Success).meters)

        // "1222,4" -> 1,222,400 meters
        val r2 = OdometerParser.parse("1222,4")
        assertTrue(r2 is OdometerParser.ParseResult.Success)
        assertEquals(1_222_400L, (r2 as OdometerParser.ParseResult.Success).meters)

        // Dot separator also accepted "1222.4"
        val r3 = OdometerParser.parse("1222.4")
        assertTrue(r3 is OdometerParser.ParseResult.Success)
        assertEquals(1_222_400L, (r3 as OdometerParser.ParseResult.Success).meters)

        // "0,5" -> 500 meters
        val r4 = OdometerParser.parse("0,5")
        assertTrue(r4 is OdometerParser.ParseResult.Success)
        assertEquals(500L, (r4 as OdometerParser.ParseResult.Success).meters)
    }

    @Test
    fun testOdometerParser_invalidInputs() {
        // More than 1 decimal place: 1222,45 -> invalid
        val r1 = OdometerParser.parse("1222,45")
        assertTrue(r1 is OdometerParser.ParseResult.Error)

        // 1222,456 -> invalid
        val r2 = OdometerParser.parse("1222,456")
        assertTrue(r2 is OdometerParser.ParseResult.Error)

        // Negative numbers: -1222 -> invalid
        val r3 = OdometerParser.parse("-1222")
        assertTrue(r3 is OdometerParser.ParseResult.Error)

        // Non-numeric or empty
        assertTrue(OdometerParser.parse("") is OdometerParser.ParseResult.Error)
        assertTrue(OdometerParser.parse("abc") is OdometerParser.ParseResult.Error)
    }

    @Test
    fun testCalculateLiters() {
        // 50,000 / 25,000 = 2.0 L
        val liters = FuelCalculator.calculateLiters(50000L, 25000L)
        assertEquals(2.0, liters, 0.001)

        // Zero or negative price/amount
        assertEquals(0.0, FuelCalculator.calculateLiters(0L, 25000L), 0.001)
        assertEquals(0.0, FuelCalculator.calculateLiters(50000L, 0L), 0.001)
        assertEquals(0.0, FuelCalculator.calculateLiters(-1000L, 25000L), 0.001)
    }

    @Test
    fun testCalculateDistance() {
        // 10,000.0 km = 10,000,000 m; 10,250.4 km = 10,250,400 m
        val dist = FuelCalculator.calculateDistance(10_000_000L, 10_250_400L)
        assertEquals(250_400L, dist)

        // Current ODO < Previous ODO (rollback/reset)
        val rollback = FuelCalculator.calculateDistance(12_000_000L, 10_000_000L)
        assertNull(rollback)
    }

    @Test
    fun testCalculateConsumption() {
        // 250 km = 250,000 m; Liters = 2.8 L
        // Consumption = (2.8 / 250) * 100 = 1.12 L/100km
        val consumption = FuelCalculator.calculateConsumptionLPer100Km(2.8, 250_000L)
        assertEquals(1.12, consumption!!, 0.01)

        // Zero distance or zero liters
        assertNull(FuelCalculator.calculateConsumptionLPer100Km(0.0, 250_000L))
        assertNull(FuelCalculator.calculateConsumptionLPer100Km(2.8, 0L))
    }

    @Test
    fun testCalculateCostPerKm() {
        // 50,000 VND over 250 km (250,000 m) = 200 VND / km
        val cost = FuelCalculator.calculateCostPerKm(50000L, 250_000L)
        assertEquals(200.0, cost!!, 0.01)

        // Zero distance
        assertNull(FuelCalculator.calculateCostPerKm(50000L, 0L))
    }
}
