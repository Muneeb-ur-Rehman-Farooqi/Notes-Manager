package com.muneeb.coursemanager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickNoteEditorUiState(
    val isLoading: Boolean = false,
    val title: String = "",
    val content: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val isNewNote: Boolean = true
)

class QuickNoteEditorViewModel(
    private val quickNoteRepository: QuickNoteRepository,
    private val noteId: Long
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuickNoteEditorUiState())
    val uiState: StateFlow<QuickNoteEditorUiState> = _uiState.asStateFlow()

    init {
        if (noteId != -1L) {
            loadNote()
        } else {
            _uiState.update { it.copy(isNewNote = true) }
        }
    }

    private fun loadNote() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                quickNoteRepository.getById(noteId).collect { note ->
                    if (note != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                title = note.title ?: "",
                                content = note.content,
                                isNewNote = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(error = "Note not found", isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateContent(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    suspend fun saveNote(): Boolean {
        val state = _uiState.value
        if (state.content.isBlank()) {
            _uiState.update { it.copy(error = "Content cannot be empty") }
            return false
        }
        _uiState.update { it.copy(isSaving = true, error = null) }
        try {
            val note = QuickNote(
                noteId = if (state.isNewNote) 0 else noteId,
                title = state.title.takeIf { it.isNotBlank() },
                content = state.content,
                createdAt = if (state.isNewNote) System.currentTimeMillis() else 0,
                updatedAt = System.currentTimeMillis()
            )
            if (state.isNewNote) {
                quickNoteRepository.insert(note)
            } else {
                quickNoteRepository.update(note)
            }
            _uiState.update { it.copy(isSaving = false) }
            return true
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message, isSaving = false) }
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

    LaunchedEffect(uiState.isSaving) {
        if (uiState.isSaving) {
            // Wait for save to complete; handled via return from saveNote
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewNote) "New Note" else "Edit Note") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.error != null && uiState.title.isEmpty() && uiState.content.isEmpty()) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = Color.Red
                )
            } else {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Title (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = { viewModel.updateContent(it) },
                    label = { Text("Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    singleLine = false
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Button(
                        onClick = {
                            viewModel.viewModelScope.launch {
                                if (viewModel.saveNote()) {
                                    navController.popBackStack()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save")
                    }
                }
                if (uiState.error != null && uiState.error?.isNotEmpty() == true) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}