package com.muneeb.coursemanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.entities.Semester
import com.muneeb.coursemanager.data.preferences.UserPreferences
import com.muneeb.coursemanager.data.repository.SemesterRepository
import com.muneeb.coursemanager.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SemesterListUiState(
    val semesters: List<Semester> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showMandatoryDialog: Boolean = false
)

class SemesterListViewModel(
    private val semesterRepository: SemesterRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(SemesterListUiState())
    val uiState: StateFlow<SemesterListUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            semesterRepository.getAllSemesters().combine(userPreferences.selectedEducationLevel) { semesters, level ->
                val isUniversity = level == "UNIVERSITY"
                val showDialog = isUniversity && semesters.isEmpty()
                _uiState.update { state ->
                    state.copy(
                        semesters = semesters,
                        isLoading = false,
                        error = null,
                        showMandatoryDialog = showDialog
                    )
                }
            }.collect { /* handled in combine */ }
        }
    }

    suspend fun addSemester(name: String) {
        try {
            val semester = Semester(
                name = name,
                sortOrder = _uiState.value.semesters.size,
                educationLevel = "UNIVERSITY"
            )
            semesterRepository.insert(semester)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to add semester: ${e.message}") }
        }
    }

    fun dismissMandatoryDialog() {
        _uiState.update { it.copy(showMandatoryDialog = false) }
    }

    class Factory(
        private val semesterRepository: SemesterRepository,
        private val userPreferences: UserPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SemesterListViewModel::class.java)) {
                return SemesterListViewModel(semesterRepository, userPreferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterListScreen(
    navController: NavController,
    viewModel: SemesterListViewModel,
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newSemesterName by remember { mutableStateOf("") }
    var semesterNumber by remember { mutableStateOf("") }

    if (uiState.showMandatoryDialog) {
        AlertDialog(
            onDismissRequest = { /* non-dismissible */ },
            title = { Text("Create Semester") },
            text = {
                Column {
                    Text("Please enter a semester number (e.g., 1, 2, 3)")
                    OutlinedTextField(
                        value = semesterNumber,
                        onValueChange = { semesterNumber = it },
                        label = { Text("Semester Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val number = semesterNumber.trim()
                        if (number.isNotBlank()) {
                            val name = "Semester $number"
                            viewModel.viewModelScope.launch {
                                viewModel.addSemester(name)
                                semesterNumber = ""
                                viewModel.dismissMandatoryDialog()
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = null
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Semesters") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Semester") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null && uiState.semesters.isEmpty()) {
                Text(
                    text = "Error: ${uiState.error}",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Red
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.semesters) { semester ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(4.dp),
                            shape = RoundedCornerShape(16.dp),
                            onClick = {
                                navController.navigate(Routes.courseList(semester.semesterId))
                            }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = semester.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color(0xFF222222)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Semester") },
            text = {
                OutlinedTextField(
                    value = newSemesterName,
                    onValueChange = { newSemesterName = it },
                    label = { Text("Semester Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newSemesterName.isNotBlank()) {
                            viewModel.viewModelScope.launch {
                                viewModel.addSemester(newSemesterName.trim())
                                newSemesterName = ""
                                showAddDialog = false
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}