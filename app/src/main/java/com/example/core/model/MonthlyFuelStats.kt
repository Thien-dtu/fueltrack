package com.example.core.model

data class MonthlyFuelStats(
    val yearMonthKey: String, // e.g. "2026-10"
    val displayName: String,   // e.g. "Tháng 10, 2026"
    val totalDistanceMeters: Long,
    val totalFuelLiters: Double,
    val totalCostVnd: Long,
    val averageConsumptionLPer100Km: Double?,
    val averageCostPerKmVnd: Double?,
    val entriesCount: Int
)

data class DashboardMetrics(
    val currentOdometerMeters: Long = 0L,
    val monthlyDistanceMeters: Long = 0L,
    val monthlyFuelLiters: Double = 0.0,
    val monthlyCostVnd: Long = 0L,
    val averageConsumptionLPer100Km: Double? = null,
    val costPerKmVnd: Double? = null
)
