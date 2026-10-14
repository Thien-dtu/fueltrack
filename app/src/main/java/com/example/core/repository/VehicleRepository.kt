package com.example.core.repository

import com.example.core.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun observeActiveVehicles(): Flow<List<Vehicle>>
    fun observeAllVehicles(): Flow<List<Vehicle>>
    fun observeVehicle(id: Long): Flow<Vehicle?>
    suspend fun getVehicle(id: Long): Vehicle?
    suspend fun insert(vehicle: Vehicle): Long
    suspend fun update(vehicle: Vehicle)
    suspend fun archive(id: Long)
    suspend fun unarchive(id: Long)
    suspend fun updateCurrentOdometer(vehicleId: Long, odometerMeters: Long)
    suspend fun getActiveCount(): Int
}
