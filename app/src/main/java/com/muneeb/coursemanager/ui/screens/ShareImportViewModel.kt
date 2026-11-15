package com.muneeb.coursemanager.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.muneeb.coursemanager.util.ImportedFileStore
import com.muneeb.coursemanager.util.StagedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShareImportViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _staged = MutableStateFlow<List<StagedFile>>(emptyList())
    val staged: StateFlow<List<StagedFile>> = _staged.asStateFlow()

    private val _navigateRequest = MutableStateFlow(false)
    val navigateRequest: StateFlow<Boolean> = _navigateRequest.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun onNavigationHandled() {
        _navigateRequest.value = false
    }

    fun clearMessage() {
        _message.value = null
    }

    fun stage(uris: List<Uri>) {
        if (uris.isEmpty()) {
            _message.value = "Nothing to import"
            return
        }
        viewModelScope.launch {
            val succeeded = mutableListOf<StagedFile>()
            var failedCount = 0
            uris.forEachIndexed { index, uri ->
                val result = ImportedFileStore.copyIntoStorage(app, uri, index)
                if (result != null) {
                    succeeded.add(result)
                } else {
                    failedCount++
                }
            }
            if (succeeded.isNotEmpty()) {
                _staged.update { it + succeeded }
                _navigateRequest.value = true
            }
            if (failedCount > 0) {
                _message.value = "Couldn't import $failedCount file(s)"
            }
        }
    }

    fun cancel() {
        val toDelete = _staged.value
        _staged.value = emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            toDelete.forEach { file ->
                ImportedFileStore.deleteIfOwned(app, file.contentUri)
            }
        }
    }

    fun onSaved() {
        _staged.value = emptyList()
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ShareImportViewModel::class.java)) {
                return ShareImportViewModel(app) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}