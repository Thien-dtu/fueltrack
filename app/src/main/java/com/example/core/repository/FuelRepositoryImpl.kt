package com.example.core.repository

import com.example.core.database.dao.FuelEntryDao
import com.example.core.database.entity.FuelEntryEntity
import com.example.core.model.FuelEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuelRepositoryImpl @Inject constructor(
    private val fuelEntryDao: FuelEntryDao,
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
        fuelEntryDao.updateEntry(FuelEntryEntity.fromDomain(entry))
        workScheduler?.evaluateOdometerReminders(entry.vehicleId, entry.odometerMeters)
    }

    override suspend fun delete(id: Long) {
        fuelEntryDao.deleteEntryById(id)
    }
}
