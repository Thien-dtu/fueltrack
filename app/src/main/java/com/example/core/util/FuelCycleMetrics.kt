package com.example.core.util

import com.example.core.model.FuelEntry

/**
 * Per-entry figures derived from full-tank cycles, computed in one pass.
 *
 * Consumption and cost per km are both defined over the same full-tank cycle, and
 * both call sites (dashboard, history) need both numbers. Calling
 * [FuelConsumptionCalculator.calculateCycles] and
 * [FuelCostCalculator.calculatePeriods] separately sorts the list twice and walks
 * the cycles twice for one screen; this bundles them behind a single pass.
 */
data class FuelCycleMetrics(
    /** Litres per 100 km for the cycle ending at this entry id. */
    val consumptionByEntryId: Map<Long, Double>,
    /** VND per km for the cycle ending at this entry id. */
    val costPerKmByEntryId: Map<Long, Double>
) {
    companion object {
        val EMPTY = FuelCycleMetrics(emptyMap(), emptyMap())
    }
}

object FuelCycleMetricsCalculator {

    fun calculate(entries: List<FuelEntry>): FuelCycleMetrics {
        if (entries.isEmpty()) return FuelCycleMetrics.EMPTY

        val ordered = entries.sortedWith(compareBy<FuelEntry> { it.timestamp }.thenBy { it.id })
        val cycles = FuelConsumptionCalculator.calculateCycles(ordered)
        if (cycles.isEmpty()) return FuelCycleMetrics.EMPTY

        // Index the ordered list once so per-cycle cost lookups stay O(1) instead of
        // rescanning the list with indexOfFirst() for every cycle.
        val indexById = HashMap<Long, Int>(ordered.size)
        ordered.forEachIndexed { index, entry -> indexById[entry.id] = index }

        val consumption = LinkedHashMap<Long, Double>(cycles.size)
        val costPerKm = LinkedHashMap<Long, Double>(cycles.size)

        for (cycle in cycles) {
            cycle.consumptionLPer100Km?.let { consumption[cycle.endEntryId] = it }

            val startIndex = indexById[cycle.startEntryId]
            val endIndex = indexById[cycle.endEntryId]
            if (startIndex == null || endIndex == null || endIndex <= startIndex) continue

            val costVnd = ordered.subList(startIndex + 1, endIndex + 1).sumOf { it.amountPaid }
            FuelCalculator.calculateCostPerKm(costVnd, cycle.distanceMeters)
                ?.let { costPerKm[cycle.endEntryId] = it }
        }

        return FuelCycleMetrics(consumption, costPerKm)
    }
}
