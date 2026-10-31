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

    @Query(
        "SELECT EXISTS(" +
            "SELECT 1 FROM fuel_entries " +
            "WHERE vehicleId = :vehicleId AND odometerMeters = :odoMeters AND id != :excludedEntryId" +
            ")"
    )
    suspend fun hasOtherEntryAtOdometer(
        vehicleId: Long,
        odoMeters: Long,
        excludedEntryId: Long
    ): Boolean

    @Query("SELECT MAX(odometerMeters) FROM fuel_entries WHERE vehicleId = :vehicleId")
    suspend fun getMaxOdometerForVehicle(vehicleId: Long): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: FuelEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: FuelEntryEntity)

    @Query("DELETE FROM fuel_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)
}
