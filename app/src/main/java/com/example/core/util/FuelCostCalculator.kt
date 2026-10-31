package com.example.core.util

import com.example.core.model.FuelEntry

data class FuelCostPeriod(
    val startEntryId: Long,
    val endEntryId: Long,
    val distanceMeters: Long,
    val costVnd: Long
) {
    val costPerKmVnd: Double?
        get() = FuelCalculator.calculateCostPerKm(costVnd, distanceMeters)
}

object FuelCostCalculator {

    /**
     * Calculates recorded fuel cost for the same reliable periods as consumption:
     * all purchases after one full tank and through the next full tank, divided
     * by the distance between those full-tank markers.
     */
    fun calculatePeriods(entries: List<FuelEntry>): List<FuelCostPeriod> {
        val ordered = entries.sortedWith(compareBy<FuelEntry> { it.timestamp }.thenBy { it.id })
        return FuelConsumptionCalculator.calculateCycles(ordered).mapNotNull { cycle ->
            val startIndex = ordered.indexOfFirst { it.id == cycle.startEntryId }
            val endIndex = ordered.indexOfFirst { it.id == cycle.endEntryId }
            if (startIndex < 0 || endIndex <= startIndex) return@mapNotNull null

            FuelCostPeriod(
                startEntryId = cycle.startEntryId,
                endEntryId = cycle.endEntryId,
                distanceMeters = cycle.distanceMeters,
                costVnd = ordered.subList(startIndex + 1, endIndex + 1).sumOf { it.amountPaid }
            )
        }
    }

    fun costByEndingEntryId(entries: List<FuelEntry>): Map<Long, Double> {
        return calculatePeriods(entries).mapNotNull { period ->
            period.costPerKmVnd?.let { period.endEntryId to it }
        }.toMap()
    }
}
