package com.muneeb.coursemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.muneeb.coursemanager.data.database.AppDatabase
import com.muneeb.coursemanager.data.preferences.UserPreferences
import com.muneeb.coursemanager.data.repository.CategoryRepository
import com.muneeb.coursemanager.data.repository.CourseRepository
import com.muneeb.coursemanager.data.repository.ItemRepository
import com.muneeb.coursemanager.data.repository.OnboardingRepository
import com.muneeb.coursemanager.data.repository.PageRepository
import com.muneeb.coursemanager.data.repository.PromotionRepository
import com.muneeb.coursemanager.data.repository.PromotionResult
import com.muneeb.coursemanager.data.repository.QuickNoteRepository
import com.muneeb.coursemanager.data.repository.SemesterRepository
import com.muneeb.coursemanager.data.repository.StudyTaskRepository
import com.muneeb.coursemanager.data.repository.TimetableRepository
import com.muneeb.coursemanager.navigation.AppNavHost
import com.muneeb.coursemanager.navigation.Routes
import com.muneeb.coursemanager.reminders.NotificationChannels
import com.muneeb.coursemanager.reminders.ReminderScheduler
import com.muneeb.coursemanager.reminders.StudyTaskReminderReceiver
import com.muneeb.coursemanager.ui.components.AppDrawerContent
import com.muneeb.coursemanager.ui.util.RequestNotificationPermission
import com.muneeb.coursemanager.ui.onboarding.OnboardingViewModel
import com.muneeb.coursemanager.ui.theme.NotesManagerTheme
import com.muneeb.coursemanager.ui.theme.ThemePalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

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
        val pageDao = database.pageDao()
        val quickNoteDao = database.quickNoteDao()
        val timetableDao = database.timetableDao()
        val studyTaskDao = database.studyTaskDao()

        val semesterRepository = SemesterRepository(semesterDao)
        val courseRepository = CourseRepository(courseDao)
        val categoryRepository = CategoryRepository(categoryDao)
        val itemRepository = ItemRepository(itemDao)
        val pageRepository = PageRepository(pageDao)
        val quickNoteRepository = QuickNoteRepository(quickNoteDao)
        val timetableRepository = TimetableRepository(timetableDao)
        val studyTaskRepository = StudyTaskRepository(studyTaskDao)

        val onboardingRepository = OnboardingRepository(
            courseRepository = courseRepository,
            categoryRepository = categoryRepository
        )
        val promotionRepository = PromotionRepository(
            semesterRepository = semesterRepository,
            onboardingRepository = onboardingRepository,
            userPreferences = userPreferences
        )

        // Purge stale Today tasks from previous days
        val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        cleanupScope.launch {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            studyTaskRepository.deleteStaleTodayTasks(todayStart)
        }

        // Re-arm the daily 4 PM ping on every launch
        ReminderScheduler.scheduleExactReminder(
            context = this,
            requestCode = StudyTaskReminderReceiver.DAILY_PING_REQUEST_CODE,
            triggerAtMillis = ReminderScheduler.computeNextDailyPingMillis(),
            channelId = NotificationChannels.CHANNEL_STUDY_REMINDERS,
            title = "To-Do List",
            body = "You have pending tasks",
            isWeeklyRecurring = false,
            receiverClass = StudyTaskReminderReceiver::class.java,
            extraAlarmType = StudyTaskReminderReceiver.ALARM_TYPE_DAILY_PING,
            extraTaskId = null
        )

        setContent {
            val storedPref by userPreferences.isDarkModeOrNull.collectAsState(initial = null)
            val systemDark = isSystemInDarkTheme()
            val isDarkMode = storedPref ?: systemDark

            val paletteString by userPreferences.selectedPalette.collectAsState(initial = "MONOCHROME")
            val palette = try {
                ThemePalette.valueOf(paletteString)
            } catch (_: Exception) {
                ThemePalette.MONOCHROME
            }

            NotesManagerTheme(palette = palette, darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RequestNotificationPermission { }

                    AppContent(
                        userPreferences = userPreferences,
                        isDarkMode = isDarkMode,
                        selectedPalette = palette,
                        semesterRepository = semesterRepository,
                        courseRepository = courseRepository,
                        categoryRepository = categoryRepository,
                        itemRepository = itemRepository,
                        pageRepository = pageRepository,
                        quickNoteRepository = quickNoteRepository,
                        timetableRepository = timetableRepository,
                        studyTaskRepository = studyTaskRepository,
                        onboardingRepository = onboardingRepository,
                        promotionRepository = promotionRepository
                    )
                }
            }
        }
    }
}

