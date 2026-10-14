package com.example.core.di

import android.content.Context
import com.example.core.database.AppDatabase
import com.example.core.database.dao.FuelEntryDao
import com.example.core.database.dao.FuelTypeDao
import com.example.core.database.dao.MaintenanceEntryDao
import com.example.core.database.dao.MaintenanceScheduleDao
import com.example.core.database.dao.MaintenanceTemplateDao
import com.example.core.database.dao.VehicleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideVehicleDao(db: AppDatabase): VehicleDao = db.vehicleDao()

    @Provides
    fun provideFuelTypeDao(db: AppDatabase): FuelTypeDao = db.fuelTypeDao()

    @Provides
    fun provideFuelEntryDao(db: AppDatabase): FuelEntryDao = db.fuelEntryDao()

    @Provides
    fun provideMaintenanceTemplateDao(db: AppDatabase): MaintenanceTemplateDao = db.maintenanceTemplateDao()

    @Provides
    fun provideMaintenanceEntryDao(db: AppDatabase): MaintenanceEntryDao = db.maintenanceEntryDao()

    @Provides
    fun provideMaintenanceScheduleDao(db: AppDatabase): MaintenanceScheduleDao = db.maintenanceScheduleDao()
}
