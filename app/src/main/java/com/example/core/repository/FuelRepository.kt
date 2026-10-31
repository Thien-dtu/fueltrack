package com.example.core.repository

import com.example.core.model.FuelEntry
import kotlinx.coroutines.flow.Flow

/** Outcome of saving a fuel entry. */
sealed class SaveFuelEntryResult {
    data class Saved(val entryId: Long) : SaveFuelEntryResult()
    /** Another entry of the same vehicle already uses this odometer reading. */
    data object DuplicateOdometer : SaveFuelEntryResult()
    /** Editing an id that no longer exists. */
    data object EntryNotFound : SaveFuelEntryResult()
}

interface FuelRepository {
    fun observeEntriesForVehicle(vehicleId: Long): Flow<List<FuelEntry>>
    fun observeAllEntries(): Flow<List<FuelEntry>>
    suspend fun getById(id: Long): FuelEntry?
    suspend fun hasOtherEntryAtOdometer(vehicleId: Long, odoMeters: Long, excludedEntryId: Long = 0L): Boolean
    /**
     * Saves an entry in one transaction: rejects a duplicate odometer reading, writes
     * the row and keeps the vehicle odometer in step.
     *
     * [entry.id] selects insert (0) or update. Pass the entry's own id as
     * [excludedEntryId] when updating so it does not collide with itself.
     */
    suspend fun saveWithOdometerSync(entry: FuelEntry): SaveFuelEntryResult
    suspend fun insertAndSyncOdometer(entry: FuelEntry): Long
    suspend fun updateAndSyncOdometer(entry: FuelEntry, previousEntry: FuelEntry)
    suspend fun deleteAndSyncOdometer(entry: FuelEntry)
    suspend fun insert(entry: FuelEntry): Long
    suspend fun update(entry: FuelEntry)
    suspend fun delete(id: Long)
}
