package com.example.feature.maintenance

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.components.EmptyVehicleState
import com.example.core.ui.components.MaintenanceEntryCard
import com.example.core.ui.components.MaintenanceScheduleCard
import com.example.core.ui.components.VehicleSwitcher
import com.example.core.ui.theme.AmberPrimary
import com.example.core.ui.theme.CyanAccent
import com.example.core.ui.theme.DarkSurface
import com.example.core.ui.theme.DarkSurfaceVariant
import com.example.core.ui.theme.ErrorRed
import com.example.core.ui.theme.SuccessGreen
import com.example.core.util.Formatters

@Composable
fun MaintenanceDashboardScreen(
    viewModel: MaintenanceDashboardViewModel,
    onNavigateToAddSchedule: (Long) -> Unit,
    onNavigateToAddEntry: (Long) -> Unit,
    onNavigateToCompleteSchedule: (Long) -> Unit,
    onNavigateToEditSchedule: (Long) -> Unit,
    onNavigateToEditEntry: (Long) -> Unit,
    onNavigateToHistory: (Long) -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToAddVehicle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

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
            Spacer(modifier = Modifier.height(28.dp))
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

                TextButton(
                    onClick = onNavigateToTemplates,
                    modifier = Modifier.testTag("maintenance_templates_top_button")
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mẫu bảo dưỡng", color = CyanAccent)
                }
            }
        }

        // Quick Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onNavigateToAddSchedule(vehicle.id) },
                    modifier = Modifier.weight(1f).testTag("add_schedule_action_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Lịch mới", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onNavigateToAddEntry(vehicle.id) },
                    modifier = Modifier.weight(1f).testTag("add_entry_action_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ghi nhận đã làm", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Monthly Maintenance Cost Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "CHI PHÍ BẢO DƯỠNG THÁNG NÀY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = Formatters.formatCurrencyVnd(uiState.stats.monthlyCostVnd),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = AmberPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Tổng tất cả thời gian: ${Formatters.formatCurrencyVnd(uiState.stats.totalCostVnd)} (${uiState.stats.totalEntriesCount} lần)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Due / Overdue Section
        if (uiState.dueSchedules.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cần bảo dưỡng ngay (${uiState.dueSchedules.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                }
            }

            items(uiState.dueSchedules) { scheduleWithStatus ->
                MaintenanceScheduleCard(
                    scheduleWithStatus = scheduleWithStatus,
                    onComplete = { onNavigateToCompleteSchedule(scheduleWithStatus.schedule.id) },
                    onClick = { onNavigateToEditSchedule(scheduleWithStatus.schedule.id) }
                )
            }
        }

        // Upcoming Schedules Section
        item {
            Text(
                text = "Lịch sắp tới (${uiState.upcomingSchedules.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (uiState.upcomingSchedules.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Không có lịch bảo dưỡng sắp tới. Nhấn '+ Lịch mới' để thêm.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.upcomingSchedules) { scheduleWithStatus ->
                MaintenanceScheduleCard(
                    scheduleWithStatus = scheduleWithStatus,
                    onComplete = { onNavigateToCompleteSchedule(scheduleWithStatus.schedule.id) },
                    onClick = { onNavigateToEditSchedule(scheduleWithStatus.schedule.id) }
                )
            }
        }

        // Recent Maintenance History Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lịch sử bảo dưỡng gần đây",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = { onNavigateToHistory(vehicle.id) },
                    modifier = Modifier.testTag("view_all_maintenance_history_button")
                ) {
                    Text("Xem tất cả", color = AmberPrimary)
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
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Chưa có lịch sử bảo dưỡng nào được ghi nhận.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.recentEntries) { entry ->
                MaintenanceEntryCard(
                    entry = entry,
                    onClick = { onNavigateToEditEntry(entry.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
