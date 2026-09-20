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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.entities.Item
import com.muneeb.coursemanager.data.entities.ItemType
import com.muneeb.coursemanager.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val isLoading: Boolean = false,
    val title: String = "",
    val content: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val isNewNote: Boolean = true
)

class NoteEditorViewModel(
    private val itemRepository: ItemRepository,
    private val categoryId: Long,
    private val itemId: Long
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    init {
        if (itemId != -1L) {
            loadItem()
        } else {
            _uiState.update { it.copy(isNewNote = true) }
        }
    }

    private fun loadItem() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                itemRepository.getItemById(itemId).collect { item ->
                    if (item != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                title = item.displayName,
                                content = item.noteText ?: "",
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
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Title is required") }
            return false
        }
        if (state.content.isBlank()) {
            _uiState.update { it.copy(error = "Content cannot be empty") }
            return false
        }
        _uiState.update { it.copy(isSaving = true, error = null) }
        try {
            if (state.isNewNote) {
                val item = Item(
                    categoryId = categoryId,
                    itemType = ItemType.NOTE,
                    displayName = state.title,
                    noteText = state.content,
                    sortOrder = 0
                )
                itemRepository.insert(item)
            } else {
                val existing = itemRepository.getItemById(itemId).first()
                if (existing == null) {
                    _uiState.update { it.copy(error = "Note not found", isSaving = false) }
                    return false
                }
                itemRepository.update(
                    existing.copy(
                        displayName = state.title,
                        noteText = state.content
                    )
                )
            }
            _uiState.update { it.copy(isSaving = false) }
            return true
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message, isSaving = false) }
            return false
        }
    }

    class Factory(
        private val itemRepository: ItemRepository,
        private val categoryId: Long,
        private val itemId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NoteEditorViewModel::class.java)) {
                return NoteEditorViewModel(itemRepository, categoryId, itemId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    navController: NavController,
    viewModel: NoteEditorViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.error != null && uiState.title.isEmpty() && uiState.content.isEmpty()) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = { viewModel.updateContent(it) },
                    label = { Text("Note") },
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
                            viewModel.viewModelScope.launch {
                                if (viewModel.saveNote()) {
                                    navController.popBackStack()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Note")
                    }
                }
                if (uiState.error != null && uiState.error?.isNotEmpty() == true) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}