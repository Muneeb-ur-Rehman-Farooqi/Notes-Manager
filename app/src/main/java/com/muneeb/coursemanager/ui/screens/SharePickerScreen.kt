package com.muneeb.coursemanager.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.entities.Category
import com.muneeb.coursemanager.data.entities.Course
import com.muneeb.coursemanager.data.entities.Item
import com.muneeb.coursemanager.data.entities.ItemType
import com.muneeb.coursemanager.data.entities.Semester
import com.muneeb.coursemanager.data.preferences.UserPreferences
import com.muneeb.coursemanager.data.repository.CategoryRepository
import com.muneeb.coursemanager.data.repository.CourseRepository
import com.muneeb.coursemanager.data.repository.ItemRepository
import com.muneeb.coursemanager.data.repository.SemesterRepository
import com.muneeb.coursemanager.navigation.Routes
import com.muneeb.coursemanager.ui.theme.LocalAppStyle
import com.muneeb.coursemanager.util.StagedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PickerStep {
    SEMESTER,
    COURSE,
    CATEGORY
}

data class SharePickerUiState(
    val step: PickerStep = PickerStep.SEMESTER,
    val initialStep: PickerStep = PickerStep.SEMESTER,
    val isUniversity: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val semesters: List<Semester> = emptyList(),
    val courses: List<Course> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedSemester: Semester? = null,
    val selectedCourse: Course? = null,
    val initialSemesterId: Long? = null
)

