package com.muneeb.coursemanager.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.muneeb.coursemanager.data.entities.Semester
import com.muneeb.coursemanager.data.preferences.UserPreferences
import com.muneeb.coursemanager.data.repository.OnboardingRepository
import com.muneeb.coursemanager.data.repository.SemesterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val selectedEducationLevel: String? = null,
    val selectedPartGrade: String? = null,
    val selectedGroup: String? = null,
    val selectedElectiveChoice: String? = null,
    val selectedFreeTextElectives: List<String>? = null,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
)

class OnboardingViewModel(
    private val userPreferences: UserPreferences,
    private val onboardingRepository: OnboardingRepository,
    private val semesterRepository: SemesterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun setEducationLevel(level: String) {
        _uiState.update { state ->
            state.copy(
                selectedEducationLevel = level,
                selectedPartGrade = null,
                selectedGroup = null,
                selectedElectiveChoice = null,
                selectedFreeTextElectives = null,
                error = null
            )
        }
    }

    fun setPartGrade(partGrade: String) {
        _uiState.update { state ->
            state.copy(
                selectedPartGrade = partGrade,
                selectedGroup = null,
                selectedElectiveChoice = null,
                selectedFreeTextElectives = null,
                error = null
            )
        }
    }

    fun setGroup(group: String) {
        _uiState.update { state ->
            state.copy(
                selectedGroup = group,
                selectedElectiveChoice = null,
                selectedFreeTextElectives = null,
                error = null
            )
        }
    }

    fun setElectiveChoice(choice: String) {
        _uiState.update { state ->
            state.copy(
                selectedElectiveChoice = choice,
                error = null
            )
        }
    }

    fun setFreeTextElectives(texts: List<String>) {
        _uiState.update { state ->
            state.copy(
                selectedFreeTextElectives = texts,
                error = null
            )
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val state = _uiState.value
            val level = state.selectedEducationLevel

            if (level == null) {
                _uiState.update { it.copy(error = "Please select an education level.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // University: skip semester creation
                if (level == "UNIVERSITY") {
                    userPreferences.setSelectedEducationLevel(level)
                    userPreferences.setSelectedGroup(state.selectedGroup)
                    userPreferences.setSelectedSemesterId(null)
                    _uiState.update { it.copy(isComplete = true) }
                    return@launch
                }

                // Matric/Inter: validate part and group
                val part = state.selectedPartGrade
                val group = state.selectedGroup
                if (part == null) {
                    _uiState.update { it.copy(error = "Please select a part/grade.") }
                    return@launch
                }
                if (group == null) {
                    _uiState.update { it.copy(error = "Please select a group.") }
                    return@launch
                }

                // Create semester
                val semester = Semester(
                    name = "$level $part",
                    sortOrder = 0,
                    educationLevel = level,
                    partGrade = part
                )
                val semesterId = semesterRepository.insert(semester)

                // Apply template
                onboardingRepository.applyEducationTemplate(
                    semesterId = semesterId,
                    level = level,
                    part = part,
                    group = group,
                    electiveChoice = state.selectedElectiveChoice,
                    freeTexts = state.selectedFreeTextElectives
                )

                // Save preferences
                userPreferences.setSelectedEducationLevel(level)
                userPreferences.setSelectedGroup(group)
                userPreferences.setSelectedSemesterId(semesterId)

                _uiState.update { it.copy(isComplete = true) }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Unknown error occurred.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    class Factory(
        private val userPreferences: UserPreferences,
        private val onboardingRepository: OnboardingRepository,
        private val semesterRepository: SemesterRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                return OnboardingViewModel(
                    userPreferences,
                    onboardingRepository,
                    semesterRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}