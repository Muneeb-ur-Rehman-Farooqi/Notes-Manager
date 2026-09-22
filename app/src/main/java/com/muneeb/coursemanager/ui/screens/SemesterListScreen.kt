package com.muneeb.coursemanager.ui.screens

import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.entities.Semester
import com.muneeb.coursemanager.data.formatStageLabel
import com.muneeb.coursemanager.data.preferences.UserPreferences
import com.muneeb.coursemanager.data.repository.SemesterRepository
import com.muneeb.coursemanager.navigation.Routes
import com.muneeb.coursemanager.ui.theme.LocalAppStyle
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
    val showMandatoryDialog: Boolean = false,
    val isDarkMode: Boolean = false
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
            combine(
                semesterRepository.getUniversitySemesters(),
                userPreferences.selectedEducationLevel,
                userPreferences.isDarkModeOrNull
            ) { semesters, level, isDark ->
                val isUniversity = level == "UNIVERSITY"
                val showDialog = isUniversity && semesters.isEmpty()
                _uiState.update { state ->
                    state.copy(
                        semesters = semesters,
                        isLoading = false,
                        error = null,
                        showMandatoryDialog = showDialog,
                        isDarkMode = isDark ?: false
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

    suspend fun renameSemester(semester: Semester, newNumber: String) {
        try {
            semesterRepository.update(semester.copy(name = "Semester $newNumber"))
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to rename semester: ${e.message}") }
        }
    }

    suspend fun deleteSemesters(semesters: List<Semester>) {
        try {
            semesters.forEach { semesterRepository.delete(it) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to delete semester: ${e.message}") }
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

/** Pulls the trailing number/word out of a "Semester X" name for pre-filling the rename field. */
private fun extractSemesterNumber(name: String): String =
    name.removePrefix("Semester").trim()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterListScreen(
    navController: NavController,
    viewModel: SemesterListViewModel,
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val style = LocalAppStyle.current

    var showAddDialog by remember { mutableStateOf(false) }
    var addNumberText by remember { mutableStateOf("") }
    var semesterNumber by remember { mutableStateOf("") } // mandatory dialog field

    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    var renameTarget by remember { mutableStateOf<Semester?>(null) }
    var renameNumberText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection")
                        }
                    },
                    actions = {
                        val singleSelected = if (selectedIds.size == 1) {
                            uiState.semesters.firstOrNull { it.semesterId == selectedIds.first() }
                        } else null
                        if (singleSelected != null) {
                            IconButton(
                                onClick = {
                                    renameTarget = singleSelected
                                    renameNumberText = extractSemesterNumber(singleSelected.name)
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename")
                            }
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Semesters") },
                    navigationIcon = {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Semester") },
                containerColor = style.fabContainerColor,
                contentColor = style.fabContentColor
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
                    color = style.errorTextColor
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.semesters) { semester ->
                        val displayText: String = when {
                            semester.educationLevel == "UNIVERSITY" -> semester.name
                            semester.educationLevel == null -> semester.name
                            else -> formatStageLabel(semester.educationLevel, semester.partGrade)
                        }
                        val isSelected = selectedIds.contains(semester.semesterId)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(4.dp)
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedIds = if (isSelected) {
                                                selectedIds - semester.semesterId
                                            } else {
                                                selectedIds + semester.semesterId
                                            }
                                        } else {
                                            navController.navigate(Routes.courseList(semester.semesterId))
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            selectedIds = setOf(semester.semesterId)
                                        }
                                    }
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = style.cardContainerColor,
                                contentColor = style.cardContentColor
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = style.cardTitleColor,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isSelectionMode) {
                                    Box(modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
                                        Checkbox(checked = isSelected, onCheckedChange = null)
                                    }
                                }
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
                    value = addNumberText,
                    onValueChange = { addNumberText = it },
                    label = { Text("Semester Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val number = addNumberText.trim()
                        if (number.isNotBlank()) {
                            viewModel.viewModelScope.launch {
                                viewModel.addSemester("Semester $number")
                                addNumberText = ""
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

    val currentRenameTarget = renameTarget
    if (currentRenameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Semester") },
            text = {
                OutlinedTextField(
                    value = renameNumberText,
                    onValueChange = { renameNumberText = it },
                    label = { Text("Semester Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameNumberText.isNotBlank(),
                    onClick = {
                        viewModel.viewModelScope.launch {
                            viewModel.renameSemester(currentRenameTarget, renameNumberText.trim())
                            renameTarget = null
                            selectedIds = emptySet()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Semester") },
            text = {
                Text(
                    if (selectedIds.size == 1) {
                        "This will also delete every course, category, and file inside it. This can't be undone."
                    } else {
                        "This will also delete every course, category, and file inside these ${selectedIds.size} semesters. This can't be undone."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = uiState.semesters.filter { selectedIds.contains(it.semesterId) }
                        viewModel.viewModelScope.launch {
                            viewModel.deleteSemesters(toDelete)
                            selectedIds = emptySet()
                            showDeleteConfirm = false
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}