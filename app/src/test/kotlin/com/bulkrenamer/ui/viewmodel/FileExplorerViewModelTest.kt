package com.bulkrenamer.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.lifecycle.SavedStateHandle
import com.bulkrenamer.data.model.FileNode
import com.bulkrenamer.data.model.RenameResult
import com.bulkrenamer.data.model.RenameRule
import com.bulkrenamer.data.repository.FileSystemRepository
import com.bulkrenamer.domain.BrowseFilesUseCase
import com.bulkrenamer.domain.RenameFilesUseCase
import com.bulkrenamer.domain.UndoOperationUseCase
import com.bulkrenamer.domain.UndoResult
import com.bulkrenamer.service.RenameProgressState
import com.bulkrenamer.ui.state.FileExplorerUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FileExplorerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var browseFiles: BrowseFilesUseCase
    private lateinit var repository: FileSystemRepository
    private lateinit var renameFilesUseCase: RenameFilesUseCase
    private lateinit var undoOperation: UndoOperationUseCase
    private lateinit var savedStateHandle: SavedStateHandle

    private val progressFlow = MutableStateFlow<RenameProgressState>(RenameProgressState.Idle)

    private val testRootPath = "/storage/emulated/0"

    private fun fileUri(path: String): Uri {
        val uri = mockk<Uri>(relaxed = true)
        every { uri.path } returns path
        every { uri.toString() } returns "file://$path"
        return uri
    }

    private fun fileNode(name: String, path: String = "$testRootPath/$name", isDir: Boolean = false): FileNode {
        val uri = fileUri(path)
        return FileNode(
            documentId = path,
            treeUri = fileUri(testRootPath),
            uri = uri,
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
        context = mockk(relaxed = true)
        browseFiles = mockk()
        repository = mockk()
        renameFilesUseCase = mockk()
        undoOperation = mockk()
        savedStateHandle = SavedStateHandle()

        every { renameFilesUseCase.progress } returns progressFlow

        // Mock permission check: always granted so tests can browse
        mockkStatic(Environment::class)
        every { Environment.isExternalStorageManager() } returns true
        every { Environment.getExternalStorageDirectory() } returns File(testRootPath)

        // Mock Build.VERSION.SDK_INT >= R
        mockkStatic(Build.VERSION::class)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): FileExplorerViewModel =
        FileExplorerViewModel(context, browseFiles, repository, renameFilesUseCase, undoOperation, savedStateHandle)

    @Test
    fun `initial state loads root folder when permission granted`() = runTest {
        coEvery { browseFiles(testRootPath) } returns emptyList()

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is FileExplorerUiState.Browsing)
    }

    @Test
    fun `onFolderGranted replaced by onPermissionGranted — navigates to root`() = runTest {
        coEvery { browseFiles(testRootPath) } returns listOf(fileNode("a.jpg"))

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onPermissionGranted()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is FileExplorerUiState.Browsing)
        assertEquals(1, (state as FileExplorerUiState.Browsing).entries.size)
    }

    @Test
    fun `toggleSelection adds and removes file`() = runTest {
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"))
        coEvery { browseFiles(testRootPath) } returns files

        val vm = createViewModel()
        advanceUntilIdle()

        val path = "$testRootPath/a.jpg"
        vm.toggleSelection(path)
        advanceUntilIdle()

        assertEquals(1, (vm.uiState.value as FileExplorerUiState.Browsing).selectedCount)

        vm.toggleSelection(path)
        advanceUntilIdle()

        assertEquals(0, (vm.uiState.value as FileExplorerUiState.Browsing).selectedCount)
    }

    @Test
    fun `selectAll selects only non-directory files`() = runTest {
        val files = listOf(
            fileNode("a.jpg"),
            fileNode("SubDir", isDir = true),
            fileNode("b.jpg")
        )
        coEvery { browseFiles(testRootPath) } returns files

        val vm = createViewModel()
        advanceUntilIdle()
        vm.selectAll()
        advanceUntilIdle()

        val browsing = vm.uiState.value as FileExplorerUiState.Browsing
        assertEquals(2, browsing.selectedCount)
        assertFalse("$testRootPath/SubDir" in browsing.selection)
    }

    @Test
    fun `deselectAll clears selection`() = runTest {
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"))
        coEvery { browseFiles(testRootPath) } returns files

        val vm = createViewModel()
        advanceUntilIdle()
        vm.selectAll()
        advanceUntilIdle()
        vm.deselectAll()
        advanceUntilIdle()

        assertEquals(0, (vm.uiState.value as FileExplorerUiState.Browsing).selectedCount)
    }

    @Test
    fun `previewRename emits RenamePreviewing state`() = runTest {
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"))
        coEvery { browseFiles(testRootPath) } returns files

        val vm = createViewModel()
        advanceUntilIdle()
        vm.selectAll()
        advanceUntilIdle()

        vm.previewRename(listOf(RenameRule.AddPrefix("2024_")))

        val state = vm.uiState.value
        assertTrue(state is FileExplorerUiState.RenamePreviewing)
        assertEquals(2, (state as FileExplorerUiState.RenamePreviewing).selectedCount)
    }

    @Test
    fun `navigateTo goes into subdirectory`() = runTest {
        val childPath = "$testRootPath/DCIM"
        coEvery { browseFiles(testRootPath) } returns listOf(fileNode("DCIM", isDir = true))
        coEvery { browseFiles(childPath) } returns listOf(fileNode("photo.jpg", "$childPath/photo.jpg"))

        val vm = createViewModel()
        advanceUntilIdle()

        vm.navigateTo(childPath)
        advanceUntilIdle()

        val state = vm.uiState.value as FileExplorerUiState.Browsing
        assertTrue(state.canGoUp)
        assertEquals(1, state.entries.size)
    }

    @Test
    fun `navigateUp returns to parent`() = runTest {
        val childPath = "$testRootPath/DCIM"
        coEvery { browseFiles(testRootPath) } returns emptyList()
        coEvery { browseFiles(childPath) } returns emptyList()

        val vm = createViewModel()
        advanceUntilIdle()
        vm.navigateTo(childPath)
        advanceUntilIdle()

        assertTrue((vm.uiState.value as FileExplorerUiState.Browsing).canGoUp)

        vm.navigateUp()
        advanceUntilIdle()

        assertFalse((vm.uiState.value as FileExplorerUiState.Browsing).canGoUp)
    }

    @Test
    fun `navigateUp at root returns false`() = runTest {
        coEvery { browseFiles(testRootPath) } returns emptyList()

        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.navigateUp())
    }

    @Test
    fun `navigateTo clears selection`() = runTest {
        val childPath = "$testRootPath/DCIM"
        coEvery { browseFiles(testRootPath) } returns listOf(fileNode("a.jpg"))
        coEvery { browseFiles(childPath) } returns emptyList()

        val vm = createViewModel()
        advanceUntilIdle()
        vm.selectAll()
        advanceUntilIdle()

        assertEquals(1, (vm.uiState.value as FileExplorerUiState.Browsing).selectedCount)

        vm.navigateTo(childPath)
        advanceUntilIdle()

        assertEquals(0, (vm.uiState.value as FileExplorerUiState.Browsing).selectedCount)
    }

    @Test
    fun `folder load failure emits Error state`() = runTest {
        coEvery { browseFiles(testRootPath) } throws RuntimeException("Permission denied")

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is FileExplorerUiState.Error)
        assertTrue((state as FileExplorerUiState.Error).recoverable)
    }

    @Test
    fun `confirmRename transitions to RenameResult`() = runTest {
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"))
        coEvery { browseFiles(testRootPath) } returns files

        val vm = createViewModel()
        advanceUntilIdle()
        vm.selectAll()
        advanceUntilIdle()
        vm.previewRename(listOf(RenameRule.AddPrefix("new_")))
        advanceUntilIdle()

        val previewState = vm.uiState.value as FileExplorerUiState.RenamePreviewing

        coEvery { renameFilesUseCase.executeBatch(any()) } returns listOf(
            RenameResult(fileUri("$testRootPath/a.jpg"), "a.jpg", "new_a.jpg", fileUri("$testRootPath/new_a.jpg"), null),
            RenameResult(fileUri("$testRootPath/b.jpg"), "b.jpg", "new_b.jpg", fileUri("$testRootPath/new_b.jpg"), null)
        )

        vm.confirmRename(previewState)
        advanceUntilIdle()

        val result = vm.uiState.value as FileExplorerUiState.RenameResult
        assertEquals(2, result.successCount)
        assertEquals(0, result.failureCount)
    }

    @Test
    fun `confirmRename records partial failures`() = runTest {
        val files = listOf(fileNode("a.jpg"), fileNode("b.jpg"))
        coEvery { browseFiles(testRootPath) } returns files

        val vm = createViewModel()
        advanceUntilIdle()
        vm.selectAll()
        advanceUntilIdle()
        vm.previewRename(listOf(RenameRule.AddPrefix("new_")))
        advanceUntilIdle()

        val previewState = vm.uiState.value as FileExplorerUiState.RenamePreviewing

        coEvery { renameFilesUseCase.executeBatch(any()) } returns listOf(
            RenameResult(fileUri("$testRootPath/a.jpg"), "a.jpg", "new_a.jpg", fileUri("$testRootPath/new_a.jpg"), null),
            RenameResult(fileUri("$testRootPath/b.jpg"), "b.jpg", "new_b.jpg", null, Exception("Permission denied"))
        )

        vm.confirmRename(previewState)
        advanceUntilIdle()

        val result = vm.uiState.value as FileExplorerUiState.RenameResult
        assertEquals(1, result.successCount)
        assertEquals(1, result.failureCount)
        assertEquals("b.jpg", result.errors.first().originalName)
    }

    @Test
    fun `undoLastBatch delegates to UndoOperationUseCase and reloads folder`() = runTest {
        coEvery { browseFiles(testRootPath) } returns emptyList()
        coEvery { undoOperation.undoBatch("batch-x") } returns UndoResult(
            batchId = "batch-x", reversedCount = 2, skippedCount = 0, failedCount = 0
        )

        val vm = createViewModel()
        advanceUntilIdle()

        vm.undoLastBatch("batch-x")
        advanceUntilIdle()

        coVerify(exactly = 1) { undoOperation.undoBatch("batch-x") }
        assertTrue(vm.uiState.value is FileExplorerUiState.Browsing)
    }

    @Test
    fun `cancelRename calls cancel on use case`() {
        val vm = createViewModel()
        every { renameFilesUseCase.cancel() } returns Unit

        vm.cancelRename()

        io.mockk.verify(exactly = 1) { renameFilesUseCase.cancel() }
    }
}
