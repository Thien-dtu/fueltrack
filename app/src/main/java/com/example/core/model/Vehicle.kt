package com.example.core.model

enum class VehicleType {
    MOTORCYCLE,
    CAR
}

data class Vehicle(
    val id: Long = 0,
    val name: String,
    val type: VehicleType,
    val brand: String = "",
    val model: String = "",
    val year: Int? = null,
    val licensePlate: String = "",
    val initialOdometerMeters: Long = 0L,
    val currentOdometerMeters: Long = 0L,
    val fuelTankCapacityLiters: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
) {
    val displayInitialOdoKm: Double
        get() = initialOdometerMeters / 1000.0

    val displayCurrentOdoKm: Double
        get() = currentOdometerMeters / 1000.0
}
