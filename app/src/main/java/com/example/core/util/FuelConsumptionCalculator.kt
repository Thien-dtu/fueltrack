package com.example.core.util

import com.example.core.model.FuelEntry

data class FuelConsumptionCycle(
    val startEntryId: Long,
    val endEntryId: Long,
    val distanceMeters: Long,
    val liters: Double
) {
    val consumptionLPer100Km: Double?
        get() = FuelCalculator.calculateConsumptionLPer100Km(liters, distanceMeters)
}

object FuelConsumptionCalculator {

    /**
     * Calculates full-tank-to-full-tank cycles, including all refuelling entries
     * after the previous full tank and through the ending full tank.
     */
    fun calculateCycles(entries: List<FuelEntry>): List<FuelConsumptionCycle> {
        val ordered = entries.sortedWith(compareBy<FuelEntry> { it.timestamp }.thenBy { it.id })
        if (ordered.isEmpty()) return emptyList()

        val cycles = mutableListOf<FuelConsumptionCycle>()
        var previousOdometer: Long? = null
        var previousFullTankIndex: Int? = null
        var hasInvalidOdometer = false

        ordered.forEachIndexed { index, entry ->
            if (previousOdometer != null && entry.odometerMeters < previousOdometer!!) {
                previousFullTankIndex = null
                hasInvalidOdometer = true
            }

            if (entry.isFullTank) {
                val startIndex = previousFullTankIndex
                if (startIndex != null && !hasInvalidOdometer) {
                    val start = ordered[startIndex]
                    val distanceMeters = entry.odometerMeters - start.odometerMeters
                    val liters = ordered.subList(startIndex + 1, index + 1).sumOf { it.liters }
                    if (distanceMeters > 0L && liters > 0.0) {
                        cycles += FuelConsumptionCycle(
                            startEntryId = start.id,
                            endEntryId = entry.id,
                            distanceMeters = distanceMeters,
                            liters = liters
                        )
                    }
                }
                previousFullTankIndex = index
                hasInvalidOdometer = false
            }

            previousOdometer = entry.odometerMeters
        }

        return cycles
    }

    fun consumptionByEndingEntryId(entries: List<FuelEntry>): Map<Long, Double> {
        return calculateCycles(entries).mapNotNull { cycle ->
            cycle.consumptionLPer100Km?.let { cycle.endEntryId to it }
        }.toMap()
    }
}
