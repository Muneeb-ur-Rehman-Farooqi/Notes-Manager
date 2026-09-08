package com.muneeb.coursemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.muneeb.coursemanager.data.database.AppDatabase
import com.muneeb.coursemanager.data.preferences.UserPreferences
import com.muneeb.coursemanager.data.repository.CategoryRepository
import com.muneeb.coursemanager.data.repository.CourseRepository
import com.muneeb.coursemanager.data.repository.ItemRepository
import com.muneeb.coursemanager.data.repository.OnboardingRepository
import com.muneeb.coursemanager.data.repository.QuickNoteRepository
import com.muneeb.coursemanager.data.repository.SemesterRepository
import com.muneeb.coursemanager.data.repository.StudyTaskRepository
import com.muneeb.coursemanager.data.repository.TimetableRepository
import com.muneeb.coursemanager.navigation.AppNavHost
import com.muneeb.coursemanager.navigation.Routes
import com.muneeb.coursemanager.ui.components.AppDrawerContent
import com.muneeb.coursemanager.ui.util.RequestNotificationPermission
import com.muneeb.coursemanager.ui.onboarding.OnboardingViewModel
import com.muneeb.coursemanager.reminders.NotificationChannels
import com.muneeb.coursemanager.ui.theme.NotesManagerTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        NotificationChannels.createChannels(this)

        val database = AppDatabase.getInstance(this)
        val userPreferences = UserPreferences(this)

        val semesterDao = database.semesterDao()
        val courseDao = database.courseDao()
        val categoryDao = database.categoryDao()
        val itemDao = database.itemDao()
        val quickNoteDao = database.quickNoteDao()
        val timetableDao = database.timetableDao()
        val studyTaskDao = database.studyTaskDao()

        val semesterRepository = SemesterRepository(semesterDao)
        val courseRepository = CourseRepository(courseDao)
        val categoryRepository = CategoryRepository(categoryDao)
        val itemRepository = ItemRepository(itemDao)
        val quickNoteRepository = QuickNoteRepository(quickNoteDao)
        val timetableRepository = TimetableRepository(timetableDao)
        val studyTaskRepository = StudyTaskRepository(studyTaskDao)

        val onboardingRepository = OnboardingRepository(
            courseRepository = courseRepository,
            categoryRepository = categoryRepository
        )

        setContent {
            val isDarkMode by userPreferences.isDarkMode.collectAsState(initial = false)

            NotesManagerTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RequestNotificationPermission { /* handle */ }

                    AppContent(
                        userPreferences = userPreferences,
                        semesterRepository = semesterRepository,
                        courseRepository = courseRepository,
                        categoryRepository = categoryRepository,
                        itemRepository = itemRepository,
                        quickNoteRepository = quickNoteRepository,
                        timetableRepository = timetableRepository,
                        studyTaskRepository = studyTaskRepository,
                        onboardingRepository = onboardingRepository
                    )
                }
            }
        }
    }
}

@Composable
fun AppContent(
    userPreferences: UserPreferences,
    semesterRepository: SemesterRepository,
    courseRepository: CourseRepository,
    categoryRepository: CategoryRepository,
    itemRepository: ItemRepository,
    quickNoteRepository: QuickNoteRepository,
    timetableRepository: TimetableRepository,
    studyTaskRepository: StudyTaskRepository,
    onboardingRepository: OnboardingRepository
) {
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val level = userPreferences.selectedEducationLevel.firstOrNull()
        val semesterId = userPreferences.selectedSemesterId.firstOrNull()

        if (level == null) {
            startDestination = Routes.EDUCATION_LEVEL
        } else {
            when (level) {
                "UNIVERSITY" -> startDestination = Routes.SEMESTER_LIST
                else -> startDestination = if (semesterId != null) {
                    Routes.courseList(semesterId)
                } else {
                    Routes.SEMESTER_LIST
                }
            }
        }
    }

    if (startDestination == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isDarkMode by userPreferences.isDarkMode.collectAsState(initial = false)
    val educationLevel by userPreferences.selectedEducationLevel.collectAsState(initial = null)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                navController = navController,
                isDarkMode = isDarkMode,
                educationLevel = educationLevel,
                onToggleDarkMode = { enabled ->
                    scope.launch {
                        userPreferences.setDarkMode(enabled)
                    }
                },
                onItemClick = {
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    ) {
        val factory = OnboardingViewModel.Factory(
            userPreferences = userPreferences,
            onboardingRepository = onboardingRepository,
            semesterRepository = semesterRepository
        )
        val onboardingViewModel: OnboardingViewModel = viewModel(factory = factory)

        AppNavHost(
            navController = navController,
            viewModel = onboardingViewModel,
            startDestination = startDestination!!,
            onMenuClick = {
                scope.launch {
                    drawerState.open()
                }
            },
            semesterRepository = semesterRepository,
            courseRepository = courseRepository,
            categoryRepository = categoryRepository,
            itemRepository = itemRepository,
            onboardingRepository = onboardingRepository,
            quickNoteRepository = quickNoteRepository,
            timetableRepository = timetableRepository,
            studyTaskRepository = studyTaskRepository,
            userPreferences = userPreferences
        )
    }
}