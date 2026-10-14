package com.example.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.datastore.UserPreferencesDataStore
import com.example.core.model.MonthlyFuelStats
import com.example.core.model.Vehicle
import com.example.core.repository.FuelRepository
import com.example.core.repository.VehicleRepository
import com.example.core.util.StatisticsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class StatisticsUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val monthlyStats: List<MonthlyFuelStats> = emptyList(),
    val allTimeDistanceMeters: Long = 0L,
    val allTimeFuelLiters: Double = 0.0,
    val allTimeCostVnd: Long = 0L,
    val isLoading: Boolean = true
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val fuelRepository: FuelRepository,
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = combine(
        vehicleRepository.observeActiveVehicles(),
        userPreferences.selectedVehicleIdFlow,
        fuelRepository.observeAllEntries()
    ) { vehicles, selectedVehicleId, allEntries ->
        if (vehicles.isEmpty()) {
            return@combine StatisticsUiState(isLoading = false)
        }

        val currentVehicle = vehicles.find { it.id == selectedVehicleId } ?: vehicles.first()
        val vehicleEntries = allEntries.filter { it.vehicleId == currentVehicle.id }

        val monthly = StatisticsCalculator.computeMonthlyStats(vehicleEntries)

        val totalDistMeters = monthly.sumOf { it.totalDistanceMeters }
        val totalFuel = monthly.sumOf { it.totalFuelLiters }
        val totalCost = monthly.sumOf { it.totalCostVnd }

        StatisticsUiState(
            vehicles = vehicles,
            selectedVehicle = currentVehicle,
            monthlyStats = monthly,
            allTimeDistanceMeters = totalDistMeters,
            allTimeFuelLiters = totalFuel,
            allTimeCostVnd = totalCost,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState()
    )

    fun selectVehicle(vehicleId: Long) {
        viewModelScope.launch {
            userPreferences.setSelectedVehicleId(vehicleId)
        }
    }
}
