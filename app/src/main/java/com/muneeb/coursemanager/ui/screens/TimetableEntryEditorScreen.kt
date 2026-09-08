package com.muneeb.coursemanager.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.entities.TimetableEntry
import com.muneeb.coursemanager.data.repository.TimetableRepository
import com.muneeb.coursemanager.reminders.NotificationChannels
import com.muneeb.coursemanager.reminders.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class TimetableEditorUiState(
    val isLoading: Boolean = false,
    val isNewEntry: Boolean = true,
    val dayOfWeek: Int = Calendar.MONDAY,
    val selectedDays: Set<Int> = emptySet(),
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 10,
    val endMinute: Int = 0,
    val subjectName: String = "",
    val room: String = "",
    val teacher: String = "",
    val error: String? = null,
    val isSaving: Boolean = false
)

class TimetableEditorViewModel(
    private val application: Application,
    private val timetableRepository: TimetableRepository,
    private val entryId: Long
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(TimetableEditorUiState())
    val uiState: StateFlow<TimetableEditorUiState> = _uiState.asStateFlow()

    init {
        if (entryId != -1L) {
            loadEntry()
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                timetableRepository.getById(entryId).collect { entry ->
                    if (entry != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isNewEntry = false,
                                dayOfWeek = entry.dayOfWeek,
                                startHour = entry.startHour,
                                startMinute = entry.startMinute,
                                endHour = entry.endHour,
                                endMinute = entry.endMinute,
                                subjectName = entry.subjectName,
                                room = entry.room ?: "",
                                teacher = entry.teacher ?: ""
                            )
                        }
                    } else {
                        _uiState.update { it.copy(error = "Entry not found", isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun updateDayOfWeek(day: Int) {
        _uiState.update { it.copy(dayOfWeek = day) }
    }

    fun toggleDaySelection(day: Int) {
        _uiState.update { state ->
            val newSet = if (state.selectedDays.contains(day)) {
                state.selectedDays - day
            } else {
                state.selectedDays + day
            }
            state.copy(selectedDays = newSet)
        }
    }

    fun updateStartHour(hour: Int) {
        _uiState.update { it.copy(startHour = hour) }
    }

    fun updateStartMinute(minute: Int) {
        _uiState.update { it.copy(startMinute = minute) }
    }

    fun updateEndHour(hour: Int) {
        _uiState.update { it.copy(endHour = hour) }
    }

    fun updateEndMinute(minute: Int) {
        _uiState.update { it.copy(endMinute = minute) }
    }

    fun updateSubjectName(name: String) {
        _uiState.update { it.copy(subjectName = name) }
    }

    fun updateRoom(room: String) {
        _uiState.update { it.copy(room = room) }
    }

    fun updateTeacher(teacher: String) {
        _uiState.update { it.copy(teacher = teacher) }
    }

    suspend fun saveEntry(): Boolean {
        val state = _uiState.value
        if (state.subjectName.isBlank()) {
            _uiState.update { it.copy(error = "Subject name is required") }
            return false
        }
        val startTotal = state.startHour * 60 + state.startMinute
        val endTotal = state.endHour * 60 + state.endMinute
        if (endTotal <= startTotal) {
            _uiState.update { it.copy(error = "End time must be after start time") }
            return false
        }

        _uiState.update { it.copy(isSaving = true, error = null) }
        try {
            if (state.isNewEntry) {
                val selectedDays = state.selectedDays
                if (selectedDays.isEmpty()) {
                    _uiState.update { it.copy(error = "Select at least one day", isSaving = false) }
                    return false
                }
                for (day in selectedDays) {
                    val entry = TimetableEntry(
                        entryId = 0,
                        dayOfWeek = day,
                        startHour = state.startHour,
                        startMinute = state.startMinute,
                        endHour = state.endHour,
                        endMinute = state.endMinute,
                        subjectName = state.subjectName,
                        room = state.room.takeIf { it.isNotBlank() },
                        teacher = state.teacher.takeIf { it.isNotBlank() }
                    )
                    val newId = timetableRepository.insert(entry)
                    scheduleReminder(newId, entry)
                }
            } else {
                // Cancel old alarm first
                ReminderScheduler.cancelReminder(application, entryId.toInt())
                val entry = TimetableEntry(
                    entryId = entryId,
                    dayOfWeek = state.dayOfWeek,
                    startHour = state.startHour,
                    startMinute = state.startMinute,
                    endHour = state.endHour,
                    endMinute = state.endMinute,
                    subjectName = state.subjectName,
                    room = state.room.takeIf { it.isNotBlank() },
                    teacher = state.teacher.takeIf { it.isNotBlank() }
                )
                timetableRepository.update(entry)
                scheduleReminder(entryId, entry)
            }
            _uiState.update { it.copy(isSaving = false) }
            return true
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message, isSaving = false) }
            return false
        }
    }

    private fun scheduleReminder(entryId: Long, entry: TimetableEntry) {
        val context = application
        val triggerMillis = ReminderScheduler.computeNextTriggerMillis(
            dayOfWeek = entry.dayOfWeek,
            hour = entry.startHour,
            minute = entry.startMinute,
            minutesBefore = 5
        )
        val title = entry.subjectName
        val body = buildString {
            if (entry.room != null && entry.teacher != null) {
                append("${entry.room} · ${entry.teacher}")
            } else if (entry.room != null) {
                append("Room ${entry.room}")
            } else if (entry.teacher != null) {
                append("Teacher ${entry.teacher}")
            } else {
                append("Class starting soon")
            }
        }
        ReminderScheduler.scheduleExactReminder(
            context = context,
            requestCode = entryId.toInt(),
            triggerAtMillis = triggerMillis,
            channelId = NotificationChannels.CHANNEL_CLASS_REMINDERS,
            title = title,
            body = body,
            isWeeklyRecurring = true
        )
    }

    class Factory(
        private val application: Application,
        private val timetableRepository: TimetableRepository,
        private val entryId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TimetableEditorViewModel::class.java)) {
                return TimetableEditorViewModel(application, timetableRepository, entryId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

private fun format12Hour(hour: Int, minute: Int): String {
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour < 12) "AM" else "PM"
    return String.format("%d:%02d %s", displayHour, minute, amPm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableEntryEditorScreen(
    navController: NavController,
    viewModel: TimetableEditorViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val startTimeState = rememberTimePickerState(
        initialHour = uiState.startHour,
        initialMinute = uiState.startMinute,
        is24Hour = false
    )
    val endTimeState = rememberTimePickerState(
        initialHour = uiState.endHour,
        initialMinute = uiState.endMinute,
        is24Hour = false
    )

    val isValid = uiState.subjectName.isNotBlank() &&
            (uiState.endHour * 60 + uiState.endMinute) > (uiState.startHour * 60 + uiState.startMinute) &&
            (if (uiState.isNewEntry) uiState.selectedDays.isNotEmpty() else true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewEntry) "Add Class" else "Edit Class") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                // Day selection: multi-day checkboxes for new, dropdown for edit
                val dayNames = listOf(
                    "Monday" to Calendar.MONDAY,
                    "Tuesday" to Calendar.TUESDAY,
                    "Wednesday" to Calendar.WEDNESDAY,
                    "Thursday" to Calendar.THURSDAY,
                    "Friday" to Calendar.FRIDAY,
                    "Saturday" to Calendar.SATURDAY,
                    "Sunday" to Calendar.SUNDAY
                )

                if (uiState.isNewEntry) {
                    Text("Select days", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                    dayNames.forEach { (name, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = uiState.selectedDays.contains(value),
                                onCheckedChange = { viewModel.toggleDaySelection(value) }
                            )
                            Text(name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    // Existing single-day dropdown
                    var dayDropdownExpanded by remember { mutableStateOf(false) }
                    val currentDayName = dayNames.find { it.second == uiState.dayOfWeek }?.first ?: "Select Day"
                    OutlinedTextField(
                        value = currentDayName,
                        onValueChange = { },
                        label = { Text("Day") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = { dayDropdownExpanded = true }) { Text("▼") }
                        }
                    )
                    DropdownMenu(
                        expanded = dayDropdownExpanded,
                        onDismissRequest = { dayDropdownExpanded = false }
                    ) {
                        dayNames.forEach { (name, value) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    viewModel.updateDayOfWeek(value)
                                    dayDropdownExpanded = false
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Start time picker
                OutlinedTextField(
                    value = format12Hour(uiState.startHour, uiState.startMinute),
                    onValueChange = { },
                    label = { Text("Start Time") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { showStartPicker = true }) { Text("🕐") }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // End time picker
                OutlinedTextField(
                    value = format12Hour(uiState.endHour, uiState.endMinute),
                    onValueChange = { },
                    label = { Text("End Time") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { showEndPicker = true }) { Text("🕐") }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = uiState.subjectName,
                    onValueChange = { viewModel.updateSubjectName(it) },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.room,
                    onValueChange = { viewModel.updateRoom(it) },
                    label = { Text("Room (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.teacher,
                    onValueChange = { viewModel.updateTeacher(it) },
                    label = { Text("Teacher (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.error != null) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Button(
                        onClick = {
                            viewModel.viewModelScope.launch {
                                if (viewModel.saveEntry()) {
                                    navController.popBackStack()
                                }
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    // Start Time Picker Dialog
    if (showStartPicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showStartPicker = false },
            title = { Text("Select Start Time") },
            text = {
                TimePicker(
                    state = startTimeState,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateStartHour(startTimeState.hour)
                        viewModel.updateStartMinute(startTimeState.minute)
                        showStartPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // End Time Picker Dialog
    if (showEndPicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEndPicker = false },
            title = { Text("Select End Time") },
            text = {
                TimePicker(
                    state = endTimeState,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateEndHour(endTimeState.hour)
                        viewModel.updateEndMinute(endTimeState.minute)
                        showEndPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}