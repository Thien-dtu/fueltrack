package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.FuelEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelEntryDao {
    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY odometerMeters DESC, timestamp DESC")
    fun observeEntriesForVehicle(vehicleId: Long): Flow<List<FuelEntryEntity>>

    @Query("SELECT * FROM fuel_entries ORDER BY timestamp DESC")
    fun observeAllEntries(): Flow<List<FuelEntryEntity>>

    @Query("SELECT * FROM fuel_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): FuelEntryEntity?

    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY odometerMeters DESC, timestamp DESC LIMIT 1")
    suspend fun getLatestEntryForVehicle(vehicleId: Long): FuelEntryEntity?

    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId AND odometerMeters < :odoMeters ORDER BY odometerMeters DESC LIMIT 1")
    suspend fun getPreviousEntryByOdo(vehicleId: Long, odoMeters: Long): FuelEntryEntity?

    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY odometerMeters ASC")
    suspend fun getEntriesInRange(vehicleId: Long, startTime: Long, endTime: Long): List<FuelEntryEntity>

    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY odometerMeters ASC")
    suspend fun getAllEntriesAscForVehicle(vehicleId: Long): List<FuelEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: FuelEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: FuelEntryEntity)

    @Query("DELETE FROM fuel_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("DELETE FROM fuel_entries WHERE vehicleId = :vehicleId")
    suspend fun deleteEntriesForVehicle(vehicleId: Long)
}
