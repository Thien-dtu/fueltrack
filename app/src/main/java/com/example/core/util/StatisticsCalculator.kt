package com.example.core.util

import com.example.core.model.FuelEntry
import com.example.core.model.MonthlyFuelStats
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StatisticsCalculator {

    /**
     * Calculates stats grouped by calendar month from a vehicle's chronological fuel entries.
     * Raw facts: odometerMeters, timestamp, liters, amountPaid, isFullTank.
     * All distance computations use meters internally.
     */
    fun computeMonthlyStats(entries: List<FuelEntry>): List<MonthlyFuelStats> {
        if (entries.isEmpty()) return emptyList()

        val sortedAsc = entries.sortedWith(compareBy<FuelEntry> { it.odometerMeters }.thenBy { it.timestamp })

        val cal = Calendar.getInstance()
        val monthFormatKey = SimpleDateFormat("yyyy-MM", Locale.US)
        val monthFormatDisplay = SimpleDateFormat("'Tháng' M, yyyy", Locale.forLanguageTag("vi-VN"))
        val entryById = entries.associateBy { it.id }
        val cyclesByMonth = FuelConsumptionCalculator.calculateCycles(entries)
            .mapNotNull { cycle ->
                entryById[cycle.endEntryId]?.let { cycle to monthFormatKey.format(Date(it.timestamp)) }
            }
            .groupBy({ it.second }, { it.first })

        // Group entries by month key (e.g. "2026-10")
        val groupedByMonth = sortedAsc.groupBy { entry ->
            cal.timeInMillis = entry.timestamp
            monthFormatKey.format(Date(entry.timestamp))
        }

        val resultList = mutableListOf<MonthlyFuelStats>()

        // For consumption, we track consecutive full-tank pairs
        // We can look at pairwise entries in the vehicle's history
        for ((monthKey, monthEntries) in groupedByMonth) {
            val totalFuelLiters = monthEntries.sumOf { it.liters }
            val totalCostVnd = monthEntries.sumOf { it.amountPaid }

            // Monthly distance: difference between max and min ODO in this month's entries,
            // or if there is a previous entry before this month, from the closest prior entry.
            val firstEntryInMonth = monthEntries.first()
            val priorEntry = sortedAsc.lastOrNull { it.odometerMeters < firstEntryInMonth.odometerMeters }

            val baselineOdo = priorEntry?.odometerMeters ?: firstEntryInMonth.odometerMeters
            val maxOdoInMonth = monthEntries.maxOf { it.odometerMeters }
            val totalDistanceMeters = (maxOdoInMonth - baselineOdo).coerceAtLeast(0L)

            val monthCycles = cyclesByMonth[monthKey].orEmpty()
            val fullTankDistanceMeters = monthCycles.sumOf { it.distanceMeters }
            val fullTankLiters = monthCycles.sumOf { it.liters }

            val avgConsumption: Double? = if (fullTankDistanceMeters > 0L && fullTankLiters > 0.0) {
                FuelCalculator.calculateConsumptionLPer100Km(fullTankLiters, fullTankDistanceMeters)
            } else {
                null
            }

            val avgCostPerKm: Double? = if (totalDistanceMeters > 0L && totalCostVnd > 0L) {
                FuelCalculator.calculateCostPerKm(totalCostVnd, totalDistanceMeters)
            } else {
                null
            }

            cal.timeInMillis = monthEntries.first().timestamp
            val displayName = monthFormatDisplay.format(Date(monthEntries.first().timestamp))

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
