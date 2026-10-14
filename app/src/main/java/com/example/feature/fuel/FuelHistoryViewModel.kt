package com.example.feature.fuel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.FuelEntryWithDetails
import com.example.core.repository.FuelRepository
import com.example.core.repository.FuelTypeRepository
import com.example.core.repository.VehicleRepository
import com.example.core.util.FuelCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FuelHistoryUiState(
    val entries: List<FuelEntryWithDetails> = emptyList(),
    val vehicleName: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class FuelHistoryViewModel @Inject constructor(
    private val fuelRepository: FuelRepository,
    private val vehicleRepository: VehicleRepository,
    private val fuelTypeRepository: FuelTypeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val vehicleId: Long = savedStateHandle.get<Long>("vehicleId")
        ?: savedStateHandle.get<String>("vehicleId")?.toLongOrNull()
        ?: 0L

    constructor(
        fuelRepository: FuelRepository,
        vehicleRepository: VehicleRepository,
        fuelTypeRepository: FuelTypeRepository,
        vehicleId: Long
    ) : this(
        fuelRepository,
        vehicleRepository,
        fuelTypeRepository,
        SavedStateHandle(mapOf("vehicleId" to vehicleId))
    )

    val uiState: StateFlow<FuelHistoryUiState> = combine(
        fuelRepository.observeEntriesForVehicle(vehicleId),
        fuelTypeRepository.observeAllFuelTypes(),
        vehicleRepository.observeVehicle(vehicleId)
    ) { entries, fuelTypes, vehicle ->
        val fuelTypeMap = fuelTypes.associateBy { it.id }
        val vehicleName = vehicle?.name ?: "Phương tiện"

        val detailedEntries = entries.mapIndexed { index, entry ->
            val prevEntry = if (index + 1 < entries.size) entries[index + 1] else null
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
                fuelTypeName = fuelTypeMap[entry.fuelTypeId]?.name ?: "Không xác định",
                vehicleName = vehicleName,
                distanceSincePreviousMeters = distMeters,
                consumptionLPer100Km = consumption,
                costPerKmVnd = costPerKm
            )
        }

        FuelHistoryUiState(
            entries = detailedEntries,
            vehicleName = vehicleName,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FuelHistoryUiState()
    )

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            fuelRepository.delete(entryId)
            val latest = fuelRepository.getLatestForVehicle(vehicleId)
            val vehicle = vehicleRepository.getVehicle(vehicleId)
            if (vehicle != null) {
                val newOdo = latest?.odometerMeters ?: vehicle.initialOdometerMeters
                vehicleRepository.updateCurrentOdometer(vehicleId, newOdo)
            }
        }
    }
}
