package com.muneeb.coursemanager.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.entities.TimetableEntry
import com.muneeb.coursemanager.data.repository.TimetableRepository
import com.muneeb.coursemanager.navigation.Routes
import com.muneeb.coursemanager.reminders.ReminderScheduler
import com.muneeb.coursemanager.ui.util.RequestNotificationPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class TimetableListUiState(
    val entries: List<TimetableEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val entryToDelete: TimetableEntry? = null,
    val showResetConfirmation: Boolean = false
)

class TimetableListViewModel(
    private val application: Application,
    private val timetableRepository: TimetableRepository
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(TimetableListUiState())
    val uiState: StateFlow<TimetableListUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
    }

    private fun loadEntries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                timetableRepository.getAll().collect { entries ->
                    _uiState.update { it.copy(entries = entries, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    suspend fun deleteEntry(entry: TimetableEntry) {
        try {
            ReminderScheduler.cancelReminder(application, entry.entryId.toInt())
            timetableRepository.delete(entry)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to delete entry: ${e.message}") }
        }
        _uiState.update { it.copy(entryToDelete = null) }
    }

    suspend fun resetAll() {
        try {
            val entries = _uiState.value.entries
            entries.forEach { entry ->
                ReminderScheduler.cancelReminder(application, entry.entryId.toInt())
            }
            timetableRepository.deleteAll()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to reset timetable: ${e.message}") }
        }
        _uiState.update { it.copy(showResetConfirmation = false) }
    }

    fun setEntryToDelete(entry: TimetableEntry?) {
        _uiState.update { it.copy(entryToDelete = entry) }
    }

    fun setShowResetConfirmation(show: Boolean) {
        _uiState.update { it.copy(showResetConfirmation = show) }
    }

    class Factory(
        private val application: Application,
        private val timetableRepository: TimetableRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TimetableListViewModel::class.java)) {
                return TimetableListViewModel(application, timetableRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableListScreen(
    navController: NavController,
    viewModel: TimetableListViewModel,
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // Permission will be handled by the composable
    }

    val canScheduleExact = ReminderScheduler.canScheduleExactAlarms(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timetable") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setShowResetConfirmation(true) }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Reset Timetable")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.timetableEditor()) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Class") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!canScheduleExact) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exact alarms are disabled — class reminders won't fire on time",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                        Button(
                            onClick = { ReminderScheduler.requestExactAlarmPermission(context) }
                        ) {
                            Text("Fix")
                        }
                    }
                }
            }

            RequestNotificationPermission { /* handle result if needed */ }

            if (uiState.isLoading && uiState.entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null && uiState.entries.isEmpty()) {
                Text(
                    text = "Error: ${uiState.error}",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Red
                )
            } else if (uiState.entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No classes scheduled.\nTap + to add one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                val grouped = uiState.entries.groupBy { it.dayOfWeek }
                val dayNames = mapOf(
                    Calendar.SUNDAY to "Sunday",
                    Calendar.MONDAY to "Monday",
                    Calendar.TUESDAY to "Tuesday",
                    Calendar.WEDNESDAY to "Wednesday",
                    Calendar.THURSDAY to "Thursday",
                    Calendar.FRIDAY to "Friday",
                    Calendar.SATURDAY to "Saturday"
                )

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sortedDays = grouped.keys.sorted()
                    sortedDays.forEach { day ->
                        val entriesForDay = grouped[day] ?: emptyList()
                        item {
                            Text(
                                text = dayNames[day] ?: "Day $day",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF222222),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(entriesForDay) { entry ->
                            TimetableEntryRow(
                                entry = entry,
                                onClick = {
                                    navController.navigate(Routes.timetableEditor(entry.entryId))
                                },
                                onDeleteClick = {
                                    viewModel.setEntryToDelete(entry)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    val entryToDelete = uiState.entryToDelete
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.setEntryToDelete(null) },
            title = { Text("Delete Class") },
            text = { Text("Are you sure you want to delete this class?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.viewModelScope.launch {
                            viewModel.deleteEntry(entryToDelete)
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setEntryToDelete(null) }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowResetConfirmation(false) },
            title = { Text("Reset Timetable") },
            text = { Text("This will delete all timetable entries. Continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.viewModelScope.launch {
                            viewModel.resetAll()
                        }
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowResetConfirmation(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TimetableEntryRow(
    entry: TimetableEntry,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val timeStr = String.format("%02d:%02d - %02d:%02d",
                    entry.startHour, entry.startMinute,
                    entry.endHour, entry.endMinute)
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = entry.subjectName,
                    style = MaterialTheme.typography.bodyLarge
                )
                val details = mutableListOf<String>()
                entry.room?.let { details.add("Room: $it") }
                entry.teacher?.let { details.add("Teacher: $it") }
                if (details.isNotEmpty()) {
                    Text(
                        text = details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}