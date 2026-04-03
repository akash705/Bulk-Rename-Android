package com.bulkrenamer.domain

import com.bulkrenamer.data.model.FileNode
import com.bulkrenamer.data.repository.FileSystemRepository
import javax.inject.Inject

class BrowseFilesUseCase @Inject constructor(
    private val repository: FileSystemRepository
) {
    /** List [path] children: directories first, then files — both alphabetical. */
    suspend operator fun invoke(path: String): List<FileNode> =
        repository.listChildren(path)
            .sortedWith(compareByDescending<FileNode> { it.isDirectory }.thenBy { it.name.lowercase() })
}
