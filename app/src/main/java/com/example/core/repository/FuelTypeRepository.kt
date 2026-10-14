package com.example.core.repository

import com.example.core.model.FuelType
import kotlinx.coroutines.flow.Flow

interface FuelTypeRepository {
    fun observeActiveFuelTypes(): Flow<List<FuelType>>
    fun observeAllFuelTypes(): Flow<List<FuelType>>
    suspend fun getById(id: Long): FuelType?
    suspend fun getByName(name: String): FuelType?
    suspend fun insert(fuelType: FuelType): Long
    suspend fun update(fuelType: FuelType)
    suspend fun archive(id: Long)
    suspend fun unarchive(id: Long)
    suspend fun getCount(): Int
}
