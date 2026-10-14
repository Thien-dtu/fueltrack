package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.MaintenanceScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceScheduleDao {
    @Query("SELECT * FROM maintenance_schedules WHERE vehicleId = :vehicleId AND isArchived = 0 ORDER BY dueAt ASC, dueOdometerMeters ASC")
    fun observeActiveSchedulesForVehicle(vehicleId: Long): Flow<List<MaintenanceScheduleEntity>>

    @Query("SELECT * FROM maintenance_schedules WHERE vehicleId = :vehicleId AND isArchived = 0 AND status = 'ACTIVE' ORDER BY dueAt ASC, dueOdometerMeters ASC")
    suspend fun getActiveSchedulesForVehicleSync(vehicleId: Long): List<MaintenanceScheduleEntity>

    @Query("SELECT * FROM maintenance_schedules WHERE isArchived = 0 ORDER BY dueAt ASC, dueOdometerMeters ASC")
    fun observeAllActiveSchedules(): Flow<List<MaintenanceScheduleEntity>>

    @Query("SELECT * FROM maintenance_schedules WHERE isArchived = 0 AND status = 'ACTIVE' ORDER BY dueAt ASC, dueOdometerMeters ASC")
    suspend fun getAllActiveSchedulesSync(): List<MaintenanceScheduleEntity>

    @Query("SELECT * FROM maintenance_schedules WHERE vehicleId = :vehicleId ORDER BY dueAt ASC, dueOdometerMeters ASC")
    fun observeAllSchedulesForVehicle(vehicleId: Long): Flow<List<MaintenanceScheduleEntity>>

    @Query("SELECT * FROM maintenance_schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: Long): MaintenanceScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: MaintenanceScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: MaintenanceScheduleEntity)

    @Query("UPDATE maintenance_schedules SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE maintenance_schedules SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveSchedule(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM maintenance_schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)
}
