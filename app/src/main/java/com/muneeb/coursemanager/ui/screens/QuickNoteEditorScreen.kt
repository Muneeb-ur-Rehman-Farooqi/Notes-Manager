package com.muneeb.coursemanager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.entities.QuickNote
import com.muneeb.coursemanager.data.repository.QuickNoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickNoteEditorUiState(
    val isLoading: Boolean = false,
    val initialTitle: String = "",
    val initialContent: String = "",
    val isSaving: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null,
    val isNewNote: Boolean = true
)

class QuickNoteEditorViewModel(
    private val quickNoteRepository: QuickNoteRepository,
    private val noteId: Long
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        QuickNoteEditorUiState(
            isLoading = noteId != -1L,
            isNewNote = noteId == -1L
        )
    )
    val uiState: StateFlow<QuickNoteEditorUiState> = _uiState.asStateFlow()

    init {
        if (noteId != -1L) {
            loadNote()
        }
    }

    private fun loadNote() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            try {
                // Single snapshot load avoids overwriting user edits on background DB emissions
                val note = quickNoteRepository.getById(noteId).first()
                if (note != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            initialTitle = note.title ?: "",
                            initialContent = note.content,
                            isNewNote = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(loadError = "Note not found", isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loadError = e.message ?: "Failed to load note", isLoading = false) }
            }
        }
    }

    suspend fun saveNote(title: String, content: String): Boolean {
        if (_uiState.value.isSaving) return false

        if (content.isBlank()) {
            _uiState.update { it.copy(saveError = "Content cannot be empty") }
            return false
        }

        _uiState.update { it.copy(isSaving = true, saveError = null) }
        try {
            val normalizedTitle = title.trim().takeIf { it.isNotBlank() }
            if (_uiState.value.isNewNote) {
                val now = System.currentTimeMillis()
                val note = QuickNote(
                    noteId = 0,
                    title = normalizedTitle,
                    content = content,
                    createdAt = now,
                    updatedAt = now
                )
                quickNoteRepository.insert(note)
            } else {
                val existing = quickNoteRepository.getById(noteId).first()
                if (existing == null) {
                    _uiState.update { it.copy(saveError = "Note not found", isSaving = false) }
                    return false
                }
                quickNoteRepository.update(
                    existing.copy(
                        title = normalizedTitle,
                        content = content,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            _uiState.update { it.copy(isSaving = false) }
            return true
        } catch (e: Exception) {
            _uiState.update { it.copy(saveError = e.message ?: "Failed to save note", isSaving = false) }
            return false
        }
    }

    class Factory(
        private val quickNoteRepository: QuickNoteRepository,
        private val noteId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(QuickNoteEditorViewModel::class.java)) {
                return QuickNoteEditorViewModel(quickNoteRepository, noteId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNoteEditorScreen(
    navController: NavController,
    viewModel: QuickNoteEditorViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewNote) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
                .padding(16.dp)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                uiState.loadError != null -> {
                    Text(
                        text = "Error: ${uiState.loadError}",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
                else -> {
                    // Composed only after loading finishes so initialText captures loaded values
                    val titleState = rememberTextFieldState(initialText = uiState.initialTitle)
                    val contentState = rememberTextFieldState(initialText = uiState.initialContent)

                    OutlinedTextField(
                        state = titleState,
                        label = { Text("Title (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        lineLimits = TextFieldLineLimits.SingleLine
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        state = contentState,
                        label = { Text("Content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val saved = viewModel.saveNote(
                                        title = titleState.text.toString(),
                                        content = contentState.text.toString()
                                    )
                                    if (saved) {
                                        navController.popBackStack()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save")
                        }
                    }
                    if (!uiState.saveError.isNullOrEmpty()) {
                        Text(
                            text = "Error: ${uiState.saveError}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}