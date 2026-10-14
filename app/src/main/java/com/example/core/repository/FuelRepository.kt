package com.example.core.repository

import com.example.core.model.FuelEntry
import kotlinx.coroutines.flow.Flow

interface FuelRepository {
    fun observeEntriesForVehicle(vehicleId: Long): Flow<List<FuelEntry>>
    fun observeAllEntries(): Flow<List<FuelEntry>>
    suspend fun getById(id: Long): FuelEntry?
    suspend fun getLatestForVehicle(vehicleId: Long): FuelEntry?
    suspend fun getPreviousEntryByOdo(vehicleId: Long, odoMeters: Long): FuelEntry?
    suspend fun getEntriesInRange(vehicleId: Long, startTime: Long, endTime: Long): List<FuelEntry>
    suspend fun getAllEntriesAscForVehicle(vehicleId: Long): List<FuelEntry>
    suspend fun insert(entry: FuelEntry): Long
    suspend fun update(entry: FuelEntry)
    suspend fun delete(id: Long)
}
