package com.example.core.repository

import com.example.core.database.dao.VehicleDao
import com.example.core.database.entity.VehicleEntity
import com.example.core.model.Vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepositoryImpl @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val workScheduler: com.example.core.work.MaintenanceWorkScheduler? = null
) : VehicleRepository {

    override fun observeActiveVehicles(): Flow<List<Vehicle>> {
        return vehicleDao.observeActiveVehicles().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeAllVehicles(): Flow<List<Vehicle>> {
        return vehicleDao.observeAllVehicles().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeVehicle(id: Long): Flow<Vehicle?> {
        return vehicleDao.observeVehicleById(id).map { it?.toDomain() }
    }

    override suspend fun getVehicle(id: Long): Vehicle? {
        return vehicleDao.getVehicleById(id)?.toDomain()
    }

    override suspend fun insert(vehicle: Vehicle): Long {
        return vehicleDao.insertVehicle(VehicleEntity.fromDomain(vehicle))
    }

    override suspend fun update(vehicle: Vehicle) {
        vehicleDao.updateVehicle(VehicleEntity.fromDomain(vehicle))
        workScheduler?.evaluateOdometerReminders(vehicle.id, vehicle.currentOdometerMeters)
    }

    override suspend fun archive(id: Long) {
        vehicleDao.archiveVehicle(id)
    }

    override suspend fun unarchive(id: Long) {
        vehicleDao.unarchiveVehicle(id)
    }

    override suspend fun updateCurrentOdometer(vehicleId: Long, odometerMeters: Long) {
        vehicleDao.updateCurrentOdometer(vehicleId, odometerMeters)
        workScheduler?.evaluateOdometerReminders(vehicleId, odometerMeters)
    }

    override suspend fun getActiveCount(): Int {
        return vehicleDao.getActiveVehicleCount()
    }
}
