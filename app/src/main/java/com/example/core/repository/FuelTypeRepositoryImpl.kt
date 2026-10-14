package com.example.core.repository

import com.example.core.database.dao.FuelTypeDao
import com.example.core.database.entity.FuelTypeEntity
import com.example.core.model.FuelType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuelTypeRepositoryImpl @Inject constructor(
    private val fuelTypeDao: FuelTypeDao
) : FuelTypeRepository {

    override fun observeActiveFuelTypes(): Flow<List<FuelType>> {
        return fuelTypeDao.observeActiveFuelTypes().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeAllFuelTypes(): Flow<List<FuelType>> {
        return fuelTypeDao.observeAllFuelTypes().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: Long): FuelType? {
        return fuelTypeDao.getFuelTypeById(id)?.toDomain()
    }

    override suspend fun getByName(name: String): FuelType? {
        return fuelTypeDao.getFuelTypeByName(name.trim())?.toDomain()
    }

    override suspend fun insert(fuelType: FuelType): Long {
        return fuelTypeDao.insertFuelType(FuelTypeEntity.fromDomain(fuelType))
    }

    override suspend fun update(fuelType: FuelType) {
        fuelTypeDao.updateFuelType(FuelTypeEntity.fromDomain(fuelType))
    }

    override suspend fun archive(id: Long) {
        fuelTypeDao.archiveFuelType(id)
    }

    override suspend fun unarchive(id: Long) {
        fuelTypeDao.unarchiveFuelType(id)
    }

    override suspend fun getCount(): Int {
        return fuelTypeDao.getFuelTypeCount()
    }
}
