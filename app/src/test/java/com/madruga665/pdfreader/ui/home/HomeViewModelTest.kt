package com.madruga665.pdfreader.ui.home

import android.net.Uri
import app.cash.turbine.test
import com.madruga665.pdfreader.core.home.FavoriteFoldersRepository
import com.madruga665.pdfreader.core.home.RecentsRepository
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.FolderPdfItem
import com.madruga665.pdfreader.core.model.RecentDocument
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var recentsRepository: RecentsRepository
    private lateinit var favoriteFoldersRepository: FavoriteFoldersRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Uri::class)
        val defaultMockUri = mockk<Uri>(relaxed = true)
        every { Uri.parse(any()) } returns defaultMockUri

        recentsRepository = mockk(relaxed = true)
        favoriteFoldersRepository = mockk(relaxed = true)

        coEvery { recentsRepository.getRecentDocuments() } returns flowOf(emptyList())
        coEvery { favoriteFoldersRepository.getFavoriteFolders() } returns flowOf(emptyList())

        viewModel = HomeViewModel(
            recentsRepository = recentsRepository,
            favoriteFoldersRepository = favoriteFoldersRepository
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
        Dispatchers.resetMain()
    }

    @Test
    fun openDocument_emitsNavigateToReaderSideEffect() = runTest {
        val uri = mockk<Uri>()

        viewModel.sideEffects.test {
            viewModel.onIntent(HomeIntent.OpenDocument(uri))
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is HomeSideEffect.NavigateToReader)
            assertEquals(uri, (effect as HomeSideEffect.NavigateToReader).uri)
        }
    }

    @Test
    fun removeRecent_callsRepository() = runTest {
        viewModel.onIntent(HomeIntent.RemoveRecent("content://pdf/1"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { recentsRepository.removeRecentDocument("content://pdf/1") }
    }

    @Test
    fun selectFolder_loadsContainedPdfs() = runTest {
        val folderUri = mockk<Uri>()
        val folder = FavoriteFolder(
            treeUri = "content://tree/folder",
            displayName = "My Books",
            cachedFileCount = 2
        )
        val pdfList = listOf(
            FolderPdfItem(uri = "content://pdf/1", fileName = "Book1.pdf"),
            FolderPdfItem(uri = "content://pdf/2", fileName = "Book2.pdf")
        )
        coEvery { favoriteFoldersRepository.scanFolderPdfs(any()) } returns Result.success(pdfList)

        viewModel.onIntent(HomeIntent.SelectFolder(folder))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(folder, viewModel.viewState.value.activeFolder)
        assertEquals(2, viewModel.viewState.value.activeFolderPdfs.size)
    }

    @Test
    fun deselectFolder_clearsActiveFolder() = runTest {
        viewModel.onIntent(HomeIntent.SelectFolder(null))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.viewState.value.activeFolder)
        assertTrue(viewModel.viewState.value.activeFolderPdfs.isEmpty())
    }

    @Test
    fun exportBackup_success_emitsSummaryToastAndClearsLoading() = runTest {
        val backupRepo = mockk<com.madruga665.pdfreader.core.backup.BackupRepository>()
        val summary = com.madruga665.pdfreader.core.backup.BackupSummary(
            exportedAt = 1764100000000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 120f,
            sessionsCount = 4,
            customSpeedsCount = 2,
            recentsCount = 3,
            favoriteFoldersCount = 1
        )
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://backup/export.json"
        coEvery { backupRepo.exportBackup("content://backup/export.json") } returns
            Result.success(com.madruga665.pdfreader.core.backup.BackupExportResult(summary))

        val vm = HomeViewModel(
            recentsRepository = recentsRepository,
            favoriteFoldersRepository = favoriteFoldersRepository,
            backupRepository = backupRepo
        )

        vm.sideEffects.test {
            vm.onIntent(HomeIntent.ExportBackupToUri(uri))
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is HomeSideEffect.ShowToast)
            val toast = effect as HomeSideEffect.ShowToast
            assertTrue(toast.isLong)
            val msg = toast.message
            assertTrue(msg.contains("4"))
            assertTrue(msg.contains("3"))
            assertTrue(msg.contains("1"))
            assertEquals(false, vm.viewState.value.isBackupInProgress)
        }
    }

    @Test
    fun inspectBackup_validFile_opensPreviewWithMergeDefaultAndConfirmRestores() = runTest {
        val backupRepo = mockk<com.madruga665.pdfreader.core.backup.BackupRepository>()
        val payload = com.madruga665.pdfreader.core.backup.BackupPayload(
            exportedAt = 1764100000000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 120f
        )
        val summary = payload.toSummary()
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://backup/in.json"

        coEvery { backupRepo.previewBackup("content://backup/in.json") } returns
            com.madruga665.pdfreader.core.backup.BackupParseResult.Success(payload, summary)
        coEvery {
            backupRepo.restoreBackup(payload, com.madruga665.pdfreader.core.backup.RestoreMode.REPLACE)
        } returns Result.success(
            com.madruga665.pdfreader.core.backup.BackupRestoreResult(
                mode = com.madruga665.pdfreader.core.backup.RestoreMode.REPLACE,
                summary = summary
            )
        )

        val vm = HomeViewModel(
            recentsRepository = recentsRepository,
            favoriteFoldersRepository = favoriteFoldersRepository,
            backupRepository = backupRepo
        )

        vm.onIntent(HomeIntent.InspectBackupUri(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        val preview = vm.viewState.value.pendingRestorePreview
        assertTrue(preview != null)
        assertEquals(com.madruga665.pdfreader.core.backup.RestoreMode.MERGE, preview!!.selectedMode)

        vm.onIntent(HomeIntent.SelectRestoreMode(com.madruga665.pdfreader.core.backup.RestoreMode.REPLACE))
        assertEquals(
            com.madruga665.pdfreader.core.backup.RestoreMode.REPLACE,
            vm.viewState.value.pendingRestorePreview?.selectedMode
        )

        vm.sideEffects.test {
            vm.onIntent(HomeIntent.ConfirmRestore)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                backupRepo.restoreBackup(payload, com.madruga665.pdfreader.core.backup.RestoreMode.REPLACE)
            }
            assertNull(vm.viewState.value.pendingRestorePreview)
            val effect = awaitItem()
            assertTrue(effect is HomeSideEffect.ShowToast)
        }
    }

    @Test
    fun inspectBackup_invalidFile_setsErrorMessageAndDoesNotOpenPreview() = runTest {
        val backupRepo = mockk<com.madruga665.pdfreader.core.backup.BackupRepository>()
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://backup/invalid.json"

        coEvery { backupRepo.previewBackup("content://backup/invalid.json") } returns
            com.madruga665.pdfreader.core.backup.BackupParseResult.Failure(
                reason = com.madruga665.pdfreader.core.backup.BackupErrorReason.INVALID_JSON,
                message = "Arquivo JSON inválido ou corrompido."
            )

        val vm = HomeViewModel(
            recentsRepository = recentsRepository,
            favoriteFoldersRepository = favoriteFoldersRepository,
            backupRepository = backupRepo
        )

        vm.onIntent(HomeIntent.InspectBackupUri(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.viewState.value.pendingRestorePreview)
        assertEquals("Arquivo JSON inválido ou corrompido.", vm.viewState.value.errorMessage)
    }

    @Test
    fun dismissRestorePreview_clearsPreviewWithoutCallingRestore() = runTest {
        val backupRepo = mockk<com.madruga665.pdfreader.core.backup.BackupRepository>(relaxed = true)
        val payload = com.madruga665.pdfreader.core.backup.BackupPayload(
            exportedAt = 1764100000000L,
            appVersion = "1.0.0"
        )
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://backup/in.json"
        coEvery { backupRepo.previewBackup("content://backup/in.json") } returns
            com.madruga665.pdfreader.core.backup.BackupParseResult.Success(payload, payload.toSummary())

        val vm = HomeViewModel(
            recentsRepository = recentsRepository,
            favoriteFoldersRepository = favoriteFoldersRepository,
            backupRepository = backupRepo
        )

        vm.onIntent(HomeIntent.InspectBackupUri(uri))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.viewState.value.pendingRestorePreview != null)

        vm.onIntent(HomeIntent.DismissRestorePreview)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.viewState.value.pendingRestorePreview)
        coVerify(exactly = 0) { backupRepo.restoreBackup(any(), any()) }
    }

    @Test
    fun selectFolder_permissionFailure_clearsActiveFolderAndSetsError() = runTest {
        val folder = FavoriteFolder(
            treeUri = "content://tree/revoked",
            displayName = "Revoked Folder",
            cachedFileCount = 3
        )
        coEvery { favoriteFoldersRepository.scanFolderPdfs(any()) } returns
            Result.failure(SecurityException("Permission revoked"))

        viewModel.onIntent(HomeIntent.SelectFolder(folder))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.viewState.value.activeFolder)
        assertTrue(viewModel.viewState.value.activeFolderPdfs.isEmpty())
        assertEquals(false, viewModel.viewState.value.isLoading)
        assertTrue(viewModel.viewState.value.errorMessage?.contains("Permission revoked") == true)
    }

    @Test
    fun exportBackup_failure_setsErrorMessageAndClearsProgress() = runTest {
        val backupRepo = mockk<com.madruga665.pdfreader.core.backup.BackupRepository>()
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://backup/fail.json"
        coEvery { backupRepo.exportBackup("content://backup/fail.json") } returns
            Result.failure(java.io.IOException("Storage full"))

        val vm = HomeViewModel(
            recentsRepository = recentsRepository,
            favoriteFoldersRepository = favoriteFoldersRepository,
            backupRepository = backupRepo
        )

        vm.onIntent(HomeIntent.ExportBackupToUri(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, vm.viewState.value.isBackupInProgress)
        assertTrue(vm.viewState.value.errorMessage?.contains("Storage full") == true)
    }

    @Test
    fun confirmRestore_mergeMode_emitsLongToastAndCallsMergeRestore() = runTest {
        val backupRepo = mockk<com.madruga665.pdfreader.core.backup.BackupRepository>()
        val payload = com.madruga665.pdfreader.core.backup.BackupPayload(
            exportedAt = 1764100000000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 110f
        )
        val summary = payload.toSummary()
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://backup/merge.json"

        coEvery { backupRepo.previewBackup("content://backup/merge.json") } returns
            com.madruga665.pdfreader.core.backup.BackupParseResult.Success(payload, summary)
        coEvery {
            backupRepo.restoreBackup(payload, com.madruga665.pdfreader.core.backup.RestoreMode.MERGE)
        } returns Result.success(
            com.madruga665.pdfreader.core.backup.BackupRestoreResult(
                mode = com.madruga665.pdfreader.core.backup.RestoreMode.MERGE,
                summary = summary
            )
        )

        val vm = HomeViewModel(
            recentsRepository = recentsRepository,
            favoriteFoldersRepository = favoriteFoldersRepository,
            backupRepository = backupRepo
        )

        vm.onIntent(HomeIntent.InspectBackupUri(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.sideEffects.test {
            vm.onIntent(HomeIntent.ConfirmRestore)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                backupRepo.restoreBackup(payload, com.madruga665.pdfreader.core.backup.RestoreMode.MERGE)
            }
            val effect = awaitItem()
            assertTrue(effect is HomeSideEffect.ShowToast)
            val toast = effect as HomeSideEffect.ShowToast
            assertTrue(toast.isLong)
            assertTrue(toast.message.contains("Mesclar"))
        }
    }

    @Test
    fun confirmRestore_failure_setsErrorMessageAndClearsProgress() = runTest {
        val backupRepo = mockk<com.madruga665.pdfreader.core.backup.BackupRepository>()
        val payload = com.madruga665.pdfreader.core.backup.BackupPayload(
            exportedAt = 1764100000000L,
            appVersion = "1.0.0"
        )
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://backup/err.json"

        coEvery { backupRepo.previewBackup("content://backup/err.json") } returns
            com.madruga665.pdfreader.core.backup.BackupParseResult.Success(payload, payload.toSummary())
        coEvery {
            backupRepo.restoreBackup(payload, com.madruga665.pdfreader.core.backup.RestoreMode.MERGE)
        } returns Result.failure(IllegalStateException("DataStore write failed"))

        val vm = HomeViewModel(
            recentsRepository = recentsRepository,
            favoriteFoldersRepository = favoriteFoldersRepository,
            backupRepository = backupRepo
        )

        vm.onIntent(HomeIntent.InspectBackupUri(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onIntent(HomeIntent.ConfirmRestore)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.viewState.value.pendingRestorePreview)
        assertEquals(false, vm.viewState.value.isBackupInProgress)
        assertTrue(vm.viewState.value.errorMessage?.contains("DataStore write failed") == true)
    }
}
