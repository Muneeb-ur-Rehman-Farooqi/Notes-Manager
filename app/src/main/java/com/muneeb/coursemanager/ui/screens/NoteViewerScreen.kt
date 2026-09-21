package com.muneeb.coursemanager.ui.screens

import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.repository.ItemRepository
import com.muneeb.coursemanager.navigation.Routes
import com.muneeb.coursemanager.ui.theme.LocalAppStyle
import io.noties.markwon.Markwon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteViewerUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val content: String = "",
    val categoryId: Long = -1L,
    val itemId: Long = -1L,
    val error: String? = null
)

class NoteViewerViewModel(
    private val itemRepository: ItemRepository,
    private val itemId: Long
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoteViewerUiState())
    val uiState: StateFlow<NoteViewerUiState> = _uiState.asStateFlow()

    init {
        loadItem()
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
                                categoryId = item.categoryId,
                                itemId = item.itemId
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

    class Factory(
        private val itemRepository: ItemRepository,
        private val itemId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NoteViewerViewModel::class.java)) {
                return NoteViewerViewModel(itemRepository, itemId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteViewerScreen(
    navController: NavController,
    viewModel: NoteViewerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val style = LocalAppStyle.current
    val context = LocalContext.current
    val markwon = remember { Markwon.create(context) }
    // Note content colors are content, not chrome — intentionally not migrated to AppStyle tokens
    val onSurfaceArgb = MaterialTheme.colorScheme.onSurface.toArgb()
    val surfaceArgb = MaterialTheme.colorScheme.surface.toArgb()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.title.ifBlank { "Note" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (uiState.categoryId != -1L) {
                            navController.navigate(
                                Routes.noteEditor(uiState.categoryId, uiState.itemId)
                            )
                        }
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = style.errorTextColor
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                TextView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                    )
                                }
                            },
                            update = { tv ->
                                tv.textSize = 18f
                                tv.setTextColor(onSurfaceArgb)
                                tv.setBackgroundColor(surfaceArgb)
                                markwon.setMarkdown(tv, uiState.content)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}