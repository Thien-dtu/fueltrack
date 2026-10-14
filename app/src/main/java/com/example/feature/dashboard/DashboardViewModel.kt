package com.example.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.datastore.UserPreferencesDataStore
import com.example.core.model.FuelEntryWithDetails
import com.example.core.model.MonthlyFuelStats
import com.example.core.model.Vehicle
import com.example.core.repository.FuelRepository
import com.example.core.repository.FuelTypeRepository
import com.example.core.repository.VehicleRepository
import com.example.core.util.FuelCalculator
import com.example.core.util.StatisticsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class DashboardUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val recentEntries: List<FuelEntryWithDetails> = emptyList(),
    val currentMonthStats: MonthlyFuelStats? = null,
    val latestConsumptionLPer100Km: Double? = null,
    val latestCostPerKmVnd: Double? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val fuelRepository: FuelRepository,
    private val fuelTypeRepository: FuelTypeRepository,
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    private val _selectedVehicleId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        vehicleRepository.observeActiveVehicles(),
        userPreferences.selectedVehicleIdFlow,
        fuelTypeRepository.observeAllFuelTypes(),
        fuelRepository.observeAllEntries()
    ) { activeVehicles, savedVehicleId, allFuelTypes, allEntries ->
        if (activeVehicles.isEmpty()) {
            return@combine DashboardUiState(isLoading = false)
        }

        // Determine currently active vehicle
        val currentVehicle = activeVehicles.find { it.id == savedVehicleId }
            ?: activeVehicles.first()

        // Persist active vehicle selection if not set or changed
        if (savedVehicleId != currentVehicle.id) {
            viewModelScope.launch {
                userPreferences.setSelectedVehicleId(currentVehicle.id)
            }
        }

        // Filter and sort entries for current vehicle
        val vehicleEntries = allEntries
            .filter { it.vehicleId == currentVehicle.id }
            .sortedWith(compareByDescending<com.example.core.model.FuelEntry> { it.odometerMeters }.thenByDescending { it.timestamp })

        val fuelTypeMap = allFuelTypes.associateBy { it.id }

        // Build recent entries with detail diffs
        val recentWithDetails = vehicleEntries.take(5).mapIndexed { index, entry ->
            val prevEntry = if (index + 1 < vehicleEntries.size) vehicleEntries[index + 1] else null
            val distMeters = if (prevEntry != null) {
                FuelCalculator.calculateDistance(prevEntry.odometerMeters, entry.odometerMeters)
            } else null

            val consumption = if (entry.isFullTank && prevEntry != null && prevEntry.isFullTank && distMeters != null) {
                FuelCalculator.calculateConsumptionLPer100Km(entry.liters, distMeters)
            } else null

            val costPerKm = if (distMeters != null) {
                FuelCalculator.calculateCostPerKm(entry.amountPaid, distMeters)
            } else null

            FuelEntryWithDetails(
                entry = entry,
                fuelTypeName = fuelTypeMap[entry.fuelTypeId]?.name ?: "Nhiên liệu",
                vehicleName = currentVehicle.name,
                distanceSincePreviousMeters = distMeters,
                consumptionLPer100Km = consumption,
                costPerKmVnd = costPerKm
            )
        }

        // Compute monthly statistics
        val monthlyStats = StatisticsCalculator.computeMonthlyStats(vehicleEntries)
        val currentMonthStats = monthlyStats.firstOrNull()

        DashboardUiState(
            vehicles = activeVehicles,
            selectedVehicle = currentVehicle,
            recentEntries = recentWithDetails,
            currentMonthStats = currentMonthStats,
            latestConsumptionLPer100Km = recentWithDetails.firstOrNull { it.consumptionLPer100Km != null }?.consumptionLPer100Km,
            latestCostPerKmVnd = recentWithDetails.firstOrNull { it.costPerKmVnd != null }?.costPerKmVnd,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun selectVehicle(vehicleId: Long) {
        viewModelScope.launch {
            userPreferences.setSelectedVehicleId(vehicleId)
        }
    }
}
