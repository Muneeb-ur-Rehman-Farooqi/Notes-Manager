package com.muneeb.coursemanager.ui.screens

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.entities.TimetableEntry
import com.muneeb.coursemanager.data.repository.TimetableRepository
import com.muneeb.coursemanager.navigation.Routes
import com.muneeb.coursemanager.reminders.ReminderScheduler
import com.muneeb.coursemanager.ui.components.CountdownTimerText
import com.muneeb.coursemanager.ui.theme.CopyIcon
import com.muneeb.coursemanager.ui.theme.LocalAppStyle
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

private val dayNames: Map<Int, String> = mapOf(
    Calendar.SUNDAY to "Sunday",
    Calendar.MONDAY to "Monday",
    Calendar.TUESDAY to "Tuesday",
    Calendar.WEDNESDAY to "Wednesday",
    Calendar.THURSDAY to "Thursday",
    Calendar.FRIDAY to "Friday",
    Calendar.SATURDAY to "Saturday"
)

private fun format12Hour(hour: Int, minute: Int): String {
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour < 12) "AM" else "PM"
    return String.format("%d:%02d %s", h, minute, amPm)
}

private fun timeRange(entry: TimetableEntry): String =
    "${format12Hour(entry.startHour, entry.startMinute)} – ${format12Hour(entry.endHour, entry.endMinute)}"

private fun entryDetails(entry: TimetableEntry): String {
    val details = mutableListOf<String>()
    entry.room?.takeIf { it.isNotBlank() }?.let { details.add(it) }
    entry.teacher?.takeIf { it.isNotBlank() }?.let { details.add(it) }
    return details.joinToString(" · ")
}

private fun computeNextClass(entries: List<TimetableEntry>): TimetableEntry? {
    if (entries.isEmpty()) return null
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_WEEK)
    val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

    val todayRemaining = entries
        .filter { it.dayOfWeek == today && (it.startHour * 60 + it.startMinute) > nowMinutes }
        .minByOrNull { it.startHour * 60 + it.startMinute }
    if (todayRemaining != null) return todayRemaining

    for (offset in 1..7) {
        val checkDay = ((today - 1 + offset) % 7) + 1
        val dayEntries = entries
            .filter { it.dayOfWeek == checkDay }
            .sortedBy { it.startHour * 60 + it.startMinute }
        if (dayEntries.isNotEmpty()) return dayEntries.first()
    }
    return null
}

private fun openMeetingLink(context: Context, link: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this link", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open this link", Toast.LENGTH_SHORT).show()
    }
}

