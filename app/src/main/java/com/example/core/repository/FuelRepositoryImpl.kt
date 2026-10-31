package com.example.core.repository

import com.example.core.database.dao.FuelEntryDao
import com.example.core.database.dao.VehicleDao
import com.example.core.database.AppDatabase
import com.example.core.database.entity.FuelEntryEntity
import com.example.core.model.FuelEntry
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuelRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val fuelEntryDao: FuelEntryDao,
    private val vehicleDao: VehicleDao,
    private val workScheduler: com.example.core.work.MaintenanceWorkScheduler? = null
) : FuelRepository {

    override fun observeEntriesForVehicle(vehicleId: Long): Flow<List<FuelEntry>> {
        return fuelEntryDao.observeEntriesForVehicle(vehicleId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeAllEntries(): Flow<List<FuelEntry>> {
        return fuelEntryDao.observeAllEntries().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: Long): FuelEntry? {
        return fuelEntryDao.getEntryById(id)?.toDomain()
    }

    override suspend fun getLatestForVehicle(vehicleId: Long): FuelEntry? {
        return fuelEntryDao.getLatestEntryForVehicle(vehicleId)?.toDomain()
    }

    override suspend fun getPreviousEntryByOdo(vehicleId: Long, odoMeters: Long): FuelEntry? {
        return fuelEntryDao.getPreviousEntryByOdo(vehicleId, odoMeters)?.toDomain()
    }

    override suspend fun hasOtherEntryAtOdometer(
        vehicleId: Long,
        odoMeters: Long,
        excludedEntryId: Long
    ): Boolean {
        return fuelEntryDao.hasOtherEntryAtOdometer(vehicleId, odoMeters, excludedEntryId)
    }

    override suspend fun insertAndSyncOdometer(entry: FuelEntry): Long {
        val id = database.withTransaction {
            val insertedId = fuelEntryDao.insertEntry(FuelEntryEntity.fromDomain(entry))
            syncVehicleOdometerAfterChange(entry.vehicleId, entry.odometerMeters)
            insertedId
        }
        workScheduler?.evaluateOdometerReminders(entry.vehicleId, entry.odometerMeters)
        return id
    }

    override suspend fun updateAndSyncOdometer(entry: FuelEntry, previousEntry: FuelEntry) {
        database.withTransaction {
            fuelEntryDao.updateEntry(FuelEntryEntity.fromDomain(entry))
            syncVehicleOdometerAfterChange(
                vehicleId = previousEntry.vehicleId,
                affectedOdometerMeters = previousEntry.odometerMeters
            )
            if (entry.vehicleId != previousEntry.vehicleId) {
                syncVehicleOdometerAfterChange(
                    vehicleId = entry.vehicleId,
                    affectedOdometerMeters = entry.odometerMeters
                )
            } else {
                syncVehicleOdometerAfterChange(
                    vehicleId = entry.vehicleId,
                    affectedOdometerMeters = maxOf(previousEntry.odometerMeters, entry.odometerMeters)
                )
            }
        }
        workScheduler?.evaluateOdometerReminders(entry.vehicleId, entry.odometerMeters)
    }

    override suspend fun deleteAndSyncOdometer(entry: FuelEntry) {
        database.withTransaction {
            fuelEntryDao.deleteEntryById(entry.id)
            syncVehicleOdometerAfterChange(entry.vehicleId, entry.odometerMeters)
        }
    }

    private suspend fun syncVehicleOdometerAfterChange(
        vehicleId: Long,
        affectedOdometerMeters: Long
    ) {
        val vehicle = vehicleDao.getVehicleById(vehicleId) ?: return
        // Preserve a value that is higher than the affected history entry: it may
        // have been entered manually or supplied by a maintenance record.
        if (vehicle.currentOdometerMeters > affectedOdometerMeters) return

        val historyMax = fuelEntryDao.getMaxOdometerForVehicle(vehicleId)
            ?: vehicle.initialOdometerMeters
        vehicleDao.updateCurrentOdometer(
            vehicleId,
            maxOf(vehicle.initialOdometerMeters, historyMax)
        )
    }

    override suspend fun getEntriesInRange(vehicleId: Long, startTime: Long, endTime: Long): List<FuelEntry> {
        return fuelEntryDao.getEntriesInRange(vehicleId, startTime, endTime).map { it.toDomain() }
    }

    override suspend fun getAllEntriesAscForVehicle(vehicleId: Long): List<FuelEntry> {
        return fuelEntryDao.getAllEntriesAscForVehicle(vehicleId).map { it.toDomain() }
    }

    override suspend fun insert(entry: FuelEntry): Long {
        val id = fuelEntryDao.insertEntry(FuelEntryEntity.fromDomain(entry))
        workScheduler?.evaluateOdometerReminders(entry.vehicleId, entry.odometerMeters)
        return id
    }

    override suspend fun update(entry: FuelEntry) {
        val previousEntry = fuelEntryDao.getEntryById(entry.id)?.toDomain()
        if (previousEntry != null) {
            updateAndSyncOdometer(entry, previousEntry)
        } else {
            fuelEntryDao.updateEntry(FuelEntryEntity.fromDomain(entry))
            workScheduler?.evaluateOdometerReminders(entry.vehicleId, entry.odometerMeters)
        }
    }

    override suspend fun delete(id: Long) {
        val entry = fuelEntryDao.getEntryById(id)?.toDomain()
        if (entry != null) {
            deleteAndSyncOdometer(entry)
        } else {
            fuelEntryDao.deleteEntryById(id)
        }
    }
}
