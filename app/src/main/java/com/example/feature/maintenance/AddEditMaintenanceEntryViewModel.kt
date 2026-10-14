package com.example.feature.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.model.MaintenanceEntry
import com.example.core.model.MaintenanceTemplate
import com.example.core.model.Vehicle
import com.example.core.repository.MaintenanceRepository
import com.example.core.repository.VehicleRepository
import com.example.core.util.OdometerParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class AddEditMaintenanceEntryState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: Long = 0L,
    val templates: List<MaintenanceTemplate> = emptyList(),
    val selectedTemplateId: Long? = null,
    val title: String = "",
    val category: String = "Bảo dưỡng",
    val description: String = "",
    val performedAt: Long = System.currentTimeMillis(),
    val odoInput: String = "",
    val costInput: String = "",
    val providerName: String = "",
    val note: String = "",
    val titleError: String? = null,
    val odoError: String? = null,
    val costError: String? = null,
    val isSubmitting: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class AddEditMaintenanceEntryViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
    private val vehicleRepository: VehicleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val targetVehicleId: Long? = savedStateHandle.get<Long>("vehicleId")
        ?: savedStateHandle.get<String>("vehicleId")?.toLongOrNull()

    val editEntryId: Long? = savedStateHandle.get<Long>("entryId")
        ?: savedStateHandle.get<String>("entryId")?.toLongOrNull()

    val prefilledScheduleId: Long? = savedStateHandle.get<Long>("scheduleId")
        ?: savedStateHandle.get<String>("scheduleId")?.toLongOrNull()

    constructor(
        maintenanceRepository: MaintenanceRepository,
        vehicleRepository: VehicleRepository,
        targetVehicleId: Long?,
        editEntryId: Long?,
        prefilledScheduleId: Long? = null
    ) : this(
        maintenanceRepository,
        vehicleRepository,
        SavedStateHandle(
            buildMap {
                if (targetVehicleId != null) put("vehicleId", targetVehicleId)
                if (editEntryId != null) put("entryId", editEntryId)
                if (prefilledScheduleId != null) put("scheduleId", prefilledScheduleId)
            }
        )
    )

    private val _formState = MutableStateFlow(AddEditMaintenanceEntryState())
    val formState: StateFlow<AddEditMaintenanceEntryState> = _formState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                vehicleRepository.observeActiveVehicles(),
                maintenanceRepository.observeActiveTemplates()
            ) { vehicles, templates ->
                Pair(vehicles, templates)
            }.collect { (vList, tList) ->
                val current = _formState.value
                val selectedVId = if (current.selectedVehicleId > 0 && vList.any { it.id == current.selectedVehicleId }) {
                    current.selectedVehicleId
                } else if (targetVehicleId != null && vList.any { it.id == targetVehicleId }) {
                    targetVehicleId
                } else {
                    vList.firstOrNull()?.id ?: 0L
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
                        templates = tList,
                        odoInput = defaultOdo
                    )
                }
            }
        }

        // If editing existing entry
        if (editEntryId != null && editEntryId > 0) {
            viewModelScope.launch {
                val entry = maintenanceRepository.getEntryById(editEntryId)
                if (entry != null) {
                    _formState.update {
                        it.copy(
                            selectedVehicleId = entry.vehicleId,
                            selectedTemplateId = entry.templateId,
                            title = entry.title,
                            category = entry.category,
                            description = entry.description,
                            performedAt = entry.performedAt,
                            odoInput = entry.odometerMeters?.let { meters -> OdometerParser.formatForEdit(meters, useComma = true) } ?: "",
                            costInput = entry.cost?.toString() ?: "",
                            providerName = entry.providerName,
                            note = entry.note
                        )
                    }
                }
            }
        } else if (prefilledScheduleId != null && prefilledScheduleId > 0) {
            // Completing a schedule: pre-fill schedule info
            viewModelScope.launch {
                val schedule = maintenanceRepository.getScheduleById(prefilledScheduleId)
                if (schedule != null) {
                    _formState.update {
                        it.copy(
                            selectedVehicleId = schedule.vehicleId,
                            selectedTemplateId = schedule.templateId,
                            title = schedule.title,
                            category = schedule.category
                        )
                    }
                }
            }
        }
    }

    fun onVehicleSelected(vehicleId: Long) {
        val v = _formState.value.vehicles.find { it.id == vehicleId }
        _formState.update {
            it.copy(
                selectedVehicleId = vehicleId,
                odoInput = if (v != null) OdometerParser.formatForEdit(v.currentOdometerMeters, useComma = true) else it.odoInput
            )
        }
    }

    fun onTemplateSelected(template: MaintenanceTemplate) {
        _formState.update {
            it.copy(
                selectedTemplateId = template.id,
                title = template.name,
                category = template.category,
                description = template.description
            )
        }
    }

    fun onTitleChanged(newTitle: String) {
        _formState.update { it.copy(title = newTitle, titleError = null) }
    }

    fun onCategoryChanged(newCategory: String) {
        _formState.update { it.copy(category = newCategory) }
    }

    fun onDescriptionChanged(newDesc: String) {
        _formState.update { it.copy(description = newDesc) }
    }

    fun onOdoChanged(newOdo: String) {
        _formState.update { it.copy(odoInput = newOdo, odoError = null) }
    }

    fun onCostChanged(newCost: String) {
        val clean = newCost.filter { it.isDigit() }
        _formState.update { it.copy(costInput = clean, costError = null) }
    }

    fun onProviderChanged(newProvider: String) {
        _formState.update { it.copy(providerName = newProvider) }
    }

    fun onNoteChanged(newNote: String) {
        _formState.update { it.copy(note = newNote) }
    }

    fun saveEntry() {
        val state = _formState.value
        if (state.isSubmitting) return

        if (state.title.isBlank()) {
            _formState.update { it.copy(titleError = "Vui lòng nhập tên hạng mục bảo dưỡng") }
            return
        }

        val odoMeters: Long? = if (state.odoInput.isNotBlank()) {
            val parseRes = OdometerParser.parse(state.odoInput)
            if (parseRes is OdometerParser.ParseResult.Error) {
                _formState.update { it.copy(odoError = parseRes.message) }
                return
            }
            (parseRes as OdometerParser.ParseResult.Success).meters
        } else {
            null
        }

        val costVnd: Long? = if (state.costInput.isNotBlank()) {
            val cost = state.costInput.toLongOrNull()
            if (cost == null || cost < 0) {
                _formState.update { it.copy(costError = "Chi phí không hợp lệ") }
                return
            }
            cost
        } else {
            null
        }

        _formState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entry = MaintenanceEntry(
                id = editEntryId ?: 0L,
                vehicleId = state.selectedVehicleId,
                templateId = state.selectedTemplateId,
                title = state.title.trim(),
                category = state.category.trim(),
                description = state.description.trim(),
                performedAt = state.performedAt,
                odometerMeters = odoMeters,
                cost = costVnd,
                providerName = state.providerName.trim(),
                note = state.note.trim(),
                createdAt = now,
                updatedAt = now
            )

            if (prefilledScheduleId != null && prefilledScheduleId > 0) {
                // Atomic transaction: Complete schedule and record entry
                maintenanceRepository.completeScheduleWithEntry(prefilledScheduleId, entry)
            } else if (editEntryId != null && editEntryId > 0) {
                maintenanceRepository.updateEntry(entry)
            } else {
                maintenanceRepository.insertEntry(entry)
            }

            // Update vehicle current odometer if higher
            if (odoMeters != null) {
                val vehicle = vehicleRepository.getVehicle(state.selectedVehicleId)
                if (vehicle != null && odoMeters > vehicle.currentOdometerMeters) {
                    vehicleRepository.updateCurrentOdometer(state.selectedVehicleId, odoMeters)
                }
            }

            _formState.update { it.copy(isSubmitting = false, isSaved = true) }
        }
    }

    fun deleteEntry() {
        if (editEntryId == null || editEntryId <= 0) return
        _formState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            maintenanceRepository.deleteEntry(editEntryId)
            _formState.update { it.copy(isSubmitting = false, isSaved = true) }
        }
    }
}
