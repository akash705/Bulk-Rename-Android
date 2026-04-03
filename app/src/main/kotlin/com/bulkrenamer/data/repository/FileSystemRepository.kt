package com.bulkrenamer.data.repository

import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import com.bulkrenamer.data.model.FileNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileSystemRepository @Inject constructor() {

    /**
     * List the immediate children of [path], mapping each to a [FileNode].
     * Returns an empty list if the path is not a readable directory.
     */
    suspend fun listChildren(path: String): List<FileNode> = withContext(Dispatchers.IO) {
        File(path).listFiles()?.map { it.toFileNode() } ?: emptyList()
    }

    /**
     * Rename [absolutePath] to [newName] within the same directory.
     * Returns the new absolute path on success, null on failure.
     */
    suspend fun renameFile(absolutePath: String, newName: String): String? = withContext(Dispatchers.IO) {
        try {
            val src = File(absolutePath)
            val dst = File(src.parentFile!!, newName)
            if (src.renameTo(dst)) dst.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns the current filename at [absolutePath], or null if the file no longer exists.
     * Used by undo validation to check whether the file is still in its expected state.
     */
    suspend fun getFileName(absolutePath: String): String? = withContext(Dispatchers.IO) {
        File(absolutePath).takeIf { it.exists() }?.name
    }
}

private fun File.toFileNode(): FileNode {
    val mime = when {
        isDirectory -> DocumentsContract.Document.MIME_TYPE_DIR
        else -> MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension.lowercase())
            ?: "application/octet-stream"
    }
    return FileNode(
        documentId = absolutePath,
        treeUri = Uri.fromFile(parentFile ?: this),
        uri = Uri.fromFile(this),
        name = name,
        mimeType = mime,
        size = if (isDirectory) 0L else length(),
        lastModified = lastModified(),
        flags = if (canWrite()) DocumentsContract.Document.FLAG_SUPPORTS_RENAME else 0
    )
}
