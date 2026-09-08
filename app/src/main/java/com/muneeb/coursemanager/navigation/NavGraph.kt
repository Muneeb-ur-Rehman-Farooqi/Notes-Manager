package com.muneeb.coursemanager.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.muneeb.coursemanager.data.preferences.UserPreferences
import com.muneeb.coursemanager.data.repository.CategoryRepository
import com.muneeb.coursemanager.data.repository.CourseRepository
import com.muneeb.coursemanager.data.repository.ItemRepository
import com.muneeb.coursemanager.data.repository.OnboardingRepository
import com.muneeb.coursemanager.data.repository.QuickNoteRepository
import com.muneeb.coursemanager.data.repository.SemesterRepository
import com.muneeb.coursemanager.data.repository.StudyTaskRepository
import com.muneeb.coursemanager.data.repository.TimetableRepository
import com.muneeb.coursemanager.ui.onboarding.EducationLevelScreen
import com.muneeb.coursemanager.ui.onboarding.ElectiveChoiceScreen
import com.muneeb.coursemanager.ui.onboarding.FreeTextElectiveScreen
import com.muneeb.coursemanager.ui.onboarding.GroupSelectionScreen
import com.muneeb.coursemanager.ui.onboarding.OnboardingViewModel
import com.muneeb.coursemanager.ui.onboarding.PartOrGradeSelectionScreen
import com.muneeb.coursemanager.ui.screens.CategoryListScreen
import com.muneeb.coursemanager.ui.screens.CategoryListViewModel
import com.muneeb.coursemanager.ui.screens.CourseListScreen
import com.muneeb.coursemanager.ui.screens.CourseListViewModel
import com.muneeb.coursemanager.ui.screens.ItemListScreen
import com.muneeb.coursemanager.ui.screens.ItemListViewModel
import com.muneeb.coursemanager.ui.screens.NoteEditorScreen
import com.muneeb.coursemanager.ui.screens.NoteEditorViewModel
import com.muneeb.coursemanager.ui.screens.NotesListScreen
import com.muneeb.coursemanager.ui.screens.NotesListViewModel
import com.muneeb.coursemanager.ui.screens.QuickNoteEditorScreen
import com.muneeb.coursemanager.ui.screens.QuickNoteEditorViewModel
import com.muneeb.coursemanager.ui.screens.SemesterListScreen
import com.muneeb.coursemanager.ui.screens.SemesterListViewModel
import com.muneeb.coursemanager.ui.screens.StudyTaskEditorScreen
import com.muneeb.coursemanager.ui.screens.StudyTaskEditorViewModel
import com.muneeb.coursemanager.ui.screens.StudyTaskListScreen
import com.muneeb.coursemanager.ui.screens.StudyTaskListViewModel
import com.muneeb.coursemanager.ui.screens.TimetableEntryEditorScreen
import com.muneeb.coursemanager.ui.screens.TimetableEditorViewModel
import com.muneeb.coursemanager.ui.screens.TimetableListScreen
import com.muneeb.coursemanager.ui.screens.TimetableListViewModel
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: OnboardingViewModel,
    startDestination: String,
    onMenuClick: () -> Unit,
    semesterRepository: SemesterRepository,
    courseRepository: CourseRepository,
    categoryRepository: CategoryRepository,
    itemRepository: ItemRepository,
    onboardingRepository: OnboardingRepository,
    quickNoteRepository: QuickNoteRepository,
    timetableRepository: TimetableRepository,
    studyTaskRepository: StudyTaskRepository,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Onboarding screens
        composable(Routes.EDUCATION_LEVEL) {
            EducationLevelScreen(navController = navController, viewModel = viewModel)
        }
        composable(Routes.PART_GRADE_SELECTION) {
            PartOrGradeSelectionScreen(navController = navController, viewModel = viewModel)
        }
        composable(Routes.GROUP_SELECTION) {
            GroupSelectionScreen(navController = navController, viewModel = viewModel)
        }
        composable(Routes.ELECTIVE_CHOICE) {
            ElectiveChoiceScreen(navController = navController, viewModel = viewModel)
        }
        composable(Routes.FREE_TEXT_ELECTIVE) {
            FreeTextElectiveScreen(navController = navController, viewModel = viewModel)
        }

        // Top-level destinations with menu
        composable(Routes.SEMESTER_LIST) {
            val factory = SemesterListViewModel.Factory(semesterRepository, userPreferences)
            val listViewModel: SemesterListViewModel = viewModel(factory = factory)
            SemesterListScreen(navController = navController, viewModel = listViewModel, onMenuClick = onMenuClick)
        }
        composable(Routes.NOTES_LIST) {
            val factory = NotesListViewModel.Factory(quickNoteRepository)
            val notesViewModel: NotesListViewModel = viewModel(factory = factory)
            NotesListScreen(navController = navController, viewModel = notesViewModel, onMenuClick = onMenuClick)
        }
        composable(Routes.TIMETABLE_LIST) {
            val factory = TimetableListViewModel.Factory(application, timetableRepository)
            val timetableViewModel: TimetableListViewModel = viewModel(factory = factory)
            TimetableListScreen(navController = navController, viewModel = timetableViewModel, onMenuClick = onMenuClick)
        }
        composable(Routes.STUDY_TASK_LIST) {
            val factory = StudyTaskListViewModel.Factory(application, studyTaskRepository)
            val studyTaskViewModel: StudyTaskListViewModel = viewModel(factory = factory)
            StudyTaskListScreen(navController = navController, viewModel = studyTaskViewModel, onMenuClick = onMenuClick)
        }

        // Course List - now with menu
        composable(
            route = Routes.COURSE_LIST,
            arguments = listOf(navArgument("semesterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val semesterId = backStackEntry.arguments?.getLong("semesterId") ?: 0L
            val factory = CourseListViewModel.Factory(
                courseRepository, onboardingRepository, semesterRepository, semesterId
            )
            val listViewModel: CourseListViewModel = viewModel(factory = factory)
            CourseListScreen(navController = navController, viewModel = listViewModel, onMenuClick = onMenuClick)
        }

        // Category List - now with menu
        composable(
            route = Routes.CATEGORY_LIST,
            arguments = listOf(navArgument("courseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getLong("courseId") ?: 0L
            val factory = CategoryListViewModel.Factory(categoryRepository, courseId)
            val listViewModel: CategoryListViewModel = viewModel(factory = factory)
            CategoryListScreen(navController = navController, viewModel = listViewModel, onMenuClick = onMenuClick)
        }

        // Item List - now with menu
        composable(
            route = Routes.ITEM_LIST,
            arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
            val factory = ItemListViewModel.Factory(itemRepository, categoryRepository, categoryId)
            val listViewModel: ItemListViewModel = viewModel(factory = factory)
            ItemListScreen(navController = navController, viewModel = listViewModel, onMenuClick = onMenuClick)
        }

        // Editors - no menu
        composable(
            route = Routes.NOTE_EDITOR,
            arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
            val factory = NoteEditorViewModel.Factory(itemRepository, categoryId)
            val noteViewModel: NoteEditorViewModel = viewModel(factory = factory)
            NoteEditorScreen(navController = navController, viewModel = noteViewModel)
        }
        composable(
            route = Routes.QUICK_NOTE_EDITOR,
            arguments = listOf(navArgument("noteId") { defaultValue = -1L })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
            val factory = QuickNoteEditorViewModel.Factory(quickNoteRepository, noteId)
            val editorViewModel: QuickNoteEditorViewModel = viewModel(factory = factory)
            QuickNoteEditorScreen(navController = navController, viewModel = editorViewModel)
        }
        composable(
            route = Routes.TIMETABLE_EDITOR,
            arguments = listOf(navArgument("entryId") { defaultValue = -1L })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
            val factory = TimetableEditorViewModel.Factory(application, timetableRepository, entryId)
            val editorViewModel: TimetableEditorViewModel = viewModel(factory = factory)
            TimetableEntryEditorScreen(navController = navController, viewModel = editorViewModel)
        }
        composable(
            route = Routes.STUDY_TASK_EDITOR,
            arguments = listOf(navArgument("taskId") { defaultValue = -1L })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
            val factory = StudyTaskEditorViewModel.Factory(application, studyTaskRepository, taskId)
            val editorViewModel: StudyTaskEditorViewModel = viewModel(factory = factory)
            StudyTaskEditorScreen(navController = navController, viewModel = editorViewModel)
        }
    }

    // Post-onboarding navigation
    val uiState by viewModel.uiState.collectAsState()
    var pendingNavigation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete && !pendingNavigation) {
            pendingNavigation = true
            val level = uiState.selectedEducationLevel
            if (level == "UNIVERSITY") {
                navController.navigate(Routes.SEMESTER_LIST) {
                    popUpTo(Routes.EDUCATION_LEVEL) { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                val semesterId = userPreferences.selectedSemesterId.firstOrNull()
                if (semesterId != null) {
                    navController.navigate(Routes.courseList(semesterId)) {
                        popUpTo(Routes.EDUCATION_LEVEL) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(Routes.SEMESTER_LIST) {
                        popUpTo(Routes.EDUCATION_LEVEL) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}