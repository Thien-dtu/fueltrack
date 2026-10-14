package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.Vehicle
import com.example.core.model.VehicleType

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // MOTORCYCLE | CAR
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
    fun toDomain(): Vehicle {
        return Vehicle(
            id = id,
            name = name,
            type = if (type == "CAR") VehicleType.CAR else VehicleType.MOTORCYCLE,
            brand = brand,
            model = model,
            year = year,
            licensePlate = licensePlate,
            initialOdometerMeters = initialOdometerMeters,
            currentOdometerMeters = currentOdometerMeters,
            fuelTankCapacityLiters = fuelTankCapacityLiters,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isArchived = isArchived
        )
    }

    companion object {
        fun fromDomain(vehicle: Vehicle): VehicleEntity {
            return VehicleEntity(
                id = vehicle.id,
                name = vehicle.name,
                type = vehicle.type.name,
                brand = vehicle.brand,
                model = vehicle.model,
                year = vehicle.year,
                licensePlate = vehicle.licensePlate,
                initialOdometerMeters = vehicle.initialOdometerMeters,
                currentOdometerMeters = vehicle.currentOdometerMeters,
                fuelTankCapacityLiters = vehicle.fuelTankCapacityLiters,
                createdAt = vehicle.createdAt,
                updatedAt = vehicle.updatedAt,
                isArchived = vehicle.isArchived
            )
        }
    }
}