class SharePickerViewModel(
    private val semesterRepository: SemesterRepository,
    private val courseRepository: CourseRepository,
    private val categoryRepository: CategoryRepository,
    private val itemRepository: ItemRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharePickerUiState())
    val uiState: StateFlow<SharePickerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val level = userPreferences.selectedEducationLevel.firstOrNull()
                val semesterId = userPreferences.selectedSemesterId.firstOrNull()
                val isUniversity = level == "UNIVERSITY"

                if (!isUniversity && semesterId != null) {
                    val courses = courseRepository.getCoursesForSemester(semesterId).first()
                    _uiState.update {
                        it.copy(
                            step = PickerStep.COURSE,
                            initialStep = PickerStep.COURSE,
                            isUniversity = false,
                            initialSemesterId = semesterId,
                            courses = courses,
                            isLoading = false
                        )
                    }
                } else {
                    val semesters = semesterRepository.getUniversitySemesters().first()
                    _uiState.update {
                        it.copy(
                            step = PickerStep.SEMESTER,
                            initialStep = PickerStep.SEMESTER,
                            isUniversity = true,
                            semesters = semesters,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load destinations", isLoading = false) }
            }
        }
    }

    fun selectSemester(semester: Semester) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val courses = courseRepository.getCoursesForSemester(semester.semesterId).first()
                _uiState.update {
                    it.copy(
                        step = PickerStep.COURSE,
                        selectedSemester = semester,
                        courses = courses,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load courses", isLoading = false) }
            }
        }
    }

    fun selectCourse(course: Course) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val categories = categoryRepository.getCategoriesForCourse(course.courseId).first()
                _uiState.update {
                    it.copy(
                        step = PickerStep.CATEGORY,
                        selectedCourse = course,
                        categories = categories,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load categories", isLoading = false) }
            }
        }
    }

    fun navigateStepBack() {
        val current = _uiState.value
        if (current.step == current.initialStep) return
        when (current.step) {
            PickerStep.CATEGORY -> {
                _uiState.update {
                    it.copy(
                        step = PickerStep.COURSE,
                        selectedCourse = null,
                        error = null
                    )
                }
            }
            PickerStep.COURSE -> {
                _uiState.update {
                    it.copy(
                        step = PickerStep.SEMESTER,
                        selectedSemester = null,
                        selectedCourse = null,
                        error = null
                    )
                }
            }
            PickerStep.SEMESTER -> Unit
        }
    }

    fun saveFilesToCategory(
        category: Category,
        stagedFiles: List<StagedFile>,
        onSuccess: (categoryId: Long, categoryName: String) -> Unit
    ) {
        if (stagedFiles.isEmpty() || _uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val existingCount = itemRepository.getItemsForCategory(category.categoryId).first().size
                stagedFiles.forEachIndexed { index, file ->
                    itemRepository.insert(
                        Item(
                            categoryId = category.categoryId,
                            itemType = ItemType.FILE,
                            displayName = file.displayName,
                            uri = file.contentUri,
                            mimeType = file.mimeType,
                            sizeBytes = file.sizeBytes,
                            sortOrder = existingCount + index
                        )
                    )
                }
                _uiState.update { it.copy(isSaving = false) }
                onSuccess(category.categoryId, category.name)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "Failed to save files")
                }
            }
        }
    }

    class Factory(
        private val semesterRepository: SemesterRepository,
        private val courseRepository: CourseRepository,
        private val categoryRepository: CategoryRepository,
        private val itemRepository: ItemRepository,
        private val userPreferences: UserPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SharePickerViewModel::class.java)) {
                return SharePickerViewModel(
                    semesterRepository,
                    courseRepository,
                    categoryRepository,
                    itemRepository,
                    userPreferences
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePickerScreen(
    navController: NavController,
    viewModel: SharePickerViewModel,
    shareImportViewModel: ShareImportViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val stagedFiles by shareImportViewModel.staged.collectAsState()
    val style = LocalAppStyle.current
    val context = LocalContext.current

    // Pop once on initial composition if there are no staged files
    LaunchedEffect(Unit) {
        if (shareImportViewModel.staged.value.isEmpty()) {
            navController.popBackStack()
        }
    }

    val handleCancel: () -> Unit = {
        shareImportViewModel.cancel()
        navController.popBackStack()
    }

    BackHandler {
        if (uiState.step == uiState.initialStep) {
            handleCancel()
        } else {
            viewModel.navigateStepBack()
        }
    }

    val stepTitle = when (uiState.step) {
        PickerStep.SEMESTER -> "Choose semester"
        PickerStep.COURSE -> if (uiState.isUniversity) "Choose course" else "Choose subject"
        PickerStep.CATEGORY -> "Choose category"
    }

    val pathParts = listOfNotNull(
        uiState.selectedSemester?.name,
        uiState.selectedCourse?.name
    )
    val pathSubtitle = pathParts.joinToString(" \u203A ")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stepTitle) },
                navigationIcon = {
                    IconButton(onClick = handleCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel import")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Context header showing chosen path and file count
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (pathSubtitle.isNotEmpty()) {
                    Text(
                        text = pathSubtitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = style.cardSubtitleColor
                    )
                }
                Text(
                    text = "Saving ${stagedFiles.size} file(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = style.cardSubtitleColor
                )
            }

            if (uiState.error != null) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = style.errorTextColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (uiState.isLoading || uiState.isSaving) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (uiState.step) {
                    PickerStep.SEMESTER -> {
                        if (uiState.semesters.isEmpty()) {
                            EmptyPickerMessage("No semesters here yet")
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.semesters) { semester ->
                                    PickerRowCard(
                                        title = semester.name,
                                        onClick = { viewModel.selectSemester(semester) }
                                    )
                                }
                            }
                        }
                    }

                    PickerStep.COURSE -> {
                        if (uiState.courses.isEmpty()) {
                            EmptyPickerMessage("No courses here yet")
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.courses) { course ->
                                    PickerRowCard(
                                        title = course.name,
                                        subtitle = course.courseCode,
                                        onClick = { viewModel.selectCourse(course) }
                                    )
                                }
                            }
                        }
                    }

                    PickerStep.CATEGORY -> {
                        if (uiState.categories.isEmpty()) {
                            EmptyPickerMessage("No categories here yet")
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.categories) { category ->
                                    PickerRowCard(
                                        title = category.name,
                                        onClick = {
                                            viewModel.saveFilesToCategory(
                                                category = category,
                                                stagedFiles = stagedFiles,
                                                onSuccess = { catId, catName ->
                                                    Toast.makeText(
                                                        context,
                                                        "Saved to $catName",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    shareImportViewModel.onSaved()
                                                    navController.navigate(Routes.itemList(catId)) {
                                                        popUpTo(Routes.SHARE_PICKER) { inclusive = true }
                                                    }
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPickerMessage(message: String) {
    val style = LocalAppStyle.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = style.cardSubtitleColor
        )
    }
}

@Composable
private fun PickerRowCard(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val style = LocalAppStyle.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = style.cardTitleColor
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = style.cardSubtitleColor
                )
            }
        }
    }
}