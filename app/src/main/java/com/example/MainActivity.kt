package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.core.ui.MainAppScaffold
import com.example.core.ui.theme.FuelTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val navigateTo = intent?.getStringExtra("navigate_to")
        val vehicleId = intent?.getLongExtra("vehicle_id", -1L)?.takeIf { it > 0L }
        val scheduleId = intent?.getLongExtra("schedule_id", -1L)?.takeIf { it > 0L }
        setContent {
            FuelTrackTheme {
                MainAppScaffold(
                    initialRoute = navigateTo,
                    initialVehicleId = vehicleId,
                    initialScheduleId = scheduleId
                )
            }
        }
    }
}
