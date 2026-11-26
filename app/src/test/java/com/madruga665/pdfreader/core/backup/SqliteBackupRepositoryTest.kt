package com.madruga665.pdfreader.core.backup

import com.madruga665.pdfreader.core.database.JdbcSqliteDriver
import com.madruga665.pdfreader.core.database.PdfReaderDatabaseSchema
import com.madruga665.pdfreader.core.database.SqliteAppDatabase
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import com.madruga665.pdfreader.core.storage.SqlitePdfDocumentStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class SqliteBackupRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var database: SqliteAppDatabase
    private lateinit var store: SqlitePdfDocumentStore
    private lateinit var stagingDir: File
    private lateinit var exportedFiles: MutableMap<String, File>
    private lateinit var gateway: FakeBackupFileGateway
    private lateinit var repository: SqliteBackupRepository

    @Before
    fun setUp() {
        val dbFile = tempFolder.newFile("local_app.db")
        database = SqliteAppDatabase(JdbcSqliteDriver(dbFile))
        store = SqlitePdfDocumentStore(database)
        stagingDir = tempFolder.newFolder("staging")
        exportedFiles = mutableMapOf()
        gateway = FakeBackupFileGateway(exportedFiles, tempFolder.newFolder("saf_storage"))
        repository = SqliteBackupRepository(
            database = database,
            documentStore = store,
            validator = SqliteBackupValidator(database),
            fileGateway = gateway,
            stagingDirectory = stagingDir,
            appVersion = "1.0.0"
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `exportBackup writes valid sqlite db file and returns exact summary`() = runTest {
        store.setGlobalDefaultSpeed(140.0f)
        val doc = store.registerOrOpenDocument("content://pdf/book.pdf", "book.pdf", 12, 1000L)
        store.saveCustomDocumentSpeed(doc.documentUri, 180.0f)
        store.upsertFavoriteFolder(FavoriteFolder("content://tree/books", "Books", "", 2000L, 3))

        val exportResult = repository.exportBackup("content://saf/backup.db")
        assertTrue(exportResult.isSuccess)
        val summary = exportResult.getOrThrow().summary
        assertEquals(140.0f, summary.globalDefaultSpeedPxPerSec, 0.001f)
        assertEquals(1, summary.sessionsCount)
        assertEquals(1, summary.customSpeedsCount)
        assertEquals(1, summary.recentsCount)
        assertEquals(1, summary.favoriteFoldersCount)

        val exportedFile = exportedFiles["content://saf/backup.db"]!!
        assertTrue(exportedFile.exists() && exportedFile.length() > 0L)

        // Verify exported file passes validator & PRAGMA integrity_check
        val preview = repository.previewBackup("content://saf/backup.db")
        assertTrue(preview is BackupParseResult.Success)
        assertEquals(1, (preview as BackupParseResult.Success).summary.sessionsCount)
        // Staging temp files must be cleaned up
        assertTrue(stagingDir.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `restoreBackup REPLACE replaces local database and guarantees datastore_migration_completed is 1`() = runTest {
        store.setGlobalDefaultSpeed(110.0f)
        store.registerOrOpenDocument("content://pdf/local.pdf", "local.pdf", 5, 1000L)

        val incomingDoc = PdfDocumentRecord.createSanitized(
            id = "22222222-2222-4222-8222-222222222222",
            documentUri = "content://pdf/remote.pdf",
            fileName = "remote.pdf",
            pageCount = 15,
            currentPageIndex = 6,
            customDefaultSpeedPxPerSec = 175.0f,
            lastAccessedAt = 5000L
        )
        val incomingPayload = BackupPayload(
            exportedAt = 6000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 155.0f,
            documents = listOf(incomingDoc),
            favoriteFolders = listOf(FavoriteFolder("content://tree/remote", "Remote", "", 5000L, 2))
        )

        val result = repository.restoreBackup(incomingPayload, RestoreMode.REPLACE)
        assertTrue(result.isSuccess)

        assertEquals(155.0f, store.getGlobalDefaultSpeed().first(), 0.001f)
        val docs = store.getAllDocumentsSnapshot()
        assertEquals(1, docs.size)
        assertEquals("22222222-2222-4222-8222-222222222222", docs[0].id)
        assertEquals(175.0f, docs[0].customDefaultSpeedPxPerSec ?: 0f, 0.001f)
        assertEquals(listOf("content://tree/remote"), store.getFavoriteFoldersSnapshot().map { it.treeUri })
        assertTrue(store.isDataStoreMigrationCompleted())
    }

    @Test
    fun `restoreBackup MERGE preserves local records and avoids duplicating ids or uris`() = runTest {
        store.setGlobalDefaultSpeed(125.0f)
        val localShared = store.registerOrOpenDocument("content://pdf/shared.pdf", "shared.pdf", 20, 9000L)
        store.updateReadingProgress(localShared.id, 10, 0.5f, 1.4f, 125.0f, 9000L)
        store.registerOrOpenDocument("content://pdf/local_only.pdf", "local_only.pdf", 4, 3000L)

        val incomingShared = PdfDocumentRecord.createSanitized(
            id = "33333333-3333-4333-8333-333333333333",
            documentUri = "content://pdf/shared.pdf",
            fileName = "shared.pdf",
            pageCount = 20,
            currentPageIndex = 3,
            customDefaultSpeedPxPerSec = 190.0f, // older, but has custom speed
            lastAccessedAt = 5000L
        )
        val incomingNew = PdfDocumentRecord.createSanitized(
            id = "44444444-4444-4444-8444-444444444444",
            documentUri = "content://pdf/new_from_backup.pdf",
            fileName = "new_from_backup.pdf",
            pageCount = 8,
            currentPageIndex = 2,
            customDefaultSpeedPxPerSec = 165.0f,
            lastAccessedAt = 8000L
        )

        val incomingPayload = BackupPayload(
            exportedAt = 9500L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 180.0f,
            documents = listOf(incomingShared, incomingNew),
            favoriteFolders = emptyList()
        )

        val result = repository.restoreBackup(incomingPayload, RestoreMode.MERGE)
        assertTrue(result.isSuccess)

        val docs = store.getAllDocumentsSnapshot()
        assertEquals(3, docs.size)
        val shared = store.getByUri("content://pdf/shared.pdf")!!
        assertEquals(10, shared.currentPageIndex) // kept newer local progress
        assertEquals(190.0f, shared.customDefaultSpeedPxPerSec ?: 0f, 0.001f) // adopted custom speed
        assertEquals(125.0f, store.getGlobalDefaultSpeed().first(), 0.001f) // preserved customized local global speed
        assertTrue(store.isDataStoreMigrationCompleted())
    }

    @Test
    fun `previewBackup rejects invalid file without modifying local database`() = runTest {
        val original = store.registerOrOpenDocument("content://pdf/keep.pdf", "keep.pdf", 7, 1000L)
        val badFile = File(tempFolder.root, "not_db.json").apply { writeText("""{"a":1}""") }
        exportedFiles["content://saf/bad.json"] = badFile

        val result = repository.previewBackup("content://saf/bad.json")
        assertTrue(result is BackupParseResult.Failure)
        assertEquals(BackupErrorReason.INVALID_SQLITE_FILE, (result as BackupParseResult.Failure).reason)

        // Local database untouched
        assertEquals(listOf(original.id), store.getAllDocumentsSnapshot().map { it.id })
        assertTrue(stagingDir.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `suggestedBackupFileName ends with db extension and contains no json`() {
        val name = SqliteBackupRepository.suggestedBackupFileName()
        assertTrue(name.startsWith("pdf-reader-backup-"))
        assertTrue(name.endsWith(".db"))
        assertFalse(name.contains("json", ignoreCase = true))
    }

    @Test
    fun `restored document with inaccessible uri can be marked unavailable and relinked preserving custom speed`() = runTest {
        val remoteDoc = PdfDocumentRecord.createSanitized(
            id = "55555555-5555-4555-8555-555555555555",
            documentUri = "content://old_device/manual.pdf",
            fileName = "manual.pdf",
            pageCount = 10,
            currentPageIndex = 6,
            normalizedScrollRatio = 0.6f,
            zoomScale = 1.5f,
            customDefaultSpeedPxPerSec = 175.0f,
            lastAccessedAt = 5000L,
            isAvailable = true
        )
        val payload = BackupPayload(
            exportedAt = 6000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 120.0f,
            documents = listOf(remoteDoc)
        )

        repository.restoreBackup(payload, RestoreMode.REPLACE).getOrThrow()

        // Device B cannot read old_device URI -> marked unavailable preserving all settings
        store.updateAvailabilityBatch(mapOf("content://old_device/manual.pdf" to false))
        val unavailable = store.getById(remoteDoc.id)!!
        assertFalse(unavailable.isAvailable)
        assertEquals(175.0f, unavailable.customDefaultSpeedPxPerSec ?: 0f, 0.001f)

        // User relinks via "Restaurar PDF" to new local URI with 4 pages
        val relinked = store.relinkDocument(
            targetDocumentId = remoteDoc.id,
            newUri = "content://new_device/manual_moved.pdf",
            newFileName = "manual_moved.pdf",
            newPageCount = 4
        ).getOrThrow()

        assertEquals(remoteDoc.id, relinked.id)
        assertTrue(relinked.isAvailable)
        assertEquals(3, relinked.currentPageIndex) // min(6, 4 - 1)
        assertEquals(175.0f, relinked.customDefaultSpeedPxPerSec ?: 0f, 0.001f)
        assertEquals(1.5f, relinked.zoomScale, 0.001f)
    }

    private class FakeBackupFileGateway(
        private val filesByUri: MutableMap<String, File>,
        private val storageDir: File
    ) : BackupFileGateway {
        override suspend fun writeFileToUri(uriString: String, sourceFile: File): Result<Unit> = runCatching {
            val dest = File(storageDir, "exported_${uriString.hashCode()}.db")
            sourceFile.copyTo(dest, overwrite = true)
            filesByUri[uriString] = dest
        }

        override suspend fun copyUriToTempFile(
            uriString: String,
            destinationFile: File,
            maxBytes: Long
        ): Result<Unit> = runCatching {
            val source = filesByUri[uriString] ?: throw IOException("File not found: $uriString")
            if (source.length() > maxBytes) throw IOException("File too large")
            source.copyTo(destinationFile, overwrite = true)
        }
    }
}
