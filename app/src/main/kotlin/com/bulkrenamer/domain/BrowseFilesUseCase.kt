package com.bulkrenamer.domain

import android.net.Uri
import com.bulkrenamer.data.model.FileNode
import com.bulkrenamer.data.repository.FileSystemRepository
import javax.inject.Inject

class BrowseFilesUseCase @Inject constructor(
    private val repository: FileSystemRepository
) {
    /**
     * List children of [folderUri], sorted: directories first, then alphabetical by name.
     */
    suspend operator fun invoke(folderUri: Uri): List<FileNode> {
        return repository.listChildren(folderUri)
            .sortedWith(compareByDescending<FileNode> { it.isDirectory }.thenBy { it.name.lowercase() })
    }
}
