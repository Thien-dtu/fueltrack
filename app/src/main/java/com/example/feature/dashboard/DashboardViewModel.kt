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
import com.example.core.util.FuelConsumptionCalculator
import com.example.core.util.FuelDistanceCalculator
import com.example.core.util.FuelCostCalculator
import com.example.core.util.StatisticsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

import dagger.hilt.android.lifecycle.HiltViewModel
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
        val consumptionByEntryId = FuelConsumptionCalculator.consumptionByEndingEntryId(vehicleEntries)
        val costByEntryId = FuelCostCalculator.costByEndingEntryId(vehicleEntries)

        // Build recent entries with detail diffs. The list is ordered newest-first for
        // display, so "distance since previous fill-up" must be resolved by timestamp,
        // not by the neighbouring list item (odometer order breaks after a reset).
        val distanceByEntryId = FuelDistanceCalculator.distanceByEntryId(vehicleEntries)
        val recentWithDetails = vehicleEntries.take(5).map { entry ->
            val distMeters = distanceByEntryId[entry.id]

            val consumption = consumptionByEntryId[entry.id]

            val costPerKm = costByEntryId[entry.id]

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

        DashboardUiState(
            vehicles = activeVehicles,
            selectedVehicle = currentVehicle,
            recentEntries = recentWithDetails,
            currentMonthStats = fallbackStats,
            statsAreForCurrentMonth = currentMonthStats != null,
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
