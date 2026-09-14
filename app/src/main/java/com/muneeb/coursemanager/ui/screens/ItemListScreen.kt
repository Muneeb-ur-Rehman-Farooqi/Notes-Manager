package com.muneeb.coursemanager.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import com.muneeb.coursemanager.data.entities.Page
import com.muneeb.coursemanager.data.repository.CategoryRepository
import com.muneeb.coursemanager.data.repository.ItemRepository
import com.muneeb.coursemanager.data.repository.PageRepository
import com.muneeb.coursemanager.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed class NameDialogMode {
    data class RenameExisting(val item: Item) : NameDialogMode()
    data class ConfirmNewFile(
        val uri: Uri,
        val size: Long?,
        val mimeType: String?,
        val defaultName: String
    ) : NameDialogMode()
    data class ConfirmNewPhotoGroup(
        val uris: List<Uri>,
        val defaultName: String
    ) : NameDialogMode()
}

private fun stripKnownExtension(name: String): String {
    val lastDot = name.lastIndexOf('.')
    return if (lastDot > 0 && lastDot < name.length - 1) name.substring(0, lastDot) else name
}

data class ItemListUiState(
    val items: List<Item> = emptyList(),
    val category: Category? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class ItemListViewModel(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val pageRepository: PageRepository,
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

    suspend fun addPhotoGroup(uris: List<Uri>, displayName: String) {
        if (uris.isEmpty()) return
        try {
            val item = Item(
                categoryId = categoryId,
                itemType = ItemType.PHOTO_GROUP,
                displayName = displayName,
                sortOrder = _uiState.value.items.size
            )
            val newItemId = itemRepository.insert(item)
            uris.forEachIndexed { index, uri ->
                pageRepository.insert(
                    Page(
                        itemId = newItemId,
                        photoUri = uri.toString(),
                        pageNumber = index
                    )
                )
            }
            loadData()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to add photos: ${e.message}") }
        }
    }

    suspend fun renameItem(item: Item, newName: String) {
        try {
            itemRepository.update(item.copy(displayName = newName))
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to rename item: ${e.message}") }
        }
    }

    suspend fun deleteItems(items: List<Item>) {
        try {
            items.forEach { itemRepository.delete(it) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to delete items: ${e.message}") }
        }
    }

    suspend fun updateLastOpened(itemId: Long) {
        itemRepository.updateLastOpened(itemId, System.currentTimeMillis())
    }

    class Factory(
        private val itemRepository: ItemRepository,
        private val categoryRepository: CategoryRepository,
        private val pageRepository: PageRepository,
        private val categoryId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ItemListViewModel::class.java)) {
                return ItemListViewModel(
                    itemRepository, categoryRepository, pageRepository, categoryId
                ) as T
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
    var selectedItemIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val isSelectionMode = selectedItemIds.isNotEmpty()
    var showAddSheet by remember { mutableStateOf(false) }
    var nameDialogMode by remember { mutableStateOf<NameDialogMode?>(null) }
    var nameDialogText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // ignore
            }
            var displayName: String? = null
            var size: Long? = null
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    displayName = it.getString(it.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                    val rawSize = it.getLong(it.getColumnIndexOrThrow(android.provider.OpenableColumns.SIZE))
                    size = if (rawSize > 0) rawSize else null
                }
            }
            val mimeType = context.contentResolver.getType(uri)
            val defaultName = stripKnownExtension(
                displayName ?: uri.lastPathSegment ?: "File"
            )
            nameDialogMode = NameDialogMode.ConfirmNewFile(
                uri = uri,
                size = size,
                mimeType = mimeType,
                defaultName = defaultName
            )
            nameDialogText = defaultName
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    // ignore
                }
            }
            val defaultName = "Photos - " + SimpleDateFormat(
                "dd MMM yyyy HH:mm", Locale.getDefault()
            ).format(Date())
            nameDialogMode = NameDialogMode.ConfirmNewPhotoGroup(
                uris = uris,
                defaultName = defaultName
            )
            nameDialogText = defaultName
        }
    }

    val titleText = uiState.category?.name ?: "Items"

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedItemIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedItemIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection")
                        }
                    },
                    actions = {
                        val singleSelected = if (selectedItemIds.size == 1) {
                            uiState.items.firstOrNull { it.itemId == selectedItemIds.first() }
                        } else null
                        if (singleSelected != null) {
                            IconButton(
                                onClick = {
                                    nameDialogMode = NameDialogMode.RenameExisting(singleSelected)
                                    nameDialogText = stripKnownExtension(singleSelected.displayName)
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename")
                            }
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(titleText) },
                    navigationIcon = {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
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
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedItemIds.contains(item.itemId),
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    viewModel.updateLastOpened(item.itemId)
                                }
                                when (item.itemType) {
                                    ItemType.PHOTO_GROUP -> {
                                        navController.navigate(Routes.photoViewer(item.itemId))
                                    }
                                    ItemType.FILE -> {
                                        val isPdf = item.mimeType == "application/pdf" ||
                                                item.displayName.endsWith(".pdf", ignoreCase = true)
                                        if (isPdf) {
                                            navController.navigate(Routes.pdfViewer(item.itemId))
                                        } else if (item.uri != null) {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(
                                                    Uri.parse(item.uri),
                                                    item.mimeType ?: "*/*"
                                                )
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: ActivityNotFoundException) {
                                                Toast.makeText(
                                                    context,
                                                    "No app found to open this file",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                    else -> { /* NOTE handled elsewhere */ }
                                }
                            },
                            onToggleSelect = {
                                selectedItemIds = if (selectedItemIds.contains(item.itemId)) {
                                    selectedItemIds - item.itemId
                                } else {
                                    selectedItemIds + item.itemId
                                }
                            },
                            onEnterSelectionMode = {
                                selectedItemIds = setOf(item.itemId)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
            dragHandle = {
                IconButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) showAddSheet = false
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
                                    showAddSheet = false
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                }
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 12.dp))
                    Text("Import File")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showAddSheet = false
                                    photoPickerLauncher.launch(arrayOf("image/*"))
                                }
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 12.dp))
                    Text("Add Photos")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showAddSheet = false
                                    navController.navigate(Routes.noteEditor(viewModel.categoryId))
                                }
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 12.dp))
                    Text("Write Note")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    val currentDialogMode = nameDialogMode
    if (currentDialogMode != null) {
        val dialogTitle = when (currentDialogMode) {
            is NameDialogMode.RenameExisting -> "Rename"
            is NameDialogMode.ConfirmNewFile -> "Name this file"
            is NameDialogMode.ConfirmNewPhotoGroup -> "Name this photo set"
        }
        AlertDialog(
            onDismissRequest = { nameDialogMode = null },
            title = { Text(dialogTitle) },
            text = {
                OutlinedTextField(
                    value = nameDialogText,
                    onValueChange = { nameDialogText = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = nameDialogText.isNotBlank(),
                    onClick = {
                        when (currentDialogMode) {
                            is NameDialogMode.RenameExisting -> {
                                viewModel.viewModelScope.launch {
                                    viewModel.renameItem(
                                        currentDialogMode.item,
                                        nameDialogText.trim()
                                    )
                                    nameDialogMode = null
                                }
                            }
                            is NameDialogMode.ConfirmNewFile -> {
                                val newItem = Item(
                                    categoryId = viewModel.categoryId,
                                    itemType = ItemType.FILE,
                                    displayName = nameDialogText.trim(),
                                    uri = currentDialogMode.uri.toString(),
                                    mimeType = currentDialogMode.mimeType,
                                    sizeBytes = currentDialogMode.size,
                                    sortOrder = uiState.items.size
                                )
                                viewModel.viewModelScope.launch {
                                    viewModel.addItem(newItem)
                                    nameDialogMode = null
                                }
                            }
                            is NameDialogMode.ConfirmNewPhotoGroup -> {
                                viewModel.viewModelScope.launch {
                                    viewModel.addPhotoGroup(
                                        currentDialogMode.uris,
                                        nameDialogText.trim()
                                    )
                                    nameDialogMode = null
                                }
                            }
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { nameDialogMode = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete") },
            text = { Text("Are you sure you want to delete ${selectedItemIds.size} item(s)?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = uiState.items.filter { selectedItemIds.contains(it.itemId) }
                        viewModel.viewModelScope.launch {
                            viewModel.deleteItems(toDelete)
                            selectedItemIds = emptySet()
                            showDeleteConfirm = false
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ItemRow(
    item: Item,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onEnterSelectionMode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelect() else onClick() },
                onLongClick = { if (!isSelectionMode) onEnterSelectionMode() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(
                text = stripKnownExtension(item.displayName),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}