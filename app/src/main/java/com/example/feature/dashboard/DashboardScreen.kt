package com.example.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.components.EmptyVehicleState
import com.example.core.ui.components.FuelEntryCard
import com.example.core.ui.components.VehicleSwitcher
import com.example.core.ui.theme.AmberPrimary
import com.example.core.ui.theme.CyanAccent
import com.example.core.ui.theme.DarkSurface
import com.example.core.ui.theme.DarkSurfaceHighlight
import com.example.core.ui.theme.DarkSurfaceVariant
import com.example.core.ui.theme.SuccessGreen
import com.example.core.util.Formatters

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddFuel: (Long) -> Unit,
    onNavigateToAddVehicle: () -> Unit,
    onNavigateToHistory: (Long) -> Unit,
    onNavigateToEditFuel: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AmberPrimary)
        }
        return
    }

    if (uiState.vehicles.isEmpty()) {
        EmptyVehicleState(onAddVehicle = onNavigateToAddVehicle, modifier = modifier)
        return
    }

    val vehicle = uiState.selectedVehicle ?: return

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            // Header with Vehicle Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VehicleSwitcher(
                    vehicles = uiState.vehicles,
                    selectedVehicle = vehicle,
                    onVehicleSelected = { viewModel.selectVehicle(it) }
                )

                Button(
                    onClick = { onNavigateToAddFuel(vehicle.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_fuel_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Đổ xăng", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Hero Metric Card: Current Odometer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ODO HIỆN TẠI",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = Formatters.formatDistanceKm(vehicle.currentOdometerMeters).removeSuffix(" km"),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "km",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberPrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    if (vehicle.licensePlate.isNotBlank()) {
                        Text(
                            text = "Biển số: ${vehicle.licensePlate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Key Monthly Summary Metrics (2x2 Grid)
        item {
            Text(
                text = "Tháng này (${uiState.currentMonthStats?.displayName ?: "Hiện tại"})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Primary Card: Consumption Rate (Featured)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceHighlight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Mức tiêu hao bình quân",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatConsumption(uiState.currentMonthStats?.averageConsumptionLPer100Km ?: uiState.latestConsumptionLPer100Km),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.currentMonthStats?.averageConsumptionLPer100Km != null || uiState.latestConsumptionLPer100Km != null) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Chi phí / km",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val costPerKm = uiState.currentMonthStats?.averageCostPerKmVnd ?: uiState.latestCostPerKmVnd
                        Text(
                            text = if (costPerKm != null) "${Formatters.formatCurrencyVnd(costPerKm.toLong())}/km" else "Chưa đủ dữ liệu",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AmberPrimary
                        )
                    }
                }
            }
        }

        // Monthly Stats (Distance, Fuel, Cost)
        item {
            val stats = uiState.currentMonthStats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Distance
                DashboardStatSmallCard(
                    title = "Quãng đường",
                    value = Formatters.formatDistanceKm(stats?.totalDistanceMeters ?: 0L),
                    accentColor = CyanAccent,
                    modifier = Modifier.weight(1f)
                )

                // Liters
                DashboardStatSmallCard(
                    title = "Nhiên liệu",
                    value = Formatters.formatLiters(stats?.totalFuelLiters ?: 0.0),
                    accentColor = AmberPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Cost
                DashboardStatSmallCard(
                    title = "Chi phí",
                    value = Formatters.formatCurrencyVnd(stats?.totalCostVnd ?: 0L),
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Entries Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lịch sử gần đây",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = { onNavigateToHistory(vehicle.id) },
                    modifier = Modifier.testTag("view_all_history_button")
                ) {
                    Text(text = "Xem tất cả", color = AmberPrimary)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (uiState.recentEntries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Chưa có dữ liệu đổ xăng.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hãy ghi nhận lần đổ xăng đầu tiên để theo dõi tiêu hao.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onNavigateToAddFuel(vehicle.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ghi nhận đổ xăng ngay")
                        }
                    }
                }
            }
        } else {
            items(uiState.recentEntries) { entryWithDetails ->
                FuelEntryCard(
                    entryWithDetails = entryWithDetails,
                    onClick = { onNavigateToEditFuel(entryWithDetails.entry.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // padding for bottom bar
        }
    }
}

@Composable
fun DashboardStatSmallCard(
    title: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}

