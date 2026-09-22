package com.muneeb.coursemanager.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.entities.Category
import com.muneeb.coursemanager.data.repository.CategoryRepository
import com.muneeb.coursemanager.navigation.Routes
import com.muneeb.coursemanager.ui.theme.LocalAppStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryListUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class CategoryListViewModel(
    private val categoryRepository: CategoryRepository,
    private val courseId: Long
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoryListUiState())
    val uiState: StateFlow<CategoryListUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                categoryRepository.getCategoriesForCourse(courseId).collect { categories ->
                    _uiState.update { it.copy(categories = categories, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    suspend fun addCategory(name: String) {
        try {
            val category = Category(
                courseId = courseId,
                name = name,
                sortOrder = _uiState.value.categories.size
            )
            categoryRepository.insert(category)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to add category: ${e.message}") }
        }
    }

    suspend fun renameCategory(category: Category, newName: String) {
        try {
            categoryRepository.update(category.copy(name = newName))
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to rename category: ${e.message}") }
        }
    }

    suspend fun deleteCategories(categories: List<Category>) {
        try {
            categories.forEach { categoryRepository.delete(it) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to delete category: ${e.message}") }
        }
    }

    class Factory(
        private val categoryRepository: CategoryRepository,
        private val courseId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CategoryListViewModel::class.java)) {
                return CategoryListViewModel(categoryRepository, courseId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    navController: NavController,
    viewModel: CategoryListViewModel,
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val style = LocalAppStyle.current

    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    var renameTarget by remember { mutableStateOf<Category?>(null) }
    var renameNameText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection")
                        }
                    },
                    actions = {
                        val singleSelected = if (selectedIds.size == 1) {
                            uiState.categories.firstOrNull { it.categoryId == selectedIds.first() }
                        } else null
                        if (singleSelected != null) {
                            IconButton(
                                onClick = {
                                    renameTarget = singleSelected
                                    renameNameText = singleSelected.name
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
                    title = { Text("Categories") },
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
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Category") },
                containerColor = style.fabContainerColor,
                contentColor = style.fabContentColor
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.categories.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null && uiState.categories.isEmpty()) {
                Text(
                    text = "Error: ${uiState.error}",
                    modifier = Modifier.padding(16.dp),
                    color = style.errorTextColor
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.categories) { category ->
                        val isSelected = selectedIds.contains(category.categoryId)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedIds = if (isSelected) {
                                                selectedIds - category.categoryId
                                            } else {
                                                selectedIds + category.categoryId
                                            }
                                        } else {
                                            navController.navigate(Routes.itemList(category.categoryId))
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            selectedIds = setOf(category.categoryId)
                                        }
                                    }
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
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = style.cardTitleColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.viewModelScope.launch {
                                viewModel.addCategory(newCategoryName.trim())
                                newCategoryName = ""
                                showAddDialog = false
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val currentRenameTarget = renameTarget
    if (currentRenameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Category") },
            text = {
                OutlinedTextField(
                    value = renameNameText,
                    onValueChange = { renameNameText = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameNameText.isNotBlank(),
                    onClick = {
                        viewModel.viewModelScope.launch {
                            viewModel.renameCategory(currentRenameTarget, renameNameText.trim())
                            renameTarget = null
                            selectedIds = emptySet()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Category") },
            text = {
                Text(
                    if (selectedIds.size == 1) {
                        "This will also delete every file inside it. This can't be undone."
                    } else {
                        "This will also delete every file inside these ${selectedIds.size} categories. This can't be undone."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = uiState.categories.filter { selectedIds.contains(it.categoryId) }
                        viewModel.viewModelScope.launch {
                            viewModel.deleteCategories(toDelete)
                            selectedIds = emptySet()
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