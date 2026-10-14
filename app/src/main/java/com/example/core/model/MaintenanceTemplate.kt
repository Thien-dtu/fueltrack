package com.example.core.model

data class MaintenanceTemplate(
    val id: Long = 0,
    val name: String,
    val category: String = "Định kỳ",
    val defaultIntervalDays: Int? = null,
    val defaultIntervalMeters: Long? = null,
    val description: String = "",
    val isBuiltIn: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayDefaultIntervalKm: Double?
        get() = defaultIntervalMeters?.let { it / 1000.0 }
}
