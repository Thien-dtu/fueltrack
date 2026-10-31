package com.example.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.datastore.UserPreferencesDataStore
import com.example.core.model.FuelEntry
import com.example.core.model.MonthlyFuelStats
import com.example.core.model.Vehicle
import com.example.core.repository.FuelRepository
import com.example.core.repository.VehicleRepository
import com.example.core.util.StatisticsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatisticsUiState> = combine(
        vehicleRepository.observeActiveVehicles(),
        userPreferences.selectedVehicleIdFlow
    ) { vehicles, savedVehicleId ->
        val current = vehicles.find { it.id == savedVehicleId } ?: vehicles.firstOrNull()
        vehicles to current
    }.flatMapLatest { (vehicles, current) ->
        if (current == null) {
            flowOf(StatisticsUiState(isLoading = false))
        } else {
            // Only this vehicle's rows are read. Observing "all entries" and filtering in
            // the view model made every emission from any vehicle recompute these stats.
            fuelRepository.observeEntriesForVehicle(current.id)
                .map { entries -> buildState(vehicles, current, entries) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState()
    )

    private fun buildState(
        vehicles: List<Vehicle>,
        current: Vehicle,
        entries: List<FuelEntry>
    ): StatisticsUiState {
        val monthly = StatisticsCalculator.computeMonthlyStats(entries)
        return StatisticsUiState(
            vehicles = vehicles,
            selectedVehicle = current,
            monthlyStats = monthly,
            allTimeDistanceMeters = monthly.sumOf { it.totalDistanceMeters },
            allTimeFuelLiters = monthly.sumOf { it.totalFuelLiters },
            allTimeCostVnd = monthly.sumOf { it.totalCostVnd },
            isLoading = false
        )
    }

    fun selectVehicle(vehicleId: Long) {
        viewModelScope.launch {
            userPreferences.setSelectedVehicleId(vehicleId)
        }
    }
}
