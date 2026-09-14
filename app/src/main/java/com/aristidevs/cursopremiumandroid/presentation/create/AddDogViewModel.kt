package com.aristidevs.cursopremiumandroid.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationError
import com.aristidevs.cursopremiumandroid.domain.model.NewDog
import com.aristidevs.cursopremiumandroid.domain.usecase.AddDogResult
import com.aristidevs.cursopremiumandroid.domain.usecase.AddDogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddDogViewModel @Inject constructor(private val addDogUseCase: AddDogUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDogUiState())
    val uiState: StateFlow<AddDogUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = updateField { it.copy(name = value) }
    fun onBreedChange(value: String) = updateField { it.copy(breed = value) }
    fun onAgeChange(value: String) = updateField { it.copy(ageText = value) }
    fun onDescriptionChange(value: String) = updateField { it.copy(description = value) }
    fun onWeightChange(value: String) = updateField { it.copy(weight = value) }
    fun onOriginChange(value: String) = updateField { it.copy(origin = value) }
    fun onTemperamentChange(value: String) = updateField { it.copy(temperament = value) }
    fun onPhotoPicked(uri: String?) = updateField { it.copy(photoUri = uri) }

    /** True when there is something the user would lose by leaving (RF-11). */
    private fun hasUnsavedChanges(state: AddDogUiState): Boolean {
        return state.name.isNotBlank() || state.breed.isNotBlank() || state.ageText.isNotBlank() ||
            state.description.isNotBlank() || state.photoUri != null || state.weight.isNotBlank() ||
            state.origin.isNotBlank() || state.temperament.isNotBlank()
    }

    fun onBackRequested() {
        if (hasUnsavedChanges(_uiState.value)) {
            _uiState.update { it.copy(showDiscardConfirmation = true) }
        } else {
            _uiState.update { it.copy(navigateBack = true) }
        }
    }

    fun onDiscardConfirmed() {
        _uiState.update { it.copy(showDiscardConfirmation = false, navigateBack = true) }
    }

    fun onDiscardDismissed() {
        _uiState.update { it.copy(showDiscardConfirmation = false) }
    }

    /**
     * Guarded so that several rapid taps only ever create one dog (CA-11): the
     * in-progress flag is read and set as the very first thing inside the
     * coroutine, before any suspending work, so a second launch queued on the
     * same single-threaded dispatcher always observes it already set.
     */
    fun onSaveClicked() {
        viewModelScope.launch {
            if (_uiState.value.isSaving) return@launch
            _uiState.update { it.copy(isSaving = true) }

            val state = _uiState.value
            val newDog = NewDog(
                name = state.name.trim(),
                breed = state.breed.trim(),
                age = state.ageText.trim().toIntOrNull(),
                description = state.description.trim(),
                photoUri = state.photoUri,
                weight = state.weight.trim().ifBlank { null },
                origin = state.origin.trim().ifBlank { null },
                temperament = state.temperament.trim().ifBlank { null }
            )

            when (val result = addDogUseCase(newDog)) {
                is AddDogResult.Success -> _uiState.update {
                    it.copy(isSaving = false, navigateBack = true)
                }

                is AddDogResult.Invalid -> _uiState.update {
                    it.copy(isSaving = false, errors = result.errors)
                }
            }
        }
    }

    private inline fun updateField(transform: (AddDogUiState) -> AddDogUiState) {
        _uiState.update { state -> transform(state).copy(errors = emptySet()) }
    }
}

data class AddDogUiState(
    val name: String = "",
    val breed: String = "",
    val ageText: String = "",
    val description: String = "",
    val photoUri: String? = null,
    val weight: String = "",
    val origin: String = "",
    val temperament: String = "",
    val isSaving: Boolean = false,
    val errors: Set<DogValidationError> = emptySet(),
    val showDiscardConfirmation: Boolean = false,
    val navigateBack: Boolean = false
)
