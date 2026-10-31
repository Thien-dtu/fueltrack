package com.example.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.datastore.UserPreferencesDataStore
import com.example.core.model.FuelEntry
import com.example.core.model.FuelEntryWithDetails
import com.example.core.model.MonthlyFuelStats
import com.example.core.model.Vehicle
import com.example.core.repository.FuelRepository
import com.example.core.repository.FuelTypeRepository
import com.example.core.repository.VehicleRepository
import com.example.core.util.FuelCycleMetricsCalculator
import com.example.core.util.FuelDistanceCalculator
import com.example.core.util.StatisticsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class DashboardUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val recentEntries: List<FuelEntryWithDetails> = emptyList(),
    val currentMonthStats: MonthlyFuelStats? = null,
    /**
     * True when [currentMonthStats] actually describes the running calendar month.
     * On the first days of a month the app falls back to the most recent month that
     * has data, and the UI must label that month instead of calling it "this month".
     */
    val statsAreForCurrentMonth: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val fuelRepository: FuelRepository,
    private val fuelTypeRepository: FuelTypeRepository,
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        vehicleRepository.observeActiveVehicles(),
        userPreferences.selectedVehicleIdFlow
    ) { vehicles, savedVehicleId ->
        val current = vehicles.find { it.id == savedVehicleId } ?: vehicles.firstOrNull()
        vehicles to current
    }.flatMapLatest { (vehicles, current) ->
        if (current == null) {
            flowOf(DashboardUiState(isLoading = false))
        } else {
            // Only this vehicle's rows are read: subscribing to every vehicle's entries
            // made the dashboard recompute its stats whenever an unrelated vehicle changed.
            combine(
                fuelRepository.observeEntriesForVehicle(current.id),
                fuelTypeRepository.observeAllFuelTypes()
            ) { entries, fuelTypes ->
                buildState(vehicles, current, entries, fuelTypes)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    private fun buildState(
        vehicles: List<Vehicle>,
        currentVehicle: Vehicle,
        vehicleEntries: List<FuelEntry>,
        fuelTypes: List<com.example.core.model.FuelType>
    ): DashboardUiState {
        val sortedDesc = vehicleEntries.sortedWith(
            compareByDescending<FuelEntry> { it.odometerMeters }.thenByDescending { it.timestamp }
        )

        val fuelTypeMap = fuelTypes.associateBy { it.id }
        val cycleMetrics = FuelCycleMetricsCalculator.calculate(vehicleEntries)
        // The list is ordered newest-first for display, so "distance since previous
        // fill-up" must be resolved by timestamp, not by the neighbouring list item.
        val distanceByEntryId = FuelDistanceCalculator.distanceByEntryId(vehicleEntries)

        val recentWithDetails = sortedDesc.take(5).map { entry ->
            FuelEntryWithDetails(
                entry = entry,
                fuelTypeName = fuelTypeMap[entry.fuelTypeId]?.name ?: "Nhiên liệu",
                vehicleName = currentVehicle.name,
                distanceSincePreviousMeters = distanceByEntryId[entry.id],
                consumptionLPer100Km = cycleMetrics.consumptionByEntryId[entry.id],
                costPerKmVnd = cycleMetrics.costPerKmByEntryId[entry.id]
            )
        }

        val monthlyStats = StatisticsCalculator.computeMonthlyStats(vehicleEntries)
        val now = Calendar.getInstance()
        val currentMonthKey = String.format(
            Locale.US,
            "%04d-%02d",
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1
        )
        val currentMonthStats = monthlyStats.firstOrNull { it.yearMonthKey == currentMonthKey }
        // Early in a new month there are no entries yet, so the running month has no
        // stats. Showing empty placeholders hides the last known consumption figures;
        // fall back to the most recent month that does have data and let the UI label it.
        val fallbackStats = currentMonthStats ?: monthlyStats.firstOrNull()

        return DashboardUiState(
            vehicles = vehicles,
            selectedVehicle = currentVehicle,
            recentEntries = recentWithDetails,
            currentMonthStats = fallbackStats,
            statsAreForCurrentMonth = currentMonthStats != null,
            isLoading = false
        )
    }

    fun selectVehicle(vehicleId: Long) {
        viewModelScope.launch {
            userPreferences.setSelectedVehicleId(vehicleId)
        }
    }
}
