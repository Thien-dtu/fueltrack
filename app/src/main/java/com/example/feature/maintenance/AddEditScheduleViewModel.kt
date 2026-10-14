package com.example.feature.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.model.MaintenanceSchedule
import com.example.core.model.MaintenanceTemplate
import com.example.core.model.ScheduleLifecycleStatus
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
import java.util.Calendar

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class AddEditScheduleFormState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: Long = 0L,
    val templates: List<MaintenanceTemplate> = emptyList(),
    val selectedTemplateId: Long? = null,
    val title: String = "",
    val category: String = "Bảo dưỡng",
    // Due conditions
    val hasDateCondition: Boolean = true,
    val dueDaysFromNow: Int = 30,
    val hasOdoCondition: Boolean = true,
    val dueOdoInput: String = "",
    // Recurring interval
    val isRecurring: Boolean = false,
    val intervalDaysInput: String = "90",
    val intervalKmInput: String = "2000",
    val titleError: String? = null,
    val conditionError: String? = null,
    val isSubmitting: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class AddEditScheduleViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository,
    private val vehicleRepository: VehicleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val targetVehicleId: Long? = savedStateHandle.get<Long>("vehicleId")
        ?: savedStateHandle.get<String>("vehicleId")?.toLongOrNull()

    val editScheduleId: Long? = savedStateHandle.get<Long>("scheduleId")
        ?: savedStateHandle.get<String>("scheduleId")?.toLongOrNull()

    constructor(
        maintenanceRepository: MaintenanceRepository,
        vehicleRepository: VehicleRepository,
        targetVehicleId: Long?,
        editScheduleId: Long?
    ) : this(
        maintenanceRepository,
        vehicleRepository,
        SavedStateHandle(
            buildMap {
                if (targetVehicleId != null) put("vehicleId", targetVehicleId)
                if (editScheduleId != null) put("scheduleId", editScheduleId)
            }
        )
    )

    private val _formState = MutableStateFlow(AddEditScheduleFormState())
    val formState: StateFlow<AddEditScheduleFormState> = _formState.asStateFlow()

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
                val defaultTargetOdo = if (current.dueOdoInput.isBlank() && currentVehicle != null) {
                    // Default to current + 2,000 km
                    val nextMeters = currentVehicle.currentOdometerMeters + 2_000_000L
                    OdometerParser.formatForEdit(nextMeters, useComma = true)
                } else {
                    current.dueOdoInput
                }

                _formState.update {
                    it.copy(
                        vehicles = vList,
                        selectedVehicleId = selectedVId,
                        templates = tList,
                        dueOdoInput = defaultTargetOdo
                    )
                }
            }
        }

        if (editScheduleId != null && editScheduleId > 0) {
            viewModelScope.launch {
                val s = maintenanceRepository.getScheduleById(editScheduleId)
                if (s != null) {
                    val now = System.currentTimeMillis()
                    val days = s.dueAt?.let { ((it - now) / 86_400_000L).toInt().coerceAtLeast(1) } ?: 30
                    _formState.update {
                        it.copy(
                            selectedVehicleId = s.vehicleId,
                            selectedTemplateId = s.templateId,
                            title = s.title,
                            category = s.category,
                            hasDateCondition = s.dueAt != null,
                            dueDaysFromNow = days,
                            hasOdoCondition = s.dueOdometerMeters != null,
                            dueOdoInput = s.dueOdometerMeters?.let { meters -> OdometerParser.formatForEdit(meters, useComma = true) } ?: "",
                            isRecurring = s.intervalDays != null || s.intervalMeters != null,
                            intervalDaysInput = s.intervalDays?.toString() ?: "",
                            intervalKmInput = s.intervalMeters?.let { m -> (m / 1000).toString() } ?: ""
                        )
                    }
                }
            }
        }
    }

    fun onVehicleSelected(id: Long) {
        val v = _formState.value.vehicles.find { it.id == id }
        _formState.update {
            it.copy(
                selectedVehicleId = id,
                dueOdoInput = if (v != null) OdometerParser.formatForEdit(v.currentOdometerMeters + 2_000_000L, useComma = true) else it.dueOdoInput
            )
        }
    }

    fun onTemplateSelected(template: MaintenanceTemplate) {
        val currentVehicle = _formState.value.vehicles.find { it.id == _formState.value.selectedVehicleId }
        val targetOdoMeters = if (template.defaultIntervalMeters != null && currentVehicle != null) {
            currentVehicle.currentOdometerMeters + template.defaultIntervalMeters
        } else null

        _formState.update {
            it.copy(
                selectedTemplateId = template.id,
                title = template.name,
                category = template.category,
                hasDateCondition = template.defaultIntervalDays != null,
                dueDaysFromNow = template.defaultIntervalDays ?: 30,
                hasOdoCondition = template.defaultIntervalMeters != null,
                dueOdoInput = targetOdoMeters?.let { m -> OdometerParser.formatForEdit(m, useComma = true) } ?: it.dueOdoInput,
                isRecurring = template.defaultIntervalDays != null || template.defaultIntervalMeters != null,
                intervalDaysInput = template.defaultIntervalDays?.toString() ?: "",
                intervalKmInput = template.defaultIntervalMeters?.let { m -> (m / 1000).toString() } ?: ""
            )
        }
    }

    fun onTitleChanged(v: String) { _formState.update { it.copy(title = v, titleError = null) } }
    fun onCategoryChanged(v: String) { _formState.update { it.copy(category = v) } }
    fun onHasDateToggled(v: Boolean) { _formState.update { it.copy(hasDateCondition = v, conditionError = null) } }
    fun onDueDaysChanged(days: Int) { _formState.update { it.copy(dueDaysFromNow = days) } }
    fun onHasOdoToggled(v: Boolean) { _formState.update { it.copy(hasOdoCondition = v, conditionError = null) } }
    fun onDueOdoChanged(v: String) { _formState.update { it.copy(dueOdoInput = v, conditionError = null) } }
    fun onIsRecurringToggled(v: Boolean) { _formState.update { it.copy(isRecurring = v) } }
    fun onIntervalDaysChanged(v: String) { _formState.update { it.copy(intervalDaysInput = v.filter { char -> char.isDigit() }) } }
    fun onIntervalKmChanged(v: String) { _formState.update { it.copy(intervalKmInput = v.filter { char -> char.isDigit() }) } }

    fun saveSchedule() {
        val state = _formState.value
        if (state.isSubmitting) return

        if (state.title.isBlank()) {
            _formState.update { it.copy(titleError = "Vui lòng nhập tiêu đề lịch bảo dưỡng") }
            return
        }

        if (!state.hasDateCondition && !state.hasOdoCondition) {
            _formState.update { it.copy(conditionError = "Lịch phải có ít nhất một điều kiện đến hạn: theo ngày hoặc theo ODO") }
            return
        }

        val dueAtMillis: Long? = if (state.hasDateCondition) {
            System.currentTimeMillis() + (state.dueDaysFromNow * 86_400_000L)
        } else null

        val dueOdoMeters: Long? = if (state.hasOdoCondition) {
            val res = OdometerParser.parse(state.dueOdoInput)
            if (res is OdometerParser.ParseResult.Error) {
                _formState.update { it.copy(conditionError = res.message) }
                return
            }
            (res as OdometerParser.ParseResult.Success).meters
        } else null

        val intervalDays: Int? = if (state.isRecurring && state.intervalDaysInput.isNotBlank()) {
            state.intervalDaysInput.toIntOrNull()
        } else null

        val intervalMeters: Long? = if (state.isRecurring && state.intervalKmInput.isNotBlank()) {
            state.intervalKmInput.toLongOrNull()?.let { it * 1000 }
        } else null

        _formState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val schedule = MaintenanceSchedule(
                id = editScheduleId ?: 0L,
                vehicleId = state.selectedVehicleId,
                templateId = state.selectedTemplateId,
                title = state.title.trim(),
                category = state.category.trim(),
                dueAt = dueAtMillis,
                dueOdometerMeters = dueOdoMeters,
                intervalDays = intervalDays,
                intervalMeters = intervalMeters,
                status = ScheduleLifecycleStatus.ACTIVE,
                createdAt = now,
                updatedAt = now
            )

            if (editScheduleId != null && editScheduleId > 0) {
                maintenanceRepository.updateSchedule(schedule)
            } else {
                maintenanceRepository.insertSchedule(schedule)
            }

            _formState.update { it.copy(isSubmitting = false, isSaved = true) }
        }
    }

    fun cancelSchedule() {
        if (editScheduleId == null || editScheduleId <= 0) return
        viewModelScope.launch {
            maintenanceRepository.cancelSchedule(editScheduleId)
            _formState.update { it.copy(isSaved = true) }
        }
    }
}