private fun copyMeetingLink(context: Context, link: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Meeting Link", link))
    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
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
    val style = LocalAppStyle.current
    var expandedWeek by remember { mutableStateOf(false) }
    var entryForSheet by remember { mutableStateOf<TimetableEntry?>(null) }

    val canScheduleExact = ReminderScheduler.canScheduleExactAlarms(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timetable") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.timetableEditor()) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Class") },
                containerColor = style.fabContainerColor,
                contentColor = style.fabContentColor
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (!canScheduleExact) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
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
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { ReminderScheduler.requestExactAlarmPermission(context) }
                        ) {
                            Text("Fix")
                        }
                    }
                }
            }

            RequestNotificationPermission { }

            when {
                uiState.isLoading && uiState.entries.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.entries.isEmpty() -> {
                    Text(
                        text = "Error: ${uiState.error}",
                        modifier = Modifier.padding(16.dp),
                        color = style.errorTextColor
                    )
                }
                uiState.entries.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No classes scheduled.\nTap + to add one.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = style.cardSubtitleColor
                        )
                    }
                }
                else -> {
                    val nextClass = computeNextClass(uiState.entries)
                    nextClass?.let { entry ->
                        NextClassBanner(entry)
                    }

                    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                    val todayName = dayNames[today] ?: "Today"
                    val todayEntries = uiState.entries
                        .filter { it.dayOfWeek == today }
                        .sortedBy { it.startHour * 60 + it.startMinute }

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DayTree(
                            dayName = todayName,
                            entries = todayEntries,
                            onEntryClick = { navController.navigate(Routes.timetableEditor(it.entryId)) },
                            onEntryLongClick = { entryForSheet = it }
                        )

                        TextButton(
                            onClick = { expandedWeek = !expandedWeek },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(if (expandedWeek) "Hide Full Week" else "View Full Week")
                        }

                        if (expandedWeek) {
                            val grouped = uiState.entries.groupBy { it.dayOfWeek }
                            val sortedDays = grouped.keys.sorted()
                            sortedDays.forEach { day ->
                                val dayEntries = grouped[day]
                                    ?.sortedBy { it.startHour * 60 + it.startMinute }
                                    .orEmpty()
                                if (dayEntries.isNotEmpty()) {
                                    DayTree(
                                        dayName = dayNames[day] ?: "Day $day",
                                        entries = dayEntries,
                                        onEntryClick = { navController.navigate(Routes.timetableEditor(it.entryId)) },
                                        onEntryLongClick = { entryForSheet = it }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Reset Timetable",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable { viewModel.setShowResetConfirmation(true) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    val sheetEntry = entryForSheet
    if (sheetEntry != null) {
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { entryForSheet = null },
            sheetState = sheetState,
            dragHandle = {
                IconButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) entryForSheet = null
                    }
                }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close")
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    entryForSheet = null
                                    navController.navigate(Routes.timetableEditor(sheetEntry.entryId))
                                }
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 12.dp))
                    Text("Edit")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    entryForSheet = null
                                    viewModel.setEntryToDelete(sheetEntry)
                                }
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 12.dp))
                    Text("Delete")
                }
                Spacer(modifier = Modifier.height(24.dp))
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
private fun NextClassBanner(entry: TimetableEntry) {
    val context = LocalContext.current
    val style = LocalAppStyle.current
    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val isToday = entry.dayOfWeek == today

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = style.bannerContainerColor,
            contentColor = style.bannerContentColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Next class",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = entry.subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                val details = entryDetails(entry)
                val line = if (details.isNotEmpty()) {
                    "${timeRange(entry)} · $details"
                } else {
                    timeRange(entry)
                }
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
                val link = entry.meetingLink
                if (!link.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "🔗 Join Class",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { openMeetingLink(context, link) }
                                .padding(vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { copyMeetingLink(context, link) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = CopyIcon,
                                contentDescription = "Copy link",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            if (isToday) {
                val targetCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, entry.startHour)
                    set(Calendar.MINUTE, entry.startMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                CountdownTimerText(
                    targetMillis = targetCal.timeInMillis,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun DayTree(
    dayName: String,
    entries: List<TimetableEntry>,
    onEntryClick: (TimetableEntry) -> Unit,
    onEntryLongClick: (TimetableEntry) -> Unit
) {
    val style = LocalAppStyle.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.titleMedium,
            color = style.cardTitleColor,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        if (entries.isEmpty()) {
            Text(
                text = "No classes",
                style = MaterialTheme.typography.bodyMedium,
                color = style.cardSubtitleColor,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 8.dp)
            )
        } else {
            entries.forEach { entry ->
                TimetableBranch(
                    entry = entry,
                    onClick = { onEntryClick(entry) },
                    onLongClick = { onEntryLongClick(entry) }
                )
            }
        }
    }
}

@Composable
private fun TimetableBranch(
    entry: TimetableEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val style = LocalAppStyle.current
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
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(vertical = 8.dp)
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
                    text = entry.subjectName,
                    style = MaterialTheme.typography.titleLarge,
                    color = style.cardTitleColor
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = timeRange(entry),
                style = MaterialTheme.typography.bodySmall,
                color = style.cardSubtitleColor,
                modifier = Modifier.padding(start = 22.dp)
            )
            val details = entryDetails(entry)
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = style.cardSubtitleColor,
                    modifier = Modifier.padding(start = 22.dp, top = 2.dp)
                )
            }
            val link = entry.meetingLink
            if (!link.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 22.dp, top = 2.dp)
                ) {
                    Text(
                        text = "🔗 Join Class",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { openMeetingLink(context, link) }
                            .padding(vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { copyMeetingLink(context, link) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = CopyIcon,
                            contentDescription = "Copy link",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}