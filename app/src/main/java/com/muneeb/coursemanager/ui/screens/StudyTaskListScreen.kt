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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.entities.StudyTask
import com.muneeb.coursemanager.data.repository.StudyTaskRepository
import com.muneeb.coursemanager.navigation.Routes
import com.muneeb.coursemanager.reminders.ReminderScheduler
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

data class StudyTaskListUiState(
    val tasks: List<StudyTask> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val taskToDelete: StudyTask? = null
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
        if (updated.isCompleted && task.hasReminder) {
            ReminderScheduler.cancelReminder(application, task.taskId.toInt())
        }
        studyTaskRepository.update(updated)
    }

    suspend fun deleteTask(task: StudyTask) {
        if (task.hasReminder) {
            ReminderScheduler.cancelReminder(application, task.taskId.toInt())
        }
        studyTaskRepository.delete(task)
        _uiState.update { it.copy(taskToDelete = null) }
    }

    fun setTaskToDelete(task: StudyTask?) {
        _uiState.update { it.copy(taskToDelete = task) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTaskListScreen(
    navController: NavController,
    viewModel: StudyTaskListViewModel,
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    RequestNotificationPermission { }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Tasks") },
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
                uiState.tasks.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No study tasks.\nTap + to add one.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    val incompleteTasks = uiState.tasks.filter { !it.isCompleted }
                    if (incompleteTasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All caught up!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val grouped = incompleteTasks.groupBy { it.dateMillis }
                        val sortedDates = grouped.keys.sorted()

                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            sortedDates.forEach { dateMillis ->
                                val tasksForDate = grouped[dateMillis].orEmpty()
                                if (tasksForDate.isNotEmpty()) {
                                    DateTree(
                                        dateMillis = dateMillis,
                                        tasks = tasksForDate,
                                        onToggleComplete = { task ->
                                            viewModel.viewModelScope.launch {
                                                viewModel.toggleCompleted(task)
                                            }
                                        },
                                        onDeleteClick = { viewModel.setTaskToDelete(it) },
                                        onEntryClick = {
                                            navController.navigate(Routes.studyTaskEditor(it.taskId))
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

    if (uiState.taskToDelete != null) {
        val task = uiState.taskToDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.setTaskToDelete(null) },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.viewModelScope.launch {
                            viewModel.deleteTask(task)
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setTaskToDelete(null) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DateTree(
    dateMillis: Long,
    tasks: List<StudyTask>,
    onToggleComplete: (StudyTask) -> Unit,
    onDeleteClick: (StudyTask) -> Unit,
    onEntryClick: (StudyTask) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = formatDateHeader(dateMillis),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        tasks.forEach { task ->
            StudyTaskBranch(
                task = task,
                onToggleComplete = { onToggleComplete(task) },
                onDeleteClick = { onDeleteClick(task) },
                onClick = { onEntryClick(task) }
            )
        }
    }
}

@Composable
private fun StudyTaskBranch(
    task: StudyTask,
    onToggleComplete: () -> Unit,
    onDeleteClick: () -> Unit,
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
                .padding(vertical = 4.dp)
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
                Spacer(modifier = Modifier.width(4.dp))
                Checkbox(
                    checked = false,
                    onCheckedChange = { onToggleComplete() }
                )
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            if (task.hasReminder && task.reminderHour != null && task.reminderMinute != null) {
                Text(
                    text = "⏰ ${format12Hour(task.reminderHour, task.reminderMinute)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 60.dp, top = 2.dp)
                )
            }
        }
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
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

private fun format12Hour(hour: Int, minute: Int): String {
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour < 12) "AM" else "PM"
    return String.format("%d:%02d %s", displayHour, minute, amPm)
}