package com.example.core.model

data class FuelEntry(
    val id: Long = 0,
    val vehicleId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val odometerMeters: Long,
    val fuelTypeId: Long,
    val pricePerLiter: Long,
    val amountPaid: Long,
    val liters: Double,
    val isFullTank: Boolean = true,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayOdoKm: Double
        get() = odometerMeters / 1000.0
}

data class FuelEntryWithDetails(
    val entry: FuelEntry,
    val fuelTypeName: String,
    val vehicleName: String,
    val distanceSincePreviousMeters: Long? = null,
    val consumptionLPer100Km: Double? = null,
    val costPerKmVnd: Double? = null
)
