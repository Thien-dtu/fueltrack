package com.example.core.util

import com.example.core.model.FuelEntry
import com.example.core.model.MonthlyFuelStats
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatisticsCalculator {

    private val monthKeyFormat = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM", Locale.US) }
    private val monthDisplayFormat = ThreadLocal.withInitial {
        SimpleDateFormat("'Tháng' M, yyyy", Locale.forLanguageTag("vi-VN"))
    }

    /**
     * Calculates stats grouped by calendar month from a vehicle's fuel entries.
     * Raw facts: odometerMeters, timestamp, liters, amountPaid, isFullTank.
     * All distance computations use meters internally.
     *
     * Distance is attributed chronologically: every entry contributes the distance
     * driven since the previous entry (by timestamp), and that distance is counted
     * in the month of the entry that ended the leg. Ordering by odometer instead
     * would misattribute legs whenever the meter is replaced or rolled back.
     */
    fun computeMonthlyStats(entries: List<FuelEntry>): List<MonthlyFuelStats> {
        if (entries.isEmpty()) return emptyList()

        val chronological = entries.sortedWith(compareBy<FuelEntry> { it.timestamp }.thenBy { it.id })
        val monthKeyFormatter = monthKeyFormat.get()!!
        val displayFormatter = monthDisplayFormat.get()!!

        fun monthKeyOf(entry: FuelEntry): String = monthKeyFormatter.format(Date(entry.timestamp))

        // Distance driven on each entry's leg, counted in that entry's month.
        val distanceByMonth = LinkedHashMap<String, Long>()
        var previousOdometer: Long? = null
        for (entry in chronological) {
            val previous = previousOdometer
            // A negative delta means the odometer was replaced or rolled back: the
            // jump is not a distance, so this leg contributes nothing.
            val legMeters = if (previous != null) (entry.odometerMeters - previous).coerceAtLeast(0L) else 0L
            val monthKey = monthKeyOf(entry)
            distanceByMonth[monthKey] = (distanceByMonth[monthKey] ?: 0L) + legMeters
            previousOdometer = entry.odometerMeters
        }

        // One cycle pass drives both consumption and cost. Calling
        // FuelCostCalculator.calculatePeriods() as well would sort the list again and
        // recompute the same full-tank cycles for the second number.
        val cycles = FuelConsumptionCalculator.calculateCycles(chronological)
        val indexById = HashMap<Long, Int>(chronological.size)
        chronological.forEachIndexed { index, entry -> indexById[entry.id] = index }

        val entryById = entries.associateBy { it.id }

        fun monthKeyOfId(entryId: Long): String? =
            entryById[entryId]?.let { monthKeyFormatter.format(Date(it.timestamp)) }

        val cyclesByMonth = cycles
            .mapNotNull { cycle -> monthKeyOfId(cycle.endEntryId)?.let { it to cycle } }
            .groupBy({ it.first }, { it.second })

        // Cost of each cycle: every purchase after the opening full tank, through the
        // closing one.
        val cycleCostByMonth = HashMap<String, Long>()
        val cycleCostDistanceByMonth = HashMap<String, Long>()
        for (cycle in cycles) {
            val monthKey = monthKeyOfId(cycle.endEntryId) ?: continue
            val startIndex = indexById[cycle.startEntryId] ?: continue
            val endIndex = indexById[cycle.endEntryId] ?: continue
            if (endIndex <= startIndex) continue
            val costVnd = chronological.subList(startIndex + 1, endIndex + 1).sumOf { it.amountPaid }
            cycleCostByMonth[monthKey] = (cycleCostByMonth[monthKey] ?: 0L) + costVnd
            cycleCostDistanceByMonth[monthKey] =
                (cycleCostDistanceByMonth[monthKey] ?: 0L) + cycle.distanceMeters
        }

        val entriesByMonth = chronological.groupBy { monthKeyOf(it) }

        val resultList = mutableListOf<MonthlyFuelStats>()

        for ((monthKey, monthEntries) in entriesByMonth) {
            val totalFuelLiters = monthEntries.sumOf { it.liters }
            val totalCostVnd = monthEntries.sumOf { it.amountPaid }
            val totalDistanceMeters = distanceByMonth[monthKey] ?: 0L

            val monthCycles = cyclesByMonth[monthKey].orEmpty()
            val fullTankDistanceMeters = monthCycles.sumOf { it.distanceMeters }
            val fullTankLiters = monthCycles.sumOf { it.liters }

            val avgConsumption: Double? = if (fullTankDistanceMeters > 0L && fullTankLiters > 0.0) {
                FuelCalculator.calculateConsumptionLPer100Km(fullTankLiters, fullTankDistanceMeters)
            } else {
                null
            }

            // Cost per km uses the same full-tank cycle as consumption: fuel bought in
            // one month mostly pays for the next month's driving, so dividing a month's
            // spend by that same month's distance shifts the cost by a whole cycle.
            val cycleCostVnd = cycleCostByMonth[monthKey] ?: 0L
            val cycleDistanceMeters = cycleCostDistanceByMonth[monthKey] ?: 0L
            val avgCostPerKm: Double? = if (cycleDistanceMeters > 0L && cycleCostVnd > 0L) {
                FuelCalculator.calculateCostPerKm(cycleCostVnd, cycleDistanceMeters)
            } else {
                null
            }

            val displayName = displayFormatter.format(Date(monthEntries.first().timestamp))

            resultList.add(
                MonthlyFuelStats(
                    yearMonthKey = monthKey,
                    displayName = displayName,
                    totalDistanceMeters = totalDistanceMeters,
                    totalFuelLiters = BigDecimal(totalFuelLiters).setScale(2, RoundingMode.HALF_UP).toDouble(),
                    totalCostVnd = totalCostVnd,
                    averageConsumptionLPer100Km = avgConsumption,
                    averageCostPerKmVnd = avgCostPerKm,
                    entriesCount = monthEntries.size
                )
            )
        }

        // Return latest month first
        return resultList.sortedByDescending { it.yearMonthKey }
    }
}
