package com.muneeb.coursemanager.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class StagedFile(
    val contentUri: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long
)

object ImportedFileStore {

    fun authority(context: Context): String = "${context.packageName}.fileprovider"

    suspend fun copyIntoStorage(context: Context, source: Uri, index: Int): StagedFile? =
        withContext(Dispatchers.IO) {
            var targetFile: File? = null
            try {
                // Resolve original display name
                var originalName: String? = null
                context.contentResolver.query(source, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) {
                            originalName = cursor.getString(nameIdx)
                        }
                    }
                }
                val resolvedName = originalName?.takeIf { it.isNotBlank() }
                    ?: source.lastPathSegment?.takeIf { it.isNotBlank() }
                    ?: "Shared file"

                // Extract extension from filename or fall back to MIME type
                val lastDot = resolvedName.lastIndexOf('.')
                val rawExt = if (lastDot > 0 && lastDot < resolvedName.length - 1) {
                    resolvedName.substring(lastDot + 1)
                        .filter { it.isLetterOrDigit() }
                        .take(8)
                        .lowercase(Locale.ROOT)
                } else {
                    ""
                }

                val resolverMime = context.contentResolver.getType(source)
                val ext = rawExt.ifEmpty {
                    resolverMime?.let {
                        MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
                    }?.filter { it.isLetterOrDigit() }?.take(8)?.lowercase(Locale.ROOT).orEmpty()
                }

                // Determine final MIME type
                val guessedMime = if (resolverMime == null || resolverMime == "application/octet-stream") {
                    if (ext.isNotEmpty()) {
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                    } else {
                        resolverMime
                    }
                } else {
                    resolverMime
                }
                val finalMime = if (ext == "pdf") "application/pdf" else guessedMime

                // Prepare destination file in internal imports directory
                val importsDir = File(context.filesDir, "imports").apply { mkdirs() }
                val fileName = if (ext.isNotEmpty()) {
                    "${System.currentTimeMillis()}_$index.$ext"
                } else {
                    "${System.currentTimeMillis()}_$index"
                }
                val file = File(importsDir, fileName)
                targetFile = file

                // Stream copy from source URI to internal storage
                val inputStream = context.contentResolver.openInputStream(source) ?: return@withContext null
                inputStream.use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead <= 0) break
                            output.write(buffer, 0, bytesRead)
                        }
                        output.flush()
                    }
                }

                val strippedDisplayName = if (lastDot > 0 && lastDot < resolvedName.length - 1) {
                    resolvedName.substring(0, lastDot)
                } else {
                    resolvedName
                }

                val contentUri = FileProvider.getUriForFile(context, authority(context), file).toString()
                StagedFile(
                    contentUri = contentUri,
                    displayName = strippedDisplayName,
                    mimeType = finalMime,
                    sizeBytes = file.length()
                )
            } catch (e: Exception) {
                targetFile?.delete()
                null
            }
        }

    suspend fun deleteIfOwned(context: Context, uriString: String?) = withContext(Dispatchers.IO) {
        if (uriString.isNullOrBlank()) return@withContext
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "content" && uri.authority == authority(context)) {
                val segment = uri.lastPathSegment ?: return@withContext
                val file = File(File(context.filesDir, "imports"), segment)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (_: Exception) {
            // Ignore malformed URIs or deletion errors
        }
    }

    // Mirrors deleteIfOwned's ownership check without deleting anything.
    fun isOwned(context: Context, uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            uri.scheme == "content" && uri.authority == authority(context)
        } catch (_: Exception) {
            false
        }
    }
}