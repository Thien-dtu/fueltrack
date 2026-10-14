package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.MaintenanceEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceEntryDao {
    @Query("SELECT * FROM maintenance_entries WHERE vehicleId = :vehicleId ORDER BY performedAt DESC, id DESC")
    fun observeEntriesForVehicle(vehicleId: Long): Flow<List<MaintenanceEntryEntity>>

    @Query("SELECT * FROM maintenance_entries ORDER BY performedAt DESC, id DESC")
    fun observeAllEntries(): Flow<List<MaintenanceEntryEntity>>

    @Query("SELECT * FROM maintenance_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): MaintenanceEntryEntity?

    @Query("SELECT * FROM maintenance_entries WHERE vehicleId = :vehicleId AND performedAt >= :startTime AND performedAt <= :endTime ORDER BY performedAt DESC")
    suspend fun getEntriesInRange(vehicleId: Long, startTime: Long, endTime: Long): List<MaintenanceEntryEntity>

    @Query("SELECT * FROM maintenance_entries WHERE performedAt >= :startTime AND performedAt <= :endTime ORDER BY performedAt DESC")
    suspend fun getAllEntriesInRange(startTime: Long, endTime: Long): List<MaintenanceEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: MaintenanceEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: MaintenanceEntryEntity)

    @Query("DELETE FROM maintenance_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("DELETE FROM maintenance_entries WHERE vehicleId = :vehicleId")
    suspend fun deleteEntriesForVehicle(vehicleId: Long)
}
