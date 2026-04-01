package com.bulkrenamer.ui.viewmodel

import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.SavedStateHandle
import com.bulkrenamer.data.model.FileNode
import com.bulkrenamer.data.model.RenameRule
import com.bulkrenamer.data.repository.FileSystemRepository
import com.bulkrenamer.domain.BrowseFilesUseCase
import com.bulkrenamer.ui.state.FileExplorerUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FileExplorerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var browseFiles: BrowseFilesUseCase
    private lateinit var repository: FileSystemRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private fun testUri(path: String = "test"): Uri {
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://test/$path"
        return uri
    }

    private fun fileNode(name: String, isDir: Boolean = false): FileNode {
        return FileNode(
            documentId = "id_$name",
            treeUri = testUri(),
            uri = testUri(name),
            name = name,
            mimeType = if (isDir) DocumentsContract.Document.MIME_TYPE_DIR else "image/jpeg",
            size = 0,
            lastModified = 0,
            flags = DocumentsContract.Document.FLAG_SUPPORTS_RENAME
        )
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        browseFiles = mockk()
        repository = mockk()
        savedStateHandle = SavedStateHandle()

        coEvery { repository.getStalePermissions() } returns emptyList()
        coEvery { repository.getGrantedUriCount() } returns 0
        coEvery { repository.isNearUriQuota() } returns false
        coEvery { repository.persistUriPermission(any()) } returns Unit
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): FileExplorerViewModel =
        FileExplorerViewModel(browseFiles, repository, savedStateHandle)

    @Test
    fun `initial state emits PermissionRequired when no URIs granted`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value is FileExplorerUiState.PermissionRequired)
    }

    @Test
    fun `onFolderGranted navigates to folder and emits Browsing`() = runTest {
        val folderUri = testUri("folder")
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"))
        coEvery { browseFiles(folderUri) } returns files
        coEvery { repository.getGrantedUriCount() } returns 1

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onFolderGranted(folderUri)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is FileExplorerUiState.Browsing)
        assertEquals(2, (state as FileExplorerUiState.Browsing).entries.size)
    }

    @Test
    fun `toggleSelection adds and removes file from selection`() = runTest {
        val folderUri = testUri("folder")
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"))
        coEvery { browseFiles(folderUri) } returns files
        coEvery { repository.getGrantedUriCount() } returns 1

        val vm = createViewModel()
        vm.onFolderGranted(folderUri)
        advanceUntilIdle()

        vm.toggleSelection("id_a.jpg")
        advanceUntilIdle()

        var browsing = vm.uiState.value as FileExplorerUiState.Browsing
        assertEquals(1, browsing.selectedCount)

        vm.toggleSelection("id_a.jpg")
        advanceUntilIdle()

        browsing = vm.uiState.value as FileExplorerUiState.Browsing
        assertEquals(0, browsing.selectedCount)
    }

    @Test
    fun `selectAll selects only non-directory files`() = runTest {
        val folderUri = testUri("folder")
        val files = listOf(fileNode("a.jpg"), fileNode("SubDir", isDir = true), fileNode("b.jpg"))
        coEvery { browseFiles(folderUri) } returns files
        coEvery { repository.getGrantedUriCount() } returns 1

        val vm = createViewModel()
        vm.onFolderGranted(folderUri)
        advanceUntilIdle()

        vm.selectAll()
        advanceUntilIdle()

        val browsing = vm.uiState.value as FileExplorerUiState.Browsing
        // 2 files, 1 directory — only 2 should be selected
        assertEquals(2, browsing.selectedCount)
        assertFalse("id_SubDir" in browsing.selection)
    }

    @Test
    fun `deselectAll clears selection`() = runTest {
        val folderUri = testUri("folder")
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"))
        coEvery { browseFiles(folderUri) } returns files
        coEvery { repository.getGrantedUriCount() } returns 1

        val vm = createViewModel()
        vm.onFolderGranted(folderUri)
        advanceUntilIdle()
        vm.selectAll()
        advanceUntilIdle()
        vm.deselectAll()
        advanceUntilIdle()

        val browsing = vm.uiState.value as FileExplorerUiState.Browsing
        assertEquals(0, browsing.selectedCount)
    }

    @Test
    fun `previewRename emits RenamePreviewing state`() = runTest {
        val folderUri = testUri("folder")
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"))
        coEvery { browseFiles(folderUri) } returns files
        coEvery { repository.getGrantedUriCount() } returns 1

        val vm = createViewModel()
        vm.onFolderGranted(folderUri)
        advanceUntilIdle()
        vm.selectAll()
        advanceUntilIdle()

        vm.previewRename(RenameRule.AddPrefix("2024_"))

        val state = vm.uiState.value
        assertTrue(state is FileExplorerUiState.RenamePreviewing)
        assertEquals(2, (state as FileExplorerUiState.RenamePreviewing).selectedCount)
    }

    @Test
    fun `navigateUp returns to parent folder`() = runTest {
        val rootUri = testUri("root")
        val childUri = testUri("child")
        val rootFiles = listOf(fileNode("SubDir", isDir = true))
        val childFiles = listOf(fileNode("img.jpg"))

        coEvery { browseFiles(rootUri) } returns rootFiles
        coEvery { browseFiles(childUri) } returns childFiles
        coEvery { repository.getGrantedUriCount() } returns 1

        val vm = createViewModel()
        vm.onFolderGranted(rootUri)
        advanceUntilIdle()
        vm.navigateTo(childUri)
        advanceUntilIdle()

        val childState = vm.uiState.value as FileExplorerUiState.Browsing
        assertTrue(childState.canGoUp)

        vm.navigateUp()
        advanceUntilIdle()

        val parentState = vm.uiState.value as FileExplorerUiState.Browsing
        assertFalse(parentState.canGoUp)
    }

    @Test
    fun `navigateUp returns false at root`() = runTest {
        val rootUri = testUri("root")
        coEvery { browseFiles(rootUri) } returns emptyList()
        coEvery { repository.getGrantedUriCount() } returns 1

        val vm = createViewModel()
        vm.onFolderGranted(rootUri)
        advanceUntilIdle()

        val result = vm.navigateUp()
        assertFalse(result)
    }

    @Test
    fun `navigateTo clears selection`() = runTest {
        val rootUri = testUri("root")
        val childUri = testUri("child")
        coEvery { browseFiles(rootUri) } returns listOf(fileNode("a.jpg"))
        coEvery { browseFiles(childUri) } returns emptyList()
        coEvery { repository.getGrantedUriCount() } returns 1

        val vm = createViewModel()
        vm.onFolderGranted(rootUri)
        advanceUntilIdle()
        vm.selectAll()
        advanceUntilIdle()

        assertEquals(1, (vm.uiState.value as FileExplorerUiState.Browsing).selectedCount)

        vm.navigateTo(childUri)
        advanceUntilIdle()

        assertEquals(0, (vm.uiState.value as FileExplorerUiState.Browsing).selectedCount)
    }

    @Test
    fun `folder load failure emits Error state`() = runTest {
        val folderUri = testUri("folder")
        coEvery { browseFiles(folderUri) } throws RuntimeException("Permission denied")
        coEvery { repository.getGrantedUriCount() } returns 1

        val vm = createViewModel()
        vm.onFolderGranted(folderUri)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is FileExplorerUiState.Error)
        assertTrue((state as FileExplorerUiState.Error).recoverable)
    }

    @Test
    fun `stale permission is removed and PermissionRequired emitted`() = runTest {
        val staleUri = testUri("stale")
        coEvery { repository.getStalePermissions() } returns listOf(staleUri)
        coEvery { repository.revokeUriPermission(staleUri) } returns Unit
        coEvery { repository.getGrantedUriCount() } returns 0

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is FileExplorerUiState.PermissionRequired)
    }
}
