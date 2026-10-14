package com.example.feature.fueltype

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.model.FuelType
import com.example.core.repository.FuelTypeRepository
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

data class FuelTypeManagementUiState(
    val activeTypes: List<FuelType> = emptyList(),
    val archivedTypes: List<FuelType> = emptyList(),
    val showArchived: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class FuelTypeViewModel @Inject constructor(
    private val fuelTypeRepository: FuelTypeRepository
) : ViewModel() {

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    val uiState: StateFlow<FuelTypeManagementUiState> = combine(
        fuelTypeRepository.observeAllFuelTypes(),
        _showArchived
    ) { all, showArchivedFlag ->
        FuelTypeManagementUiState(
            activeTypes = all.filter { !it.isArchived },
            archivedTypes = all.filter { it.isArchived },
            showArchived = showArchivedFlag,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FuelTypeManagementUiState()
    )

    fun toggleShowArchived() {
        _showArchived.update { !it }
    }

    fun saveFuelType(id: Long?, name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (id != null && id > 0) {
                val existing = fuelTypeRepository.getById(id)
                if (existing != null) {
                    fuelTypeRepository.update(
                        existing.copy(
                            name = name.trim(),
                            description = description.trim(),
                            updatedAt = now
                        )
                    )
                }
            } else {
                fuelTypeRepository.insert(
                    FuelType(
                        name = name.trim(),
                        description = description.trim(),
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }
    }

    fun archiveFuelType(id: Long) {
        viewModelScope.launch {
            fuelTypeRepository.archive(id)
        }
    }

    fun unarchiveFuelType(id: Long) {
        viewModelScope.launch {
            fuelTypeRepository.unarchive(id)
        }
    }
}
