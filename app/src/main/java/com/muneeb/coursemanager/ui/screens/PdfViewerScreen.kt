package com.muneeb.coursemanager.ui.screens

import android.app.Application
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.muneeb.coursemanager.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PdfViewerUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val displayName: String = "",
    val currentPageIndex: Int = 0,
    val pageCount: Int = 0,
    val nightModeEnabled: Boolean = false,
    val pagedModeEnabled: Boolean = false,
    val documentUri: Uri? = null,
    val lastReadPage: Int = 0
)

class PdfViewerViewModel(
    private val app: Application,
    private val itemRepository: ItemRepository,
    private val itemId: Long,
    initialDarkMode: Boolean
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(PdfViewerUiState(nightModeEnabled = initialDarkMode))
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    init {
        loadItem()
    }

    private fun loadItem() {
        viewModelScope.launch {
            try {
                val item = itemRepository.getItemById(itemId).firstOrNull()
                if (item == null) {
                    setError("File not found")
                    return@launch
                }
                val uriStr = item.uri
                if (uriStr == null) {
                    setError("This file can't be previewed")
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        displayName = item.displayName,
                        documentUri = Uri.parse(uriStr),
                        lastReadPage = item.lastReadPage
                    )
                }
            } catch (e: Exception) {
                setError("This file can't be previewed")
            }
        }
    }

    fun onDocumentLoaded(pageCount: Int) {
        _uiState.update {
            it.copy(isLoading = false, pageCount = pageCount)
        }
    }

    fun onPageChanged(page: Int, pageCount: Int) {
        _uiState.update {
            it.copy(currentPageIndex = page, pageCount = pageCount)
        }
        viewModelScope.launch {
            itemRepository.updateLastReadPage(itemId, page)
        }
    }

    fun onRenderError(message: String) {
        setError(message)
    }

    fun toggleNightMode() {
        _uiState.update { it.copy(nightModeEnabled = !it.nightModeEnabled) }
    }

    fun togglePagedMode() {
        _uiState.update { it.copy(pagedModeEnabled = !it.pagedModeEnabled) }
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(error = message, isLoading = false) }
    }

    class Factory(
        private val app: Application,
        private val itemRepository: ItemRepository,
        private val itemId: Long,
        private val initialDarkMode: Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PdfViewerViewModel::class.java)) {
                return PdfViewerViewModel(app, itemRepository, itemId, initialDarkMode) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    navController: NavController,
    viewModel: PdfViewerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var chromeVisible by remember { mutableStateOf(true) }
    var lastKnownPage by remember(uiState.documentUri) {
        mutableStateOf(uiState.lastReadPage)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.documentUri == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = uiState.error ?: "Error", color = Color.Red)
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
            }
            else -> {
                val documentUri = uiState.documentUri
                if (documentUri == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    key(documentUri, uiState.nightModeEnabled, uiState.pagedModeEnabled) {
                        AndroidView(
                            factory = { ctx ->
                                val pdfView = PDFView(ctx, null)
                                pdfView.useBestQuality(true)
                                val config = pdfView.fromUri(documentUri)
                                    .defaultPage(lastKnownPage)
                                    .enableSwipe(true)
                                    .enableDoubletap(true)
                                    .scrollHandle(DefaultScrollHandle(ctx))
                                    .onLoad { pages ->
                                        viewModel.onDocumentLoaded(pages)
                                    }
                                    .onPageChange { page, pageCount ->
                                        lastKnownPage = page
                                        viewModel.onPageChanged(page, pageCount)
                                    }
                                    .onError { throwable ->
                                        viewModel.onRenderError(
                                            throwable.message ?: "Failed to load PDF"
                                        )
                                    }
                                    .nightMode(uiState.nightModeEnabled)
                                    .onTap {
                                        chromeVisible = !chromeVisible
                                        false
                                    }

                                if (uiState.pagedModeEnabled) {
                                    config
                                        .swipeHorizontal(true)
                                        .pageSnap(true)
                                        .autoSpacing(true)
                                        .pageFling(true)
                                } else {
                                    config
                                        .swipeHorizontal(false)
                                        .pageSnap(false)
                                        .autoSpacing(false)
                                        .pageFling(false)
                                }

                                config.load()

                                pdfView.setMinZoom(1f)
                                pdfView.setMidZoom(3f)
                                pdfView.setMaxZoom(8f)
                                pdfView
                            },
                            update = { /* all config happens in factory */ },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (uiState.pageCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                                .background(Color.Black, RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Page ${uiState.currentPageIndex + 1} of ${uiState.pageCount}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.displayName.ifBlank { "PDF" },
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
                    TextButton(onClick = { viewModel.toggleNightMode() }) {
                        Text(if (uiState.nightModeEnabled) "Day" else "Night")
                    }
                    TextButton(onClick = { viewModel.togglePagedMode() }) {
                        Text(if (uiState.pagedModeEnabled) "Scroll" else "Pages")
                    }
                }
            )
        }
    }
}