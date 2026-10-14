package com.example.core.di

import com.example.core.repository.FuelRepository
import com.example.core.repository.FuelRepositoryImpl
import com.example.core.repository.FuelTypeRepository
import com.example.core.repository.FuelTypeRepositoryImpl
import com.example.core.repository.MaintenanceRepository
import com.example.core.repository.MaintenanceRepositoryImpl
import com.example.core.repository.VehicleRepository
import com.example.core.repository.VehicleRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVehicleRepository(
        impl: VehicleRepositoryImpl
    ): VehicleRepository

    @Binds
    @Singleton
    abstract fun bindFuelTypeRepository(
        impl: FuelTypeRepositoryImpl
    ): FuelTypeRepository

    @Binds
    @Singleton
    abstract fun bindFuelRepository(
        impl: FuelRepositoryImpl
    ): FuelRepository

    @Binds
    @Singleton
    abstract fun bindMaintenanceRepository(
        impl: MaintenanceRepositoryImpl
    ): MaintenanceRepository
}
