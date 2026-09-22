package com.muneeb.coursemanager.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.SubjectTemplates
import com.muneeb.coursemanager.data.entities.Course
import com.muneeb.coursemanager.data.entities.Semester
import com.muneeb.coursemanager.data.repository.CourseRepository
import com.muneeb.coursemanager.data.repository.OnboardingRepository
import com.muneeb.coursemanager.data.repository.SemesterRepository
import com.muneeb.coursemanager.navigation.Routes
import com.muneeb.coursemanager.ui.theme.LocalAppStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CourseListUiState(
    val courses: List<Course> = emptyList(),
    val semester: Semester? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class CourseListViewModel(
    private val courseRepository: CourseRepository,
    private val onboardingRepository: OnboardingRepository,
    private val semesterRepository: SemesterRepository,
    private val semesterId: Long
) : ViewModel() {
    private val _uiState = MutableStateFlow(CourseListUiState())
    val uiState: StateFlow<CourseListUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                semesterRepository.getSemesterById(semesterId).collect { semester ->
                    if (semester == null) {
                        _uiState.update { it.copy(error = "Semester not found", isLoading = false) }
                        return@collect
                    }
                    _uiState.update { it.copy(semester = semester) }
                    courseRepository.getCoursesForSemester(semesterId).collect { courses ->
                        _uiState.update { state ->
                            state.copy(courses = courses, isLoading = false)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    suspend fun addCourse(name: String, code: String?) {
        try {
            val semester = _uiState.value.semester
            val categories = if (semester?.educationLevel != null && semester.partGrade != null) {
                SubjectTemplates.getDefaultCategories(
                    subjectName = name,
                    level = semester.educationLevel!!,
                    part = semester.partGrade
                )
            } else {
                SubjectTemplates.getDefaultCategories(
                    subjectName = name,
                    level = "UNIVERSITY",
                    part = null
                )
            }
            onboardingRepository.createCourseWithDefaultCategories(
                semesterId = semesterId,
                courseName = name,
                courseCode = code,
                categories = categories
            )
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to add course: ${e.message}") }
        }
    }

    suspend fun renameCourse(course: Course, newName: String, newCode: String?) {
        try {
            courseRepository.update(course.copy(name = newName, courseCode = newCode))
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to rename course: ${e.message}") }
        }
    }

    suspend fun deleteCourses(courses: List<Course>) {
        try {
            courses.forEach { courseRepository.delete(it) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to delete course: ${e.message}") }
        }
    }

    class Factory(
        private val courseRepository: CourseRepository,
        private val onboardingRepository: OnboardingRepository,
        private val semesterRepository: SemesterRepository,
        private val semesterId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CourseListViewModel::class.java)) {
                return CourseListViewModel(
                    courseRepository,
                    onboardingRepository,
                    semesterRepository,
                    semesterId
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    navController: NavController,
    viewModel: CourseListViewModel,
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val style = LocalAppStyle.current

    var showAddDialog by remember { mutableStateOf(false) }
    var newCourseName by remember { mutableStateOf("") }
    var newCourseCode by remember { mutableStateOf("") }

    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    var renameTarget by remember { mutableStateOf<Course?>(null) }
    var renameNameText by remember { mutableStateOf("") }
    var renameCodeText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isUniversity = uiState.semester?.educationLevel == "UNIVERSITY"
    val titleText = if (isUniversity) {
        uiState.semester?.name ?: "Courses"
    } else {
        "Subjects"
    }
    val fabLabel = if (isUniversity) "Add Course" else "Add Subject"
    val nameFieldLabel = if (isUniversity) "Course Name" else "Subject Name"

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
                            uiState.courses.firstOrNull { it.courseId == selectedIds.first() }
                        } else null
                        if (singleSelected != null) {
                            IconButton(
                                onClick = {
                                    renameTarget = singleSelected
                                    renameNameText = singleSelected.name
                                    renameCodeText = singleSelected.courseCode ?: ""
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
                    title = { Text(titleText) },
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
                text = { Text(fabLabel) },
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
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null && uiState.courses.isEmpty()) {
                Text(
                    text = "Error: ${uiState.error}",
                    modifier = Modifier.padding(16.dp),
                    color = style.errorTextColor
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.courses) { course ->
                        val isSelected = selectedIds.contains(course.courseId)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedIds = if (isSelected) {
                                                selectedIds - course.courseId
                                            } else {
                                                selectedIds + course.courseId
                                            }
                                        } else {
                                            navController.navigate(Routes.categoryList(course.courseId))
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            selectedIds = setOf(course.courseId)
                                        }
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelectionMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = null,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                    Text(
                                        text = course.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = style.cardTitleColor
                                    )
                                }
                                course.courseCode?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = style.cardSubtitleColor
                                    )
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
            title = { Text(fabLabel) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCourseName,
                        onValueChange = { newCourseName = it },
                        label = { Text(nameFieldLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = newCourseCode,
                        onValueChange = { newCourseCode = it },
                        label = { Text("Course Code (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCourseName.isNotBlank()) {
                            viewModel.viewModelScope.launch {
                                viewModel.addCourse(
                                    name = newCourseName.trim(),
                                    code = newCourseCode.trim().takeIf { it.isNotBlank() }
                                )
                                newCourseName = ""
                                newCourseCode = ""
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
            title = { Text("Rename") },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameNameText,
                        onValueChange = { renameNameText = it },
                        label = { Text(nameFieldLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = renameCodeText,
                        onValueChange = { renameCodeText = it },
                        label = { Text("Course Code (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = renameNameText.isNotBlank(),
                    onClick = {
                        viewModel.viewModelScope.launch {
                            viewModel.renameCourse(
                                currentRenameTarget,
                                renameNameText.trim(),
                                renameCodeText.trim().takeIf { it.isNotBlank() }
                            )
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
            title = { Text("Delete") },
            text = {
                Text(
                    if (selectedIds.size == 1) {
                        "This will also delete every category and file inside it. This can't be undone."
                    } else {
                        "This will also delete every category and file inside these ${selectedIds.size} items. This can't be undone."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = uiState.courses.filter { selectedIds.contains(it.courseId) }
                        viewModel.viewModelScope.launch {
                            viewModel.deleteCourses(toDelete)
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