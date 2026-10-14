package com.example.feature.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.datastore.UserPreferencesDataStore
import com.example.core.model.MaintenanceDueStatus
import com.example.core.model.MaintenanceEntry
import com.example.core.model.MaintenancePolicy
import com.example.core.model.MaintenanceSchedule
import com.example.core.model.MaintenanceScheduleWithStatus
import com.example.core.model.MaintenanceStats
import com.example.core.model.Vehicle
import com.example.core.repository.MaintenanceRepository
import com.example.core.repository.VehicleRepository
import com.example.core.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaintenanceDashboardUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val dueSchedules: List<MaintenanceScheduleWithStatus> = emptyList(),
    val upcomingSchedules: List<MaintenanceScheduleWithStatus> = emptyList(),
    val recentEntries: List<MaintenanceEntry> = emptyList(),
    val stats: MaintenanceStats = MaintenanceStats(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MaintenanceDashboardViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    private val focusedScheduleId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<MaintenanceDashboardUiState> = combine(
        vehicleRepository.observeActiveVehicles(),
        userPreferences.selectedVehicleIdFlow,
        maintenanceRepository.observeAllActiveSchedules(),
        maintenanceRepository.observeAllEntries(),
        focusedScheduleId
    ) { vehicles, selectedVehicleId, allSchedules, allEntries, focusedId ->
        if (vehicles.isEmpty()) {
            return@combine MaintenanceDashboardUiState(isLoading = false)
        }

        val currentVehicle = vehicles.find { it.id == selectedVehicleId } ?: vehicles.first()
        val now = System.currentTimeMillis()

        // Filter schedules for current vehicle
        val vehicleSchedules = allSchedules.filter { it.vehicleId == currentVehicle.id }
        val vehicleEntries = allEntries.filter { it.vehicleId == currentVehicle.id }

        // Map schedules with computed due status
        val schedulesWithStatus = vehicleSchedules.map { schedule ->
            val status = MaintenancePolicy.calculateDueStatus(
                lifecycleStatus = schedule.status,
                dueAt = schedule.dueAt,
                dueOdometerMeters = schedule.dueOdometerMeters,
                currentOdometerMeters = currentVehicle.currentOdometerMeters,
                currentTimeMillis = now
            )
            val reason = buildTriggerReason(schedule, currentVehicle, now, status)
            MaintenanceScheduleWithStatus(
                schedule = schedule,
                vehicleName = currentVehicle.name,
                currentOdometerMeters = currentVehicle.currentOdometerMeters,
                dueStatus = status,
                triggerReason = reason
            )
        }

        val dueOrOverdue = schedulesWithStatus.filter {
            it.dueStatus == MaintenanceDueStatus.DUE || it.dueStatus == MaintenanceDueStatus.OVERDUE
        }.sortedWith(compareBy({ it.schedule.id != focusedId }, { it.dueStatus != MaintenanceDueStatus.OVERDUE }, { it.schedule.dueAt ?: Long.MAX_VALUE }))

        val upcoming = schedulesWithStatus.filter {
            it.dueStatus == MaintenanceDueStatus.UPCOMING
        }.sortedWith(compareBy({ it.schedule.id != focusedId }, { it.schedule.dueAt ?: Long.MAX_VALUE }))

        val stats = maintenanceRepository.computeMaintenanceStats(vehicleEntries)

        MaintenanceDashboardUiState(
            vehicles = vehicles,
            selectedVehicle = currentVehicle,
            dueSchedules = dueOrOverdue,
            upcomingSchedules = upcoming,
            recentEntries = vehicleEntries.take(5),
            stats = stats,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MaintenanceDashboardUiState(isLoading = true)
    )

    fun selectVehicle(vehicleId: Long) {
        viewModelScope.launch {
            userPreferences.setSelectedVehicleId(vehicleId)
        }
    }

    fun focusSchedule(scheduleId: Long) {
        focusedScheduleId.value = scheduleId
    }

    private fun buildTriggerReason(
        schedule: MaintenanceSchedule,
        vehicle: Vehicle,
        now: Long,
        status: MaintenanceDueStatus
    ): String {
        return buildString {
            if (schedule.dueAt != null) {
                val diffDays = (schedule.dueAt - now) / 86_400_000L
                if (diffDays < 0) {
                    append("Quá hạn ${-diffDays} ngày (${Formatters.formatDateOnly(schedule.dueAt)})")
                } else {
                    append("Đến hạn sau $diffDays ngày (${Formatters.formatDateOnly(schedule.dueAt)})")
                }
            }
            if (schedule.dueOdometerMeters != null) {
                if (isNotEmpty()) append(" • ")
                val diffMeters = schedule.dueOdometerMeters - vehicle.currentOdometerMeters
                if (diffMeters <= 0) {
                    append("Vượt ODO ${Formatters.formatDistanceKm(-diffMeters)}")
                } else {
                    append("Còn ${Formatters.formatDistanceKm(diffMeters)} (mốc ${Formatters.formatDistanceKm(schedule.dueOdometerMeters)})")
                }
            }
        }
    }
}
