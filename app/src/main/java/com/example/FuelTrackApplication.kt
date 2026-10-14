package com.example

import android.app.Application
import com.example.core.notification.MaintenanceNotificationHelper
import com.example.core.work.MaintenanceWorkScheduler
import dagger.hilt.android.HiltAndroidApp
import android.util.Log
import javax.inject.Inject

@HiltAndroidApp
class FuelTrackApplication : Application() {

    @Inject
    lateinit var maintenanceWorkScheduler: MaintenanceWorkScheduler

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
