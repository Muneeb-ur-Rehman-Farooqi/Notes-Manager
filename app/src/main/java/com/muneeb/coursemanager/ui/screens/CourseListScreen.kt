package com.muneeb.coursemanager.ui.screens

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
    var showAddDialog by remember { mutableStateOf(false) }
    var newCourseName by remember { mutableStateOf("") }
    var newCourseCode by remember { mutableStateOf("") }

    val isUniversity = uiState.semester?.educationLevel == "UNIVERSITY"
    val titleText = if (isUniversity) {
        uiState.semester?.name ?: "Courses"
    } else {
        "Subjects"
    }
    val fabLabel = if (isUniversity) "Add Course" else "Add Subject"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText) },
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
                text = { Text(fabLabel) }
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
                    color = Color.Red
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.courses) { course ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            onClick = {
                                navController.navigate(Routes.categoryList(course.courseId))
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                course.courseCode?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
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
                        label = { Text(if (isUniversity) "Course Name" else "Subject Name") },
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
}