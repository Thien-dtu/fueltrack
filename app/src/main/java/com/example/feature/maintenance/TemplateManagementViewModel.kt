package com.example.feature.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.model.MaintenanceTemplate
import com.example.core.repository.MaintenanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class TemplateManagementUiState(
    val builtInTemplates: List<MaintenanceTemplate> = emptyList(),
    val userTemplates: List<MaintenanceTemplate> = emptyList(),
    val archivedTemplates: List<MaintenanceTemplate> = emptyList(),
    val showArchived: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class TemplateManagementViewModel @Inject constructor(
    private val maintenanceRepository: MaintenanceRepository
) : ViewModel() {

    private val _showArchived = MutableStateFlow(false)
    val showArchived = _showArchived.asStateFlow()

    val uiState: StateFlow<TemplateManagementUiState> = combine(
        maintenanceRepository.observeAllTemplates(),
        _showArchived
    ) { all, showArchivedFlag ->
        val active = all.filter { !it.isArchived }
        val archived = all.filter { it.isArchived }
        val builtIn = active.filter { it.isBuiltIn }
        val custom = active.filter { !it.isBuiltIn }

        TemplateManagementUiState(
            builtInTemplates = builtIn,
            userTemplates = custom,
            archivedTemplates = archived,
            showArchived = showArchivedFlag,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TemplateManagementUiState()
    )

    fun toggleShowArchived() { _showArchived.update { !it } }

    fun saveTemplate(id: Long?, name: String, category: String, days: Int?, km: Long?, desc: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val template = MaintenanceTemplate(
                id = id ?: 0L,
                name = name.trim(),
                category = category.trim(),
                defaultIntervalDays = days,
                defaultIntervalMeters = km?.let { it * 1000 },
                description = desc.trim(),
                isBuiltIn = false,
                isArchived = false,
                createdAt = now,
                updatedAt = now
            )
            if (id != null && id > 0) {
                maintenanceRepository.updateTemplate(template)
            } else {
                maintenanceRepository.insertTemplate(template)
            }
        }
    }

    fun archiveTemplate(id: Long) {
        viewModelScope.launch { maintenanceRepository.archiveTemplate(id) }
    }

    fun unarchiveTemplate(id: Long) {
        viewModelScope.launch { maintenanceRepository.unarchiveTemplate(id) }
    }
}
