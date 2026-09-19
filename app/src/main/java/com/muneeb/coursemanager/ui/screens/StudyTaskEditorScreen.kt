package com.muneeb.coursemanager.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.entities.StudyTask
import com.muneeb.coursemanager.data.repository.StudyTaskRepository
import com.muneeb.coursemanager.reminders.NotificationChannels
import com.muneeb.coursemanager.reminders.ReminderScheduler
import com.muneeb.coursemanager.reminders.StudyTaskReminderReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StudyTaskEditorUiState(
    val isLoading: Boolean = false,
    val isNewTask: Boolean = true,
    val title: String = "",
    val isTodayTask: Boolean = true,
    val deadlineDateMillis: Long? = null,
    val hasSpecificTime: Boolean = false,
    val deadlineHour: Int = 9,
    val deadlineMinute: Int = 0,
    val error: String? = null,
    val isSaving: Boolean = false
)

class StudyTaskEditorViewModel(
    private val application: Application,
    private val studyTaskRepository: StudyTaskRepository,
    private val taskId: Long
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StudyTaskEditorUiState())
    val uiState: StateFlow<StudyTaskEditorUiState> = _uiState.asStateFlow()

    init {
        if (taskId != -1L) {
            loadTask()
        }
    }

    private fun loadTask() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val task = studyTaskRepository.getById(taskId)
                if (task != null) {
                    val hasTime = task.deadlineHour != null && task.deadlineMinute != null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNewTask = false,
                            title = task.title,
                            isTodayTask = task.isTodayTask,
                            deadlineDateMillis = task.deadlineDateMillis,
                            hasSpecificTime = hasTime,
                            deadlineHour = task.deadlineHour ?: 9,
                            deadlineMinute = task.deadlineMinute ?: 0
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "Task not found", isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun setTodayMode() {
        _uiState.update { it.copy(isTodayTask = true, error = null) }
    }

    fun setDeadlineMode() {
        _uiState.update { it.copy(isTodayTask = false, error = null) }
    }

    fun updateDeadlineDate(dateMillis: Long) {
        _uiState.update { it.copy(deadlineDateMillis = dateMillis, error = null) }
    }

    fun toggleSpecificTime() {
        _uiState.update { state ->
            state.copy(hasSpecificTime = !state.hasSpecificTime)
        }
    }

    fun updateDeadlineHour(hour: Int) {
        _uiState.update { it.copy(deadlineHour = hour) }
    }

    fun updateDeadlineMinute(minute: Int) {
        _uiState.update { it.copy(deadlineMinute = minute) }
    }

    suspend fun saveTask(): Boolean {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Title is required") }
            return false
        }
        if (!state.isTodayTask && state.deadlineDateMillis == null) {
            _uiState.update { it.copy(error = "Please pick a deadline date") }
            return false
        }
        _uiState.update { it.copy(isSaving = true, error = null) }
        try {
            // Cancel any stale escalation alarm before re-inserting
            if (!state.isNewTask) {
                ReminderScheduler.cancelReminder(
                    context = application,
                    requestCode = taskId.toInt(),
                    receiverClass = StudyTaskReminderReceiver::class.java
                )
            }

            val task = StudyTask(
                taskId = if (state.isNewTask) 0 else taskId,
                title = state.title,
                isTodayTask = state.isTodayTask,
                createdDateMillis = System.currentTimeMillis(),
                deadlineDateMillis = if (state.isTodayTask) null else state.deadlineDateMillis,
                deadlineHour = if (!state.isTodayTask && state.hasSpecificTime) state.deadlineHour else null,
                deadlineMinute = if (!state.isTodayTask && state.hasSpecificTime) state.deadlineMinute else null,
                isCompleted = false
            )
            val newId = if (state.isNewTask) {
                studyTaskRepository.insert(task)
            } else {
                studyTaskRepository.update(task)
                taskId
            }

            // Schedule escalation for deadline tasks
            if (!state.isTodayTask && state.deadlineDateMillis != null) {
                val effective = ReminderScheduler.computeEffectiveDeadlineMillis(
                    dateMillis = state.deadlineDateMillis,
                    hour = if (state.hasSpecificTime) state.deadlineHour else null,
                    minute = if (state.hasSpecificTime) state.deadlineMinute else null
                )
                val trigger = ReminderScheduler.firstEscalationTriggerMillis(effective)
                if (trigger != null) {
                    ReminderScheduler.scheduleExactReminder(
                        context = application,
                        requestCode = newId.toInt(),
                        triggerAtMillis = trigger,
                        channelId = NotificationChannels.CHANNEL_STUDY_REMINDERS,
                        title = state.title,
                        body = "Due soon",
                        isWeeklyRecurring = false,
                        receiverClass = StudyTaskReminderReceiver::class.java,
                        extraAlarmType = StudyTaskReminderReceiver.ALARM_TYPE_ESCALATION,
                        extraTaskId = newId
                    )
                }
            }

            _uiState.update { it.copy(isSaving = false) }
            return true
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message, isSaving = false) }
            return false
        }
    }

    class Factory(
        private val application: Application,
        private val studyTaskRepository: StudyTaskRepository,
        private val taskId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StudyTaskEditorViewModel::class.java)) {
                return StudyTaskEditorViewModel(application, studyTaskRepository, taskId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTaskEditorScreen(
    navController: NavController,
    viewModel: StudyTaskEditorViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.deadlineDateMillis ?: System.currentTimeMillis()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.deadlineHour,
        initialMinute = uiState.deadlineMinute,
        is24Hour = false
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewTask) "Add Task" else "Edit Task") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = uiState.isTodayTask,
                        onClick = { viewModel.setTodayMode() },
                        label = { Text("Today") }
                    )
                    FilterChip(
                        selected = !uiState.isTodayTask,
                        onClick = { viewModel.setDeadlineMode() },
                        label = { Text("Has a deadline") }
                    )
                }

                if (!uiState.isTodayTask) {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.deadlineDateMillis?.let { formatDate(it) } ?: "",
                        onValueChange = { },
                        label = { Text("Deadline date") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = { showDatePicker = true }) { Text("📅") }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Switch(
                            checked = uiState.hasSpecificTime,
                            onCheckedChange = { viewModel.toggleSpecificTime() }
                        )
                        Text(
                            text = "Set a specific time",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    if (uiState.hasSpecificTime) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = format12Hour(uiState.deadlineHour, uiState.deadlineMinute),
                            onValueChange = { },
                            label = { Text("Deadline time") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                TextButton(onClick = { showTimePicker = true }) { Text("🕐") }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.error != null) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Button(
                        onClick = {
                            viewModel.viewModelScope.launch {
                                if (viewModel.saveTask()) {
                                    navController.popBackStack()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.updateDeadlineDate(it)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateDeadlineHour(timePickerState.hour)
                        viewModel.updateDeadlineMinute(timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatDate(dateMillis: Long): String {
    val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(dateMillis))
}

private fun format12Hour(hour: Int, minute: Int): String {
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour < 12) "AM" else "PM"
    return String.format("%d:%02d %s", displayHour, minute, amPm)
}