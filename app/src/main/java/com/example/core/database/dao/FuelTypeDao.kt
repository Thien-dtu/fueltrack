package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.FuelTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelTypeDao {
    @Query("SELECT * FROM fuel_types WHERE isArchived = 0 ORDER BY name ASC")
    fun observeActiveFuelTypes(): Flow<List<FuelTypeEntity>>

    @Query("SELECT * FROM fuel_types ORDER BY name ASC")
    fun observeAllFuelTypes(): Flow<List<FuelTypeEntity>>

    @Query("SELECT * FROM fuel_types WHERE id = :id LIMIT 1")
    suspend fun getFuelTypeById(id: Long): FuelTypeEntity?

    @Query("SELECT * FROM fuel_types WHERE lower(name) = lower(:name) LIMIT 1")
    suspend fun getFuelTypeByName(name: String): FuelTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelType(fuelType: FuelTypeEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(fuelTypes: List<FuelTypeEntity>)

    @Update
    suspend fun updateFuelType(fuelType: FuelTypeEntity)

    @Query("UPDATE fuel_types SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveFuelType(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE fuel_types SET isArchived = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun unarchiveFuelType(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM fuel_types")
    suspend fun getFuelTypeCount(): Int
}
