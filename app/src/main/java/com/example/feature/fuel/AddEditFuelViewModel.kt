package com.example.feature.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.model.FuelEntry
import com.example.core.model.FuelEntryWithDetails
import com.example.core.model.FuelType
import com.example.core.model.Vehicle
import com.example.core.repository.FuelRepository
import com.example.core.repository.FuelTypeRepository
import com.example.core.repository.VehicleRepository
import com.example.core.util.FuelCalculator
import com.example.core.util.OdometerParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class AddEditFuelFormState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: Long = 0L,
    val fuelTypes: List<FuelType> = emptyList(),
    val selectedFuelTypeId: Long = 0L,
    val odoInput: String = "",
    val pricePerLiterInput: String = "25000",
    val amountPaidInput: String = "50000",
    val calculatedLiters: Double = 2.0,
    val isFullTank: Boolean = true,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val latestVehicleOdoMeters: Long = 0L,
    val odoWarning: String? = null,
    val odoError: String? = null,
    val amountError: String? = null,
    val priceError: String? = null,
    val isSubmitting: Boolean = false,
    val isSaved: Boolean = false,
    val showQuickCreateFuelTypeDialog: Boolean = false
)

@HiltViewModel
class AddEditFuelViewModel @Inject constructor(
    private val fuelRepository: FuelRepository,
    private val vehicleRepository: VehicleRepository,
    private val fuelTypeRepository: FuelTypeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val targetVehicleId: Long? = savedStateHandle.get<Long>("vehicleId")
        ?: savedStateHandle.get<String>("vehicleId")?.toLongOrNull()

    val editEntryId: Long? = savedStateHandle.get<Long>("entryId")
        ?: savedStateHandle.get<String>("entryId")?.toLongOrNull()

    constructor(
        fuelRepository: FuelRepository,
        vehicleRepository: VehicleRepository,
        fuelTypeRepository: FuelTypeRepository,
        targetVehicleId: Long?,
        editEntryId: Long?
    ) : this(
        fuelRepository,
        vehicleRepository,
        fuelTypeRepository,
        SavedStateHandle(
            buildMap {
                if (targetVehicleId != null) put("vehicleId", targetVehicleId)
                if (editEntryId != null) put("entryId", editEntryId)
            }
        )
    )

    private val _formState = MutableStateFlow(AddEditFuelFormState())
    val formState: StateFlow<AddEditFuelFormState> = _formState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val vehicles = vehicleRepository.observeActiveVehicles()
            val fuelTypes = fuelTypeRepository.observeActiveFuelTypes()

            combine(vehicles, fuelTypes) { vList, fList ->
                Pair(vList, fList)
            }.collect { (vList, fList) ->
                val current = _formState.value
                val selectedVId = if (current.selectedVehicleId > 0 && vList.any { it.id == current.selectedVehicleId }) {
                    current.selectedVehicleId
                } else if (targetVehicleId != null && vList.any { it.id == targetVehicleId }) {
                    targetVehicleId
                } else {
                    vList.firstOrNull()?.id ?: 0L
                }

                val selectedFId = if (current.selectedFuelTypeId > 0 && fList.any { it.id == current.selectedFuelTypeId }) {
                    current.selectedFuelTypeId
                } else {
                    fList.firstOrNull()?.id ?: 0L
                }

                val currentVehicle = vList.find { it.id == selectedVId }
                val defaultOdo = if (current.odoInput.isBlank() && currentVehicle != null) {
                    OdometerParser.formatForEdit(currentVehicle.currentOdometerMeters, useComma = true)
                } else {
                    current.odoInput
                }

                _formState.update {
                    it.copy(
                        vehicles = vList,
                        selectedVehicleId = selectedVId,
                        fuelTypes = fList,
                        selectedFuelTypeId = selectedFId,
                        odoInput = defaultOdo,
                        latestVehicleOdoMeters = currentVehicle?.currentOdometerMeters ?: 0L
                    )
                }
                recalculateLiters()
            }
        }

        // If editing existing entry, populate fields
        if (editEntryId != null && editEntryId > 0) {
            viewModelScope.launch {
                val entry = fuelRepository.getById(editEntryId)
                if (entry != null) {
                    _formState.update {
                        it.copy(
                            selectedVehicleId = entry.vehicleId,
                            selectedFuelTypeId = entry.fuelTypeId,
                            odoInput = OdometerParser.formatForEdit(entry.odometerMeters, useComma = true),
                            pricePerLiterInput = entry.pricePerLiter.toString(),
                            amountPaidInput = entry.amountPaid.toString(),
                            calculatedLiters = entry.liters,
                            isFullTank = entry.isFullTank,
                            note = entry.note,
                            timestamp = entry.timestamp
                        )
                    }
                    recalculateLiters()
                }
            }
        }
    }

    fun onVehicleSelected(vehicleId: Long) {
        val selected = _formState.value.vehicles.find { it.id == vehicleId }
        _formState.update {
            it.copy(
                selectedVehicleId = vehicleId,
                latestVehicleOdoMeters = selected?.currentOdometerMeters ?: 0L,
                odoInput = if (selected != null) OdometerParser.formatForEdit(selected.currentOdometerMeters, useComma = true) else it.odoInput
            )
        }
        validateOdoWarning()
    }

    fun onFuelTypeSelected(fuelTypeId: Long) {
        _formState.update { it.copy(selectedFuelTypeId = fuelTypeId) }
    }

    fun onOdoChanged(input: String) {
        _formState.update { it.copy(odoInput = input, odoError = null) }
        validateOdoWarning()
    }

    private fun validateOdoWarning() {
        val state = _formState.value
        val parseResult = OdometerParser.parse(state.odoInput)
        if (parseResult is OdometerParser.ParseResult.Success) {
            if (parseResult.meters < state.latestVehicleOdoMeters && (editEntryId == null || editEntryId <= 0)) {
                _formState.update {
                    it.copy(
                        odoWarning = "Chỉ số ODO (${OdometerParser.formatKm(parseResult.meters)} km) thấp hơn ODO hiện tại của xe (${OdometerParser.formatKm(state.latestVehicleOdoMeters)} km). Có phải ODO đã reset?"
                    )
                }
            } else {
                _formState.update { it.copy(odoWarning = null) }
            }
        } else {
            _formState.update { it.copy(odoWarning = null) }
        }
    }

    fun onPriceChanged(input: String) {
        val clean = input.filter { it.isDigit() }
        _formState.update { it.copy(pricePerLiterInput = clean, priceError = null) }
        recalculateLiters()
    }

    fun onAmountPaidChanged(input: String) {
        val clean = input.filter { it.isDigit() }
        _formState.update { it.copy(amountPaidInput = clean, amountError = null) }
        recalculateLiters()
    }

    private fun recalculateLiters() {
        val state = _formState.value
        val amount = state.amountPaidInput.toLongOrNull() ?: 0L
        val price = state.pricePerLiterInput.toLongOrNull() ?: 0L
        val liters = FuelCalculator.calculateLiters(amount, price)
        _formState.update { it.copy(calculatedLiters = liters) }
    }

    fun onFullTankChanged(isFull: Boolean) {
        _formState.update { it.copy(isFullTank = isFull) }
    }

    fun onNoteChanged(newNote: String) {
        _formState.update { it.copy(note = newNote) }
    }

    fun setShowQuickCreateFuelType(show: Boolean) {
        _formState.update { it.copy(showQuickCreateFuelTypeDialog = show) }
    }

    fun quickCreateFuelType(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newId = fuelTypeRepository.insert(
                FuelType(name = name.trim(), description = description.trim(), createdAt = now, updatedAt = now)
            )
            // Auto-select newly created fuel type
            _formState.update {
                it.copy(
                    selectedFuelTypeId = newId,
                    showQuickCreateFuelTypeDialog = false
                )
            }
        }
    }

    fun saveFuelEntry() {
        val state = _formState.value
        if (state.isSubmitting) return

        if (state.selectedVehicleId <= 0L) {
            _formState.update { it.copy(odoError = "Vui lòng chọn phương tiện") }
            return
        }
        if (state.selectedFuelTypeId <= 0L) {
            _formState.update { it.copy(odoError = "Vui lòng chọn loại nhiên liệu") }
            return
        }

        val odoResult = OdometerParser.parse(state.odoInput)
        if (odoResult is OdometerParser.ParseResult.Error) {
            _formState.update { it.copy(odoError = odoResult.message) }
            return
        }
        val odoMeters = (odoResult as OdometerParser.ParseResult.Success).meters
        val selectedVehicle = state.vehicles.find { it.id == state.selectedVehicleId }
        if (selectedVehicle != null &&
            (editEntryId == null || editEntryId <= 0L) &&
            odoMeters < selectedVehicle.currentOdometerMeters
        ) {
            _formState.update {
                it.copy(
                    odoError = "ODO không được nhỏ hơn số hiện tại của xe (${OdometerParser.formatKm(selectedVehicle.currentOdometerMeters)} km)",
                    odoWarning = null
                )
            }
            return
        }

        val amount = state.amountPaidInput.toLongOrNull() ?: 0L
        if (amount <= 0L) {
            _formState.update { it.copy(amountError = "Số tiền phải lớn hơn 0") }
            return
        }

        val price = state.pricePerLiterInput.toLongOrNull() ?: 0L
        if (price <= 0L) {
            _formState.update { it.copy(priceError = "Giá mỗi lít phải lớn hơn 0") }
            return
        }

        val liters = FuelCalculator.calculateLiters(amount, price)
        if (liters <= 0.0) {
            _formState.update { it.copy(amountError = "Số lít không hợp lệ") }
            return
        }
        if (state.isFullTank && selectedVehicle?.fuelTankCapacityLiters != null &&
            liters > selectedVehicle.fuelTankCapacityLiters
        ) {
            _formState.update {
                it.copy(amountError = "Số lít vượt quá dung tích bình xăng của xe")
            }
            return
        }

        _formState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (editEntryId != null && editEntryId > 0) {
                val existing = fuelRepository.getById(editEntryId)
                if (existing != null) {
                    val updated = existing.copy(
                        vehicleId = state.selectedVehicleId,
                        fuelTypeId = state.selectedFuelTypeId,
                        odometerMeters = odoMeters,
                        amountPaid = amount,
                        pricePerLiter = price,
                        liters = liters,
                        isFullTank = state.isFullTank,
                        note = state.note.trim(),
                        updatedAt = now
                    )
                    fuelRepository.update(updated)
                }
            } else {
                val newEntry = FuelEntry(
                    vehicleId = state.selectedVehicleId,
                    timestamp = now,
                    odometerMeters = odoMeters,
                    fuelTypeId = state.selectedFuelTypeId,
                    pricePerLiter = price,
                    amountPaid = amount,
                    liters = liters,
                    isFullTank = state.isFullTank,
                    note = state.note.trim(),
                    createdAt = now,
                    updatedAt = now
                )
                fuelRepository.insert(newEntry)
            }

            // Also update vehicle's current odometer if new ODO is higher
            val vehicle = vehicleRepository.getVehicle(state.selectedVehicleId)
            if (vehicle != null && odoMeters > vehicle.currentOdometerMeters) {
                vehicleRepository.updateCurrentOdometer(state.selectedVehicleId, odoMeters)
            }

            _formState.update { it.copy(isSubmitting = false, isSaved = true) }
        }
    }

    fun deleteFuelEntry() {
        if (editEntryId == null || editEntryId <= 0) return
        _formState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            fuelRepository.delete(editEntryId)
            // Recalculate latest vehicle odometer
            val vehicleId = _formState.value.selectedVehicleId
            val latest = fuelRepository.getLatestForVehicle(vehicleId)
            val vehicle = vehicleRepository.getVehicle(vehicleId)
            if (vehicle != null) {
                val newOdo = latest?.odometerMeters ?: vehicle.initialOdometerMeters
                vehicleRepository.updateCurrentOdometer(vehicleId, newOdo)
            }
            _formState.update { it.copy(isSubmitting = false, isSaved = true) }
        }
    }
}
