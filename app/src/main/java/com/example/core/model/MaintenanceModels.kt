package com.example.core.model

data class MaintenanceEntry(
    val id: Long = 0,
    val vehicleId: Long,
    val templateId: Long? = null,
    val title: String,
    val category: String = "Bảo dưỡng",
    val description: String = "",
    val performedAt: Long = System.currentTimeMillis(),
    val odometerMeters: Long? = null,
    val cost: Long? = null, // Long VND, null represents unknown/unrecorded
    val providerName: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayOdoKm: Double?
        get() = odometerMeters?.let { it / 1000.0 }
}

data class MaintenanceSchedule(
    val id: Long = 0,
    val vehicleId: Long,
    val templateId: Long? = null,
    val title: String,
    val category: String = "Bảo dưỡng",
    val dueAt: Long? = null,
    val dueOdometerMeters: Long? = null,
    val intervalDays: Int? = null,
    val intervalMeters: Long? = null,
    val lastCompletedAt: Long? = null,
    val lastCompletedOdometerMeters: Long? = null,
    val status: ScheduleLifecycleStatus = ScheduleLifecycleStatus.ACTIVE,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayDueOdoKm: Double?
        get() = dueOdometerMeters?.let { it / 1000.0 }

    val displayIntervalKm: Double?
        get() = intervalMeters?.let { it / 1000.0 }
}

data class MaintenanceScheduleWithStatus(
    val schedule: MaintenanceSchedule,
    val vehicleName: String,
    val currentOdometerMeters: Long?,
    val dueStatus: MaintenanceDueStatus,
    val triggerReason: String
)
