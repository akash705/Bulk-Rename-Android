package com.bulkrenamer.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.bulkrenamer.data.db.GrantedUriDao
import com.bulkrenamer.data.db.GrantedUriEntity
import com.bulkrenamer.data.model.FileNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// Warn user when approaching Android's 512 persistent URI permission limit
const val URI_QUOTA_WARN_THRESHOLD = 400

@Singleton
class FileSystemRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    private val grantedUriDao: GrantedUriDao
) {

    /**
     * List children of a folder using a cursor query — single IPC call, 10-50x faster
     * than DocumentFile.listFiles() which issues one IPC per child.
     */
    suspend fun listChildren(folderUri: Uri): List<FileNode> = withContext(Dispatchers.IO) {
        val treeDocumentId = DocumentsContract.getTreeDocumentId(folderUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            folderUri,
            treeDocumentId
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS
        )
        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            buildList {
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val flagsCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_FLAGS)
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idCol)
                    add(
                        FileNode(
                            documentId = documentId,
                            treeUri = folderUri,
                            uri = DocumentsContract.buildDocumentUriUsingTree(folderUri, documentId),
                            name = cursor.getString(nameCol) ?: "",
                            mimeType = cursor.getString(mimeCol) ?: "",
                            size = cursor.getLong(sizeCol),
                            lastModified = cursor.getLong(modCol),
                            flags = cursor.getInt(flagsCol)
                        )
                    )
                }
            }
        } ?: emptyList()
    }

    /**
     * Rename a document. Returns the new URI on success (old URI is dead after rename).
     * Returns null if the provider returns null or the document is not found.
     */
    suspend fun renameDocument(uri: Uri, newName: String): Uri? = withContext(Dispatchers.IO) {
        val doc = DocumentFile.fromSingleUri(contentResolver.run { this }, uri)
            ?: return@withContext null
        // DocumentFile.renameTo() calls DocumentsContract.renameDocument() internally
        doc.renameTo(newName)?.uri
    }

    suspend fun persistUriPermission(uri: Uri) = withContext(Dispatchers.IO) {
        grantedUriDao.insert(GrantedUriEntity(uri.toString(), System.currentTimeMillis()))
    }

    suspend fun revokeUriPermission(uri: Uri) = withContext(Dispatchers.IO) {
        grantedUriDao.delete(GrantedUriEntity(uri.toString(), 0))
        try {
            contentResolver.releasePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Already revoked externally
        }
    }

    suspend fun getGrantedUriCount(): Int = withContext(Dispatchers.IO) {
        grantedUriDao.getCount()
    }

    suspend fun isNearUriQuota(): Boolean = getGrantedUriCount() >= URI_QUOTA_WARN_THRESHOLD

    /**
     * Validate persisted URIs against the system's permission list.
     * Returns URIs that are no longer valid.
     */
    suspend fun getStalePermissions(): List<Uri> = withContext(Dispatchers.IO) {
        val systemPerms = contentResolver.persistedUriPermissions.map { it.uri.toString() }.toSet()
        grantedUriDao.getAll()
            .filter { it.uri !in systemPerms }
            .map { Uri.parse(it.uri) }
    }
}
