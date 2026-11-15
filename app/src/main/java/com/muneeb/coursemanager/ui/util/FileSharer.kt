package com.muneeb.coursemanager.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ShareTarget(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?
)

object FileSharer {

    private val ILLEGAL_FILENAME_CHARS = Regex("""[\\/:*?"<>|]""")

    suspend fun share(context: Context, targets: List<ShareTarget>) {
        if (targets.isEmpty()) return

        val stagedUrisWithMime: List<Pair<Uri, String?>> = withContext(Dispatchers.IO) {
            val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
            // Clear previous cached share files
            shareDir.listFiles()?.forEach { it.deleteRecursively() }

            val results = mutableListOf<Pair<Uri, String?>>()
            val usedNames = mutableSetOf<String>()

            for (target in targets) {
                try {
                    val inputStream = context.contentResolver.openInputStream(target.uri) ?: continue
                    val sanitizedBase = target.displayName
                        .replace(ILLEGAL_FILENAME_CHARS, "_")
                        .trim()
                        .ifEmpty { "file" }

                    val ext = target.mimeType?.let {
                        MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
                    }?.takeIf { it.isNotBlank() }

                    // Resolve unique filename in share cache
                    var candidateName = if (ext != null) "$sanitizedBase.$ext" else sanitizedBase
                    var counter = 1
                    while (usedNames.contains(candidateName)) {
                        candidateName = if (ext != null) {
                            "${sanitizedBase}_$counter.$ext"
                        } else {
                            "${sanitizedBase}_$counter"
                        }
                        counter++
                    }
                    usedNames.add(candidateName)

                    val outFile = File(shareDir, candidateName)
                    inputStream.use { input ->
                        outFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                        }
                    }

                    val shareUri = FileProvider.getUriForFile(
                        context,
                        ImportedFileStore.authority(context),
                        outFile
                    )
                    results.add(shareUri to target.mimeType)
                } catch (_: Exception) {
                    // Skip files that fail to open or copy
                }
            }
            results
        }

        if (stagedUrisWithMime.isEmpty()) {
            Toast.makeText(context, "Couldn't read this file", Toast.LENGTH_SHORT).show()
            return
        }

        val distinctMimes = stagedUrisWithMime.map { it.second }.distinct()
        val sharedMime = if (distinctMimes.size == 1 && !distinctMimes.first().isNullOrBlank()) {
            distinctMimes.first()!!
        } else {
            "*/*"
        }

        val builder = ShareCompat.IntentBuilder(context)
            .setType(sharedMime)
            .setChooserTitle("Share")

        stagedUrisWithMime.forEach { (uri, _) ->
            builder.addStream(uri)
        }

        builder.startChooser()
    }
}