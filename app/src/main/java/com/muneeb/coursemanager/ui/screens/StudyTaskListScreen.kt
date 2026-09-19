package com.muneeb.coursemanager.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.entities.StudyTask
import com.muneeb.coursemanager.data.repository.StudyTaskRepository
import com.muneeb.coursemanager.navigation.Routes
import com.muneeb.coursemanager.reminders.ReminderScheduler
import com.muneeb.coursemanager.reminders.StudyTaskReminderReceiver
import com.muneeb.coursemanager.ui.components.CountdownTimerText
import com.muneeb.coursemanager.ui.util.RequestNotificationPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class StudyTaskListUiState(
    val tasks: List<StudyTask> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class StudyTaskListViewModel(
    private val application: Application,
    private val studyTaskRepository: StudyTaskRepository
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(StudyTaskListUiState())
    val uiState: StateFlow<StudyTaskListUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                studyTaskRepository.getAll().collect { tasks ->
                    _uiState.update { it.copy(tasks = tasks, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    suspend fun toggleCompleted(task: StudyTask) {
        val updated = task.copy(isCompleted = !task.isCompleted)
        if (updated.isCompleted) {
            ReminderScheduler.cancelReminder(
                context = application,
                requestCode = task.taskId.toInt(),
                receiverClass = StudyTaskReminderReceiver::class.java
            )
        }
        studyTaskRepository.update(updated)
    }

    class Factory(
        private val application: Application,
        private val studyTaskRepository: StudyTaskRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StudyTaskListViewModel::class.java)) {
                return StudyTaskListViewModel(application, studyTaskRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

private fun formatDateHeader(dateMillis: Long): String {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val tomorrow = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis + (24 * 60 * 60 * 1000L)
    }
    val taskDate = Calendar.getInstance().apply { timeInMillis = dateMillis }

    return when {
        taskDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                taskDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
        taskDate.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                taskDate.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> "Tomorrow"
        else -> {
            val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            sdf.format(Date(dateMillis))
        }
    }
}

private fun deadlineEpochMillis(task: StudyTask): Long? {
    val date = task.deadlineDateMillis ?: return null
    val cal = Calendar.getInstance().apply {
        timeInMillis = date
        set(Calendar.HOUR_OF_DAY, task.deadlineHour ?: 23)
        set(Calendar.MINUTE, task.deadlineMinute ?: 59)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun isOverdue(task: StudyTask, nowMillis: Long): Boolean {
    val deadline = deadlineEpochMillis(task) ?: return false
    return deadline < nowMillis
}

private fun timeLeftBadge(task: StudyTask, nowMillis: Long): String? {
    val deadline = deadlineEpochMillis(task) ?: return null
    val diff = deadline - nowMillis
    return if (diff < 0) {
        val absDiff = abs(diff)
        val days = absDiff / (24L * 60 * 60 * 1000)
        val hours = (absDiff % (24L * 60 * 60 * 1000)) / (60L * 60 * 1000)
        val minutes = (absDiff % (60L * 60 * 1000)) / (60L * 1000)
        when {
            days > 0 -> "Overdue by ${days}d ${hours}h"
            hours > 0 -> "Overdue by ${hours}h ${minutes}m"
            else -> "Overdue by ${minutes}m"
        }
    } else {
        val days = diff / (24L * 60 * 60 * 1000)
        val hours = (diff % (24L * 60 * 60 * 1000)) / (60L * 60 * 1000)
        val minutes = (diff % (60L * 60 * 1000)) / (60L * 1000)
        when {
            days > 0 -> "${days}d ${hours}h left"
            hours > 0 -> "${hours}h ${minutes}m left"
            else -> "${minutes}m left"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTaskListScreen(
    navController: NavController,
    viewModel: StudyTaskListViewModel,
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    RequestNotificationPermission { }

    val incompleteTasks = uiState.tasks.filter { !it.isCompleted }
    val nowMillis = System.currentTimeMillis()

    val overdueTasks = incompleteTasks.filter { !it.isTodayTask && isOverdue(it, nowMillis) }

    val upcomingDeadlineTasks = incompleteTasks
        .filter { !it.isTodayTask && !isOverdue(it, nowMillis) && it.deadlineDateMillis != null }
        .sortedBy { deadlineEpochMillis(it) }
    val nextFlagTask = upcomingDeadlineTasks.firstOrNull()

    val todayTasks = incompleteTasks.filter { it.isTodayTask }
    val remainingDeadlineTasks = upcomingDeadlineTasks

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("To-Do List") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.studyTaskEditor()) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Task") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                uiState.isLoading && uiState.tasks.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.tasks.isEmpty() -> {
                    Text(
                        text = "Error: ${uiState.error}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                incompleteTasks.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.tasks.isEmpty()) {
                                "No tasks yet.\nTap + to add one."
                            } else {
                                "All caught up!"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    nextFlagTask?.let { task ->
                        DueTaskBanner(
                            task = task,
                            onClick = { navController.navigate(Routes.studyTaskEditor(task.taskId)) }
                        )
                    }

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        if (overdueTasks.isNotEmpty()) {
                            TaskTree(
                                trunkLabel = "Overdue",
                                trunkColor = MaterialTheme.colorScheme.error,
                                tasks = overdueTasks,
                                nowMillis = nowMillis,
                                onToggle = { task ->
                                    viewModel.viewModelScope.launch {
                                        viewModel.toggleCompleted(task)
                                    }
                                },
                                onClick = { task ->
                                    navController.navigate(Routes.studyTaskEditor(task.taskId))
                                }
                            )
                        }

                        if (todayTasks.isNotEmpty()) {
                            TaskTree(
                                trunkLabel = "Today",
                                trunkColor = MaterialTheme.colorScheme.onBackground,
                                tasks = todayTasks,
                                nowMillis = nowMillis,
                                onToggle = { task ->
                                    viewModel.viewModelScope.launch {
                                        viewModel.toggleCompleted(task)
                                    }
                                },
                                onClick = { task ->
                                    navController.navigate(Routes.studyTaskEditor(task.taskId))
                                }
                            )
                        }

                        val grouped = remainingDeadlineTasks
                            .filter { it.deadlineDateMillis != null }
                            .groupBy { it.deadlineDateMillis!! }
                        val sortedDates = grouped.keys.sorted()
                        sortedDates.forEach { date ->
                            val tasksForDate = grouped[date].orEmpty()
                            if (tasksForDate.isNotEmpty()) {
                                TaskTree(
                                    trunkLabel = formatDateHeader(date),
                                    trunkColor = MaterialTheme.colorScheme.onBackground,
                                    tasks = tasksForDate,
                                    nowMillis = nowMillis,
                                    onToggle = { task ->
                                        viewModel.viewModelScope.launch {
                                            viewModel.toggleCompleted(task)
                                        }
                                    },
                                    onClick = { task ->
                                        navController.navigate(Routes.studyTaskEditor(task.taskId))
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DueTaskBanner(
    task: StudyTask,
    onClick: () -> Unit
) {
    val targetMillis = deadlineEpochMillis(task)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            Text(
                text = "Due Task",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (targetMillis != null) {
                CountdownTimerText(
                    targetMillis = targetMillis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TaskTree(
    trunkLabel: String,
    trunkColor: androidx.compose.ui.graphics.Color,
    tasks: List<StudyTask>,
    nowMillis: Long,
    onToggle: (StudyTask) -> Unit,
    onClick: (StudyTask) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = trunkLabel,
            style = MaterialTheme.typography.titleMedium,
            color = trunkColor,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        tasks.forEach { task ->
            TaskBranch(
                task = task,
                nowMillis = nowMillis,
                onToggle = { onToggle(task) },
                onClick = { onClick(task) }
            )
        }
    }
}

@Composable
private fun TaskBranch(
    task: StudyTask,
    nowMillis: Long,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() }
                .padding(vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Checkbox(
                    checked = false,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.size(28.dp)
                )
            }
            val badge = timeLeftBadge(task, nowMillis)
            if (badge != null && !task.isTodayTask) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                )
            }
        }
    }
}