@Composable
fun AppContent(
    userPreferences: UserPreferences,
    isDarkMode: Boolean,
    selectedPalette: ThemePalette,
    semesterRepository: SemesterRepository,
    courseRepository: CourseRepository,
    categoryRepository: CategoryRepository,
    itemRepository: ItemRepository,
    pageRepository: PageRepository,
    quickNoteRepository: QuickNoteRepository,
    timetableRepository: TimetableRepository,
    studyTaskRepository: StudyTaskRepository,
    onboardingRepository: OnboardingRepository,
    promotionRepository: PromotionRepository
) {
    var startDestination by remember { mutableStateOf<String?>(null) }
    var postNameEntryDestination by remember { mutableStateOf(Routes.EDUCATION_LEVEL) }

    LaunchedEffect(Unit) {
        val name = userPreferences.userName.firstOrNull()
        val level = userPreferences.selectedEducationLevel.firstOrNull()
        val semesterId = userPreferences.selectedSemesterId.firstOrNull()

        val realDestination = if (level == null) {
            Routes.EDUCATION_LEVEL
        } else {
            when (level) {
                "UNIVERSITY" -> Routes.SEMESTER_LIST
                else -> if (semesterId != null) Routes.courseList(semesterId) else Routes.SEMESTER_LIST
            }
        }

        postNameEntryDestination = realDestination
        startDestination = if (name == null) Routes.NAME_ENTRY else realDestination
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
    val educationLevel by userPreferences.selectedEducationLevel.collectAsState(initial = null)
    val partGrade by userPreferences.selectedPartGrade.collectAsState(initial = null)
    val universityMajor by userPreferences.selectedUniversityMajor.collectAsState(initial = null)
    val currentSemesterId by userPreferences.selectedSemesterId.collectAsState(initial = null)
    val userName by userPreferences.userName.collectAsState(initial = null)

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val factory = OnboardingViewModel.Factory(
        userPreferences = userPreferences,
        onboardingRepository = onboardingRepository,
        semesterRepository = semesterRepository
    )
    val onboardingViewModel: OnboardingViewModel = viewModel(factory = factory)

    var showPromotionWarning by remember { mutableStateOf(false) }
    var showPromotionSuccess by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute != Routes.PDF_VIEWER,
        drawerContent = {
            AppDrawerContent(
                navController = navController,
                isDarkMode = isDarkMode,
                educationLevel = educationLevel,
                partGrade = partGrade,
                universityMajor = universityMajor,
                currentSemesterId = currentSemesterId,
                userName = userName,
                selectedPalette = selectedPalette,
                onPaletteSelected = { newPalette ->
                    scope.launch {
                        userPreferences.setSelectedPalette(newPalette.name)
                    }
                },
                onToggleDarkMode = { enabled ->
                    scope.launch {
                        userPreferences.setDarkMode(enabled)
                    }
                },
                onItemClick = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                onPromoteClick = {
                    scope.launch {
                        drawerState.close()
                    }
                    showPromotionWarning = true
                }
            )
        }
    ) {
        AppNavHost(
            navController = navController,
            viewModel = onboardingViewModel,
            startDestination = startDestination!!,
            postNameEntryDestination = postNameEntryDestination,
            onMenuClick = {
                scope.launch {
                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                }
            },
            isDarkMode = isDarkMode,
            semesterRepository = semesterRepository,
            courseRepository = courseRepository,
            categoryRepository = categoryRepository,
            itemRepository = itemRepository,
            pageRepository = pageRepository,
            onboardingRepository = onboardingRepository,
            quickNoteRepository = quickNoteRepository,
            timetableRepository = timetableRepository,
            studyTaskRepository = studyTaskRepository,
            userPreferences = userPreferences
        )
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }

    if (showPromotionWarning) {
        AlertDialog(
            onDismissRequest = { showPromotionWarning = false },
            title = { Text("Promote to Next Level") },
            text = {
                Text("This moves you to the next level. Your current folders and files are kept as history and stay accessible.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPromotionWarning = false
                        scope.launch {
                            val currentId = userPreferences.selectedSemesterId.firstOrNull()
                            if (currentId == null) return@launch
                            when (val result = promotionRepository.promote(currentId)) {
                                is PromotionResult.SameLevelPromoted -> {
                                    userPreferences.setSelectedSemesterId(result.newSemesterId)
                                    userPreferences.setSelectedPartGrade(result.newPart)
                                    showPromotionSuccess = true
                                }
                                is PromotionResult.NeedsGroupSelection -> {
                                    onboardingViewModel.resetForNewFlow()
                                    onboardingViewModel.setEducationLevel(result.newLevel)
                                    onboardingViewModel.setPartGrade(result.newPart)
                                    navController.navigate(Routes.GROUP_SELECTION) {
                                        popUpTo(Routes.SEMESTER_LIST) { inclusive = false }
                                    }
                                }
                                PromotionResult.PromotedToUniversity -> {
                                    onboardingViewModel.resetForNewFlow()
                                    onboardingViewModel.setEducationLevel("UNIVERSITY")
                                    navController.navigate(Routes.UNIVERSITY_MAJOR)
                                }
                                PromotionResult.NotPromotable -> {
                                    // no-op
                                }
                            }
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromotionWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPromotionSuccess) {
        AlertDialog(
            onDismissRequest = { showPromotionSuccess = false },
            title = { Text("Congratulations") },
            text = { Text("Congratulations! You've been promoted.") },
            confirmButton = {
                TextButton(onClick = { showPromotionSuccess = false }) {
                    Text("OK")
                }
            }
        )
    }
}