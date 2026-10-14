package com.example.core.model

data class MaintenanceStats(
    val totalCostVnd: Long = 0L,
    val monthlyCostVnd: Long = 0L,
    val totalEntriesCount: Int = 0,
    val costByCategory: Map<String, Long> = emptyMap(),
    val costByMonth: List<MonthlyCostStat> = emptyList()
)

data class MonthlyCostStat(
    val yearMonthKey: String,
    val displayName: String,
    val costVnd: Long,
    val entriesCount: Int
)
