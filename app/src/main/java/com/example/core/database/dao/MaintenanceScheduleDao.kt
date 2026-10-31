package com.example.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.MaintenanceScheduleEntity
import kotlinx.coroutines.flow.Flow

/**
 * A schedule joined with the few vehicle fields the reminder path needs.
 *
 * The daily audit used to fetch every schedule and then call getVehicleById() once
 * per schedule. With N schedules that is N+1 queries inside a background worker;
 * this projection gets the same data in one statement.
 */
data class ScheduleWithVehicle(
    @Embedded val schedule: MaintenanceScheduleEntity,
    // Aliased away from "vehicleId": that column already belongs to the embedded
    // schedule entity, and reusing the name makes Room map it twice.
    @ColumnInfo(name = "joinedVehicleId") val vehicleId: Long,
    @ColumnInfo(name = "vehicleName") val vehicleName: String,
    @ColumnInfo(name = "vehicleCurrentOdometerMeters") val vehicleCurrentOdometerMeters: Long
)

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

    /**
     * Active, non-archived schedules together with their vehicle, skipping archived
     * vehicles. Used by the periodic audit so it does not query per schedule.
     */
    @Query(
        """
        SELECT s.*, v.id AS joinedVehicleId, v.name AS vehicleName,
               v.currentOdometerMeters AS vehicleCurrentOdometerMeters
        FROM maintenance_schedules s
        INNER JOIN vehicles v ON v.id = s.vehicleId
        WHERE s.isArchived = 0
          AND s.status = 'ACTIVE'
          AND v.isArchived = 0
        ORDER BY s.dueAt ASC, s.dueOdometerMeters ASC
        """
    )
    suspend fun getActiveSchedulesWithVehicle(): List<ScheduleWithVehicle>

    /** Active schedules of one vehicle, joined with that vehicle, in a single query. */
    @Query(
        """
        SELECT s.*, v.id AS joinedVehicleId, v.name AS vehicleName,
               v.currentOdometerMeters AS vehicleCurrentOdometerMeters
        FROM maintenance_schedules s
        INNER JOIN vehicles v ON v.id = s.vehicleId
        WHERE s.vehicleId = :vehicleId
          AND s.isArchived = 0
          AND s.status = 'ACTIVE'
          AND v.isArchived = 0
        ORDER BY s.dueAt ASC, s.dueOdometerMeters ASC
        """
    )
    suspend fun getActiveSchedulesWithVehicleFor(vehicleId: Long): List<ScheduleWithVehicle>

    /** Single schedule plus its vehicle, or null when either is missing/archived. */
    @Query(
        """
        SELECT s.*, v.id AS joinedVehicleId, v.name AS vehicleName,
               v.currentOdometerMeters AS vehicleCurrentOdometerMeters
        FROM maintenance_schedules s
        INNER JOIN vehicles v ON v.id = s.vehicleId
        WHERE s.id = :scheduleId
          AND s.isArchived = 0
          AND s.status = 'ACTIVE'
          AND v.isArchived = 0
        LIMIT 1
        """
    )
    suspend fun getScheduleWithVehicle(scheduleId: Long): ScheduleWithVehicle?

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
