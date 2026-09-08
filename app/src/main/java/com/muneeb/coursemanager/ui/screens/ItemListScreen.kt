package com.muneeb.coursemanager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.entities.Category
import com.muneeb.coursemanager.data.entities.Item
import com.muneeb.coursemanager.data.entities.ItemType
import com.muneeb.coursemanager.data.repository.CategoryRepository
import com.muneeb.coursemanager.data.repository.ItemRepository
import com.muneeb.coursemanager.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ItemListUiState(
    val items: List<Item> = emptyList(),
    val category: Category? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class ItemListViewModel(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    val categoryId: Long
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItemListUiState())
    val uiState: StateFlow<ItemListUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                categoryRepository.getCategoryById(categoryId).collect { category ->
                    if (category == null) {
                        _uiState.update { it.copy(error = "Category not found", isLoading = false) }
                        return@collect
                    }
                    _uiState.update { it.copy(category = category) }
                    itemRepository.getItemsForCategory(categoryId).collect { items ->
                        _uiState.update { state ->
                            state.copy(items = items, isLoading = false)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    suspend fun addItem(item: Item) {
        try {
            itemRepository.insert(item)
            loadData()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to add item: ${e.message}") }
        }
    }

    suspend fun updateLastOpened(itemId: Long) {
        itemRepository.updateLastOpened(itemId, System.currentTimeMillis())
    }

    class Factory(
        private val itemRepository: ItemRepository,
        private val categoryRepository: CategoryRepository,
        private val categoryId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ItemListViewModel::class.java)) {
                return ItemListViewModel(itemRepository, categoryRepository, categoryId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(
    navController: NavController,
    viewModel: ItemListViewModel,
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // ignore
            }
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayName = it.getString(it.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                    val size = it.getLong(it.getColumnIndexOrThrow(android.provider.OpenableColumns.SIZE))
                    val mimeType = context.contentResolver.getType(uri)
                    viewModel.viewModelScope.launch {
                        val item = Item(
                            categoryId = viewModel.categoryId,
                            itemType = ItemType.FILE,
                            displayName = displayName ?: uri.lastPathSegment ?: "File",
                            uri = uri.toString(),
                            mimeType = mimeType,
                            sizeBytes = if (size > 0) size else null,
                            sortOrder = uiState.items.size
                        )
                        viewModel.addItem(item)
                    }
                }
            }
        }
    }

    val titleText = uiState.category?.name ?: "Items"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showMenu = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Files") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null && uiState.items.isEmpty()) {
                Text(
                    text = "Error: ${uiState.error}",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Red
                )
            } else if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Please drop your related files",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.items) { item ->
                        ItemRow(
                            item = item,
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    viewModel.updateLastOpened(item.itemId)
                                }
                            }
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Import File") },
                onClick = {
                    showMenu = false
                    filePickerLauncher.launch(arrayOf("*/*"))
                }
            )
            DropdownMenuItem(
                text = { Text("Write Note") },
                onClick = {
                    showMenu = false
                    navController.navigate(Routes.noteEditor(viewModel.categoryId))
                }
            )
        }
    }
}

@Composable
private fun ItemRow(item: Item, onClick: () -> Unit) {
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
            Column {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = item.itemType.name + " · " + formatDate(item.dateAdded),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            if (item.itemType == ItemType.FILE) {
                Text(
                    text = formatFileSize(item.sizeBytes ?: 0L),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}