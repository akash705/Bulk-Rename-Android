package com.bulkrenamer.data.model

import android.net.Uri

data class RenameResult(
    val originalUri: Uri,
    val originalName: String,
    val newName: String,
    val newUri: Uri?,
    val error: Exception?
) {
    val isSuccess: Boolean get() = error == null && newUri != null
}
