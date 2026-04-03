package com.bulkrenamer.data.model

import android.net.Uri
import android.provider.DocumentsContract

data class FileNode(
    val documentId: String,
    val treeUri: Uri,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long,
    val lastModified: Long,
    val flags: Int,
    val displayPath: String = ""
) {
    /** Absolute filesystem path, derived from the file:// URI. */
    val absolutePath: String get() = uri.path ?: ""

    val isDirectory: Boolean
        get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR

    val isRenameable: Boolean
        get() = flags and DocumentsContract.Document.FLAG_SUPPORTS_RENAME != 0

    val extension: String
        get() = if (isDirectory) "" else {
            val lastDot = name.lastIndexOf('.')
            if (lastDot > 0) name.substring(lastDot + 1) else ""
        }

    val nameWithoutExtension: String
        get() = if (isDirectory) name else {
            val lastDot = name.lastIndexOf('.')
            if (lastDot > 0) name.substring(0, lastDot) else name
        }
}
