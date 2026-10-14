package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun observeActiveVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY createdAt DESC")
    fun observeAllVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    suspend fun getVehicleById(id: Long): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE id = :id LIMIT 1")
    fun observeVehicleById(id: Long): Flow<VehicleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long

    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Query("UPDATE vehicles SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveVehicle(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE vehicles SET isArchived = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun unarchiveVehicle(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE vehicles SET currentOdometerMeters = :odometerMeters, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCurrentOdometer(id: Long, odometerMeters: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM vehicles WHERE isArchived = 0")
    suspend fun getActiveVehicleCount(): Int
}
