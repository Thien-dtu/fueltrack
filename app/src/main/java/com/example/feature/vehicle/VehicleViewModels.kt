package com.example.feature.vehicle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.Vehicle
import com.example.core.model.VehicleType
import com.example.core.repository.VehicleRepository
import com.example.core.util.OdometerParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehicleListUiState(
    val activeVehicles: List<Vehicle> = emptyList(),
    val archivedVehicles: List<Vehicle> = emptyList(),
    val showArchived: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class VehicleListViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _showArchived = MutableStateFlow(false)
    val showArchived = _showArchived.asStateFlow()

    val uiState: StateFlow<VehicleListUiState> = vehicleRepository.observeAllVehicles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        ).let { vehiclesFlow ->
            combine(vehiclesFlow, _showArchived) { list, showArchivedFlag ->
                val active = list.filter { !it.isArchived }
                val archived = list.filter { it.isArchived }
                VehicleListUiState(
                    activeVehicles = active,
                    archivedVehicles = archived,
                    showArchived = showArchivedFlag,
                    isLoading = false
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = VehicleListUiState()
            )
        }

    fun toggleShowArchived() {
        _showArchived.update { !it }
    }

    fun archiveVehicle(vehicleId: Long) {
        viewModelScope.launch {
            vehicleRepository.archive(vehicleId)
        }
    }

    fun unarchiveVehicle(vehicleId: Long) {
        viewModelScope.launch {
            vehicleRepository.unarchive(vehicleId)
        }
    }
}

data class AddEditVehicleFormState(
    val name: String = "",
    val type: VehicleType = VehicleType.MOTORCYCLE,
    val brand: String = "",
    val model: String = "",
    val yearString: String = "",
    val licensePlate: String = "",
    val odoInput: String = "0",
    val fuelTankCapacityString: String = "",
    val nameError: String? = null,
    val odoError: String? = null,
    val tankCapacityError: String? = null,
    val isSaved: Boolean = false,
    val isSubmitting: Boolean = false
)

@HiltViewModel
class AddEditVehicleViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val vehicleId: Long? = savedStateHandle.get<Long>("vehicleId")
        ?: savedStateHandle.get<String>("vehicleId")?.toLongOrNull()

    // Secondary constructor for testing
    constructor(
        vehicleRepository: VehicleRepository,
        vehicleId: Long?
    ) : this(
        vehicleRepository,
        SavedStateHandle(if (vehicleId != null) mapOf("vehicleId" to vehicleId) else emptyMap())
    )

    private val _formState = MutableStateFlow(AddEditVehicleFormState())
    val formState: StateFlow<AddEditVehicleFormState> = _formState.asStateFlow()

    init {
        if (vehicleId != null && vehicleId > 0) {
            viewModelScope.launch {
                val v = vehicleRepository.getVehicle(vehicleId)
                if (v != null) {
                    _formState.update {
                        it.copy(
                            name = v.name,
                            type = v.type,
                            brand = v.brand,
                            model = v.model,
                            yearString = v.year?.toString() ?: "",
                            licensePlate = v.licensePlate,
                            odoInput = OdometerParser.formatForEdit(v.currentOdometerMeters, useComma = true),
                            fuelTankCapacityString = v.fuelTankCapacityLiters?.toString() ?: ""
                        )
                    }
                }
            }
        }
    }

    fun onNameChanged(newName: String) {
        _formState.update { it.copy(name = newName, nameError = null) }
    }

    fun onTypeChanged(newType: VehicleType) {
        _formState.update { it.copy(type = newType) }
    }

    fun onBrandChanged(newBrand: String) {
        _formState.update { it.copy(brand = newBrand) }
    }

    fun onModelChanged(newModel: String) {
        _formState.update { it.copy(model = newModel) }
    }

    fun onYearChanged(newYear: String) {
        _formState.update { it.copy(yearString = newYear.filter { char -> char.isDigit() }) }
    }

    fun onLicensePlateChanged(newPlate: String) {
        _formState.update { it.copy(licensePlate = newPlate.uppercase()) }
    }

    fun onOdoChanged(newOdo: String) {
        _formState.update { it.copy(odoInput = newOdo, odoError = null) }
    }

    fun onTankCapacityChanged(newCap: String) {
        _formState.update { it.copy(fuelTankCapacityString = newCap, tankCapacityError = null) }
    }

    fun saveVehicle() {
        val state = _formState.value
        if (state.isSubmitting) return

        if (state.name.isBlank()) {
            _formState.update { it.copy(nameError = "Vui lòng nhập tên phương tiện") }
            return
        }

        val odoResult = OdometerParser.parse(state.odoInput)
        if (odoResult is OdometerParser.ParseResult.Error) {
            _formState.update { it.copy(odoError = odoResult.message) }
            return
        }
        val odoMeters = (odoResult as OdometerParser.ParseResult.Success).meters

        val yearInt = state.yearString.toIntOrNull()
        val tankCapacity = state.fuelTankCapacityString.replace(',', '.').toDoubleOrNull()
        if (state.yearString.isNotBlank() && (yearInt == null || yearInt !in 1886..2100)) {
            _formState.update { it.copy(nameError = "Năm sản xuất không hợp lệ") }
            return
        }
        if (state.fuelTankCapacityString.isNotBlank() && (tankCapacity == null || tankCapacity <= 0.0)) {
            _formState.update { it.copy(tankCapacityError = "Dung tích bình phải lớn hơn 0") }
            return
        }

        _formState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            if (vehicleId != null && vehicleId > 0) {
                val existing = vehicleRepository.getVehicle(vehicleId)
                if (existing != null) {
                    val updated = existing.copy(
                        name = state.name.trim(),
                        type = state.type,
                        brand = state.brand.trim(),
                        model = state.model.trim(),
                        year = yearInt,
                        licensePlate = state.licensePlate.trim(),
                        currentOdometerMeters = odoMeters,
                        fuelTankCapacityLiters = tankCapacity,
                        updatedAt = System.currentTimeMillis()
                    )
                    vehicleRepository.update(updated)
                }
            } else {
                val newVehicle = Vehicle(
                    name = state.name.trim(),
                    type = state.type,
                    brand = state.brand.trim(),
                    model = state.model.trim(),
                    year = yearInt,
                    licensePlate = state.licensePlate.trim(),
                    initialOdometerMeters = odoMeters,
                    currentOdometerMeters = odoMeters,
                    fuelTankCapacityLiters = tankCapacity,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                vehicleRepository.insert(newVehicle)
            }
            _formState.update { it.copy(isSubmitting = false, isSaved = true) }
        }
    }
}
