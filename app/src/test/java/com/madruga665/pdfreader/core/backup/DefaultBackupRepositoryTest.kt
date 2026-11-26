package com.madruga665.pdfreader.core.backup

import com.madruga665.pdfreader.core.home.FavoriteFoldersRepository
import com.madruga665.pdfreader.core.home.RecentsRepository
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.ReadingSession
import com.madruga665.pdfreader.core.model.RecentDocument
import com.madruga665.pdfreader.core.storage.ReadingSessionStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultBackupRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionStore: ReadingSessionStore
    private lateinit var recentsRepository: RecentsRepository
    private lateinit var favoriteFoldersRepository: FavoriteFoldersRepository
    private lateinit var fileGateway: BackupFileGateway
    private lateinit var repository: DefaultBackupRepository

    @Before
    fun setUp() {
        sessionStore = mockk(relaxed = true)
        recentsRepository = mockk(relaxed = true)
        favoriteFoldersRepository = mockk(relaxed = true)
        fileGateway = mockk(relaxed = true)

        coEvery { sessionStore.getGlobalDefaultSpeed() } returns flowOf(115f)
        coEvery { sessionStore.getAllSessionsSnapshot() } returns listOf(
            ReadingSession(
                documentUri = "content://pdf/1",
                currentPageIndex = 4,
                normalizedScrollRatio = 0.5f,
                zoomScale = 1.2f,
                preferredSpeedPxPerSec = 120f,
                customDefaultSpeedPxPerSec = 160f,
                lastReadTimestamp = 5000L,
                fileName = "" // should be enriched from recents
            )
        )
        coEvery { recentsRepository.getRecentDocuments() } returns flowOf(
            listOf(
                RecentDocument(
                    uri = "content://pdf/1",
                    fileName = "enriched.pdf",
                    pageCount = 20,
                    lastReadPageIndex = 4,
                    progressRatio = 0.5f,
                    lastAccessedTimestamp = 5000L
                )
            )
        )
        coEvery { favoriteFoldersRepository.getFavoriteFolders() } returns flowOf(
            listOf(
                FavoriteFolder(
                    treeUri = "content://tree/1",
                    displayName = "Study",
                    pathSummary = "primary:Study",
                    pinnedTimestamp = 4000L,
                    cachedFileCount = 3
                )
            )
        )

        repository = DefaultBackupRepository(
            sessionStore = sessionStore,
            recentsRepository = recentsRepository,
            favoriteFoldersRepository = favoriteFoldersRepository,
            fileGateway = fileGateway,
            appVersion = "1.0.0",
            clock = { 1764100000000L },
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun exportBackup_enrichesSessionFileNameAndWritesValidJson() = runTest(testDispatcher) {
        var writtenContent = ""
        coEvery { fileGateway.writeText("content://backup/out.json", any()) } answers {
            writtenContent = secondArg()
            Result.success(Unit)
        }

        val result = repository.exportBackup("content://backup/out.json")

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow().summary
        assertEquals(1, summary.sessionsCount)
        assertEquals(1, summary.customSpeedsCount)
        assertEquals(1, summary.recentsCount)
        assertEquals(1, summary.favoriteFoldersCount)

        val decoded = BackupJsonCodec.decode(writtenContent) as BackupParseResult.Success
        assertEquals("enriched.pdf", decoded.payload.readingSessions.single().fileName)
    }

    @Test
    fun previewBackup_invalidJson_returnsFailureWithoutTouchingStores() = runTest(testDispatcher) {
        coEvery { fileGateway.readText("content://backup/bad.json") } returns Result.success("{ broken")

        val result = repository.previewBackup("content://backup/bad.json")

        assertTrue(result is BackupParseResult.Failure)
        assertEquals(BackupErrorReason.INVALID_JSON, (result as BackupParseResult.Failure).reason)
        coVerify(exactly = 0) { sessionStore.replaceAllData(any(), any()) }
        coVerify(exactly = 0) { recentsRepository.replaceAllRecents(any()) }
        coVerify(exactly = 0) { favoriteFoldersRepository.replaceAllFavoriteFolders(any()) }
    }

    @Test
    fun restoreBackup_replaceMode_updatesAllThreeStores() = runTest(testDispatcher) {
        val incoming = BackupPayload(
            exportedAt = 1764100000000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 140f,
            readingSessions = listOf(
                BackupReadingSession(
                    documentUri = "content://pdf/restored",
                    fileName = "restored.pdf",
                    currentPageIndex = 2,
                    normalizedScrollRatio = 0.25f,
                    zoomScale = 1.0f,
                    preferredSpeedPxPerSec = 140f,
                    customDefaultSpeedPxPerSec = 180f,
                    lastReadTimestamp = 8000L
                )
            ),
            recentDocuments = listOf(
                RecentDocument("content://pdf/restored", "restored.pdf", 10, 2, 0.25f, 8000L)
            ),
            favoriteFolders = listOf(
                FavoriteFolder("content://tree/restored", "Restored", "", 8000L, 1)
            )
        )

        val result = repository.restoreBackup(incoming, RestoreMode.REPLACE)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            sessionStore.replaceAllData(140f, match { it.size == 1 && it.first().documentUri == "content://pdf/restored" })
        }
        coVerify(exactly = 1) {
            recentsRepository.replaceAllRecents(match { it.size == 1 && it.first().uri == "content://pdf/restored" })
        }
        coVerify(exactly = 1) {
            favoriteFoldersRepository.replaceAllFavoriteFolders(match { it.size == 1 && it.first().treeUri == "content://tree/restored" })
        }
    }

    @Test
    fun restoreBackup_mergeMode_combinesLocalAndIncomingAcrossAllStores() = runTest(testDispatcher) {
        val incoming = BackupPayload(
            exportedAt = 1764200000000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 180f,
            readingSessions = listOf(
                BackupReadingSession(
                    documentUri = "content://pdf/2",
                    fileName = "incoming.pdf",
                    currentPageIndex = 6,
                    normalizedScrollRatio = 0.6f,
                    zoomScale = 1.25f,
                    preferredSpeedPxPerSec = 150f,
                    customDefaultSpeedPxPerSec = 175f,
                    lastReadTimestamp = 9000L
                )
            ),
            recentDocuments = listOf(
                RecentDocument("content://pdf/2", "incoming.pdf", 30, 6, 0.2f, 9000L)
            ),
            favoriteFolders = listOf(
                FavoriteFolder("content://tree/2", "Work", "primary:Work", 9000L, 4)
            )
        )

        val result = repository.restoreBackup(incoming, RestoreMode.MERGE)

        assertTrue(result.isSuccess)
        val restored = result.getOrThrow()
        assertEquals(RestoreMode.MERGE, restored.mode)
        assertEquals(2, restored.summary.sessionsCount)
        assertEquals(2, restored.summary.customSpeedsCount)
        assertEquals(2, restored.summary.recentsCount)
        assertEquals(2, restored.summary.favoriteFoldersCount)

        coVerify(exactly = 1) {
            sessionStore.replaceAllData(115f, match { it.size == 2 })
        }
        coVerify(exactly = 1) {
            recentsRepository.replaceAllRecents(match { it.size == 2 })
        }
        coVerify(exactly = 1) {
            favoriteFoldersRepository.replaceAllFavoriteFolders(match { it.size == 2 })
        }
    }

    @Test
    fun exportBackup_ioFailure_returnsFailure() = runTest(testDispatcher) {
        coEvery { fileGateway.writeText("content://backup/fail.json", any()) } returns
            Result.failure(java.io.IOException("Disk full"))

        val result = repository.exportBackup("content://backup/fail.json")

        assertTrue(result.isFailure)
        assertEquals("Disk full", result.exceptionOrNull()?.message)
    }

    @Test
    fun previewBackup_ioFailure_returnsIoErrorWithoutTouchingStores() = runTest(testDispatcher) {
        coEvery { fileGateway.readText("content://backup/unreadable.json") } returns
            Result.failure(java.io.IOException("Permission denied"))

        val result = repository.previewBackup("content://backup/unreadable.json")

        assertTrue(result is BackupParseResult.Failure)
        val failure = result as BackupParseResult.Failure
        assertEquals(BackupErrorReason.IO_ERROR, failure.reason)
        assertEquals("Permission denied", failure.message)
        coVerify(exactly = 0) { sessionStore.replaceAllData(any(), any()) }
        coVerify(exactly = 0) { recentsRepository.replaceAllRecents(any()) }
        coVerify(exactly = 0) { favoriteFoldersRepository.replaceAllFavoriteFolders(any()) }
    }

    @Test
    fun exportBackup_emptyState_writesValidEmptyPayload() = runTest(testDispatcher) {
        coEvery { sessionStore.getGlobalDefaultSpeed() } returns flowOf(90f)
        coEvery { sessionStore.getAllSessionsSnapshot() } returns emptyList()
        coEvery { recentsRepository.getRecentDocuments() } returns flowOf(emptyList())
        coEvery { favoriteFoldersRepository.getFavoriteFolders() } returns flowOf(emptyList())

        var writtenJson = ""
        coEvery { fileGateway.writeText("content://backup/empty.json", any()) } answers {
            writtenJson = secondArg()
            Result.success(Unit)
        }

        val result = repository.exportBackup("content://backup/empty.json")

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow().summary
        assertEquals(0, summary.sessionsCount)
        assertEquals(0, summary.customSpeedsCount)
        assertEquals(0, summary.recentsCount)
        assertEquals(0, summary.favoriteFoldersCount)
        assertEquals(90f, summary.globalDefaultSpeedPxPerSec, 0.001f)

        val decoded = BackupJsonCodec.decode(writtenJson)
        assertTrue(decoded is BackupParseResult.Success)
    }
}
