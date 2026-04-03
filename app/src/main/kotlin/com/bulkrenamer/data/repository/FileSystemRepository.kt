package com.bulkrenamer.data.repository

import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import com.bulkrenamer.data.model.FileNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
    suspend fun renameFile(absolutePath: String, newName: String, overwrite: Boolean = false): String? = withContext(Dispatchers.IO) {
        try {
            val src = File(absolutePath)
            val dst = File(src.parentFile!!, newName)
            if (overwrite && dst.exists()) dst.delete()
            if (src.renameTo(dst)) dst.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Creates a copy of [absolutePath] with [newName] in the same directory.
     * Returns the new absolute path on success, null on failure.
     */
    suspend fun copyFile(absolutePath: String, newName: String, overwrite: Boolean = false): String? = withContext(Dispatchers.IO) {
        try {
            val src = File(absolutePath)
            val dst = File(src.parentFile!!, newName)
            val options = if (overwrite) arrayOf(StandardCopyOption.REPLACE_EXISTING) else emptyArray()
            Files.copy(src.toPath(), dst.toPath(), *options)
            dst.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Deletes the file at [absolutePath]. Used for undo of copy operations.
     * Returns true on success.
     */
    suspend fun deleteFile(absolutePath: String): Boolean = withContext(Dispatchers.IO) {
        try { File(absolutePath).delete() } catch (_: Exception) { false }
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
