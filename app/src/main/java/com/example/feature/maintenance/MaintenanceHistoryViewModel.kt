package com.example.feature.maintenance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.MaintenanceEntry
import com.example.core.repository.MaintenanceRepository
import com.example.core.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaintenanceHistoryUiState(
    val entries: List<MaintenanceEntry> = emptyList(),
    val vehicleName: String = "",
    val totalCostVnd: Long = 0L,
    val isLoading: Boolean = true
)

@HiltViewModel
class MaintenanceHistoryViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
    private val vehicleRepository: VehicleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val vehicleId: Long = savedStateHandle.get<Long>("vehicleId")
        ?: savedStateHandle.get<String>("vehicleId")?.toLongOrNull()
        ?: 0L

    constructor(
        maintenanceRepository: MaintenanceRepository,
        vehicleRepository: VehicleRepository,
        vehicleId: Long
    ) : this(
        maintenanceRepository,
        vehicleRepository,
        SavedStateHandle(mapOf("vehicleId" to vehicleId))
    )

    val uiState: StateFlow<MaintenanceHistoryUiState> = combine(
        maintenanceRepository.observeEntriesForVehicle(vehicleId),
        vehicleRepository.observeVehicle(vehicleId)
    ) { entries, vehicle ->
        val totalCost = entries.mapNotNull { it.cost }.fold(0L) { acc, c ->
            try {
                Math.addExact(acc, c)
            } catch (_: ArithmeticException) {
                Long.MAX_VALUE
            }
        }
        MaintenanceHistoryUiState(
            entries = entries,
            vehicleName = vehicle?.name ?: "Phương tiện",
            totalCostVnd = totalCost,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MaintenanceHistoryUiState()
    )

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            maintenanceRepository.deleteEntry(id)
        }
    }
}
