package com.example.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.model.FuelEntry
import com.example.core.model.FuelEntryWithDetails
import com.example.core.model.MaintenanceDueStatus
import com.example.core.model.MaintenanceSchedule
import com.example.core.model.MaintenanceScheduleWithStatus
import com.example.core.ui.theme.DarkBackground
import com.example.core.ui.theme.FuelTrackTheme

/**
 * Compose previews for the reusable card components.
 *
 * These exist so the layout can be inspected in Android Studio without running
 * the app or standing up Hilt, Room and DataStore. They are compiled into the
 * app but never referenced at runtime.
 */

private fun sampleFuelEntry(
    id: Long,
    odoMeters: Long,
    liters: Double,
    amount: Long,
    full: Boolean
) = FuelEntryWithDetails(
    entry = FuelEntry(
        id = id,
        vehicleId = 1L,
        timestamp = 1_760_000_000_000L,
        odometerMeters = odoMeters,
        fuelTypeId = 1L,
        pricePerLiter = (amount / liters).toLong(),
        amountPaid = amount,
        liters = liters,
        isFullTank = full
    ),
    fuelTypeName = "RON 95-III",
    vehicleName = "Wave Alpha"
)

@Preview(name = "Fuel entry - full tank", showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun FuelEntryCardFullTankPreview() {
    FuelTrackTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FuelEntryCard(
                entryWithDetails = sampleFuelEntry(1, 12_500_000L, 3.8, 95_000L, true)
                    .copy(
                        distanceSincePreviousMeters = 142_000L,
                        consumptionLPer100Km = 2.68,
                        costPerKmVnd = 669.0
                    ),
                onClick = {}
            )
            FuelEntryCard(
                entryWithDetails = sampleFuelEntry(2, 12_358_000L, 1.5, 38_000L, false),
                onClick = {}
            )
        }
    }
}

@Preview(name = "Maintenance schedule - due", showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun MaintenanceScheduleCardPreview() {
    FuelTrackTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(16.dp)
        ) {
            MaintenanceScheduleCard(
                scheduleWithStatus = MaintenanceScheduleWithStatus(
                    schedule = MaintenanceSchedule(
                        id = 1L,
                        vehicleId = 1L,
                        title = "Thay dầu máy",
                        category = "Bảo dưỡng",
                        dueOdometerMeters = 12_000_000L,
                        intervalMeters = 2_000_000L
                    ),
                    vehicleName = "Wave Alpha",
                    currentOdometerMeters = 12_100_000L,
                    dueStatus = MaintenanceDueStatus.DUE,
                    triggerReason = "Đã vượt 100 km"
                ),
                onComplete = {},
                onClick = {}
            )
        }
    }
}
