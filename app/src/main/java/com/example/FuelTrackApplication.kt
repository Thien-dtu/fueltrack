package com.example

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.core.notification.MaintenanceNotificationHelper
import com.example.core.work.MaintenanceWorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FuelTrackApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var maintenanceWorkScheduler: MaintenanceWorkScheduler

    // Workers are created by Hilt so they can take DAOs and repositories as
    // constructor parameters instead of reaching for AppDatabase.getInstance().
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        MaintenanceNotificationHelper.createNotificationChannel(this)
        try {
            if (::maintenanceWorkScheduler.isInitialized) {
                maintenanceWorkScheduler.scheduleDailyMaintenanceAudit()
            }
        } catch (error: IllegalStateException) {
            Log.e("FuelTrackApplication", "Unable to schedule maintenance audit", error)
        }
    }
}
