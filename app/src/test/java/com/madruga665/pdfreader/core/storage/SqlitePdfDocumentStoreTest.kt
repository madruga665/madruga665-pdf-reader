package com.madruga665.pdfreader.core.storage

import com.madruga665.pdfreader.core.database.JdbcSqliteDriver
import com.madruga665.pdfreader.core.database.PdfReaderDatabaseSchema
import com.madruga665.pdfreader.core.database.SqliteAppDatabase
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import com.madruga665.pdfreader.core.model.ReadingSession
import com.madruga665.pdfreader.core.model.SpeedPreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.UUID

class SqlitePdfDocumentStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var database: SqliteAppDatabase
    private lateinit var store: SqlitePdfDocumentStore

    @Before
    fun setUp() {
        val dbFile = tempFolder.newFile("pdf_store_test.db")
        database = SqliteAppDatabase(JdbcSqliteDriver(dbFile))
        store = SqlitePdfDocumentStore(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `registerOrOpenDocument creates unique UUID and reuses same id on reopen`() = runTest {
        val uri = "content://docs/book.pdf"
        val first = store.registerOrOpenDocument(uri = uri, fileName = "book.pdf", pageCount = 20, nowMillis = 1000L)

        // Valid UUID v4
        assertNotNull(UUID.fromString(first.id))
        assertEquals(uri, first.documentUri)
        assertEquals("book.pdf", first.fileName)
        assertEquals(20, first.pageCount)
        assertTrue(first.isAvailable)
        assertTrue(first.isInRecents)

        val second = store.registerOrOpenDocument(uri = uri, fileName = "book.pdf", pageCount = 20, nowMillis = 2000L)
        assertEquals(first.id, second.id)
        assertEquals(2000L, second.lastAccessedAt)

        val allDocs = store.observeAllDocuments().first()
        assertEquals(1, allDocs.size)
    }

    @Test
    fun `updateReadingProgress preserves customDefaultSpeedPxPerSec and clamps values per RN-02`() = runTest {
        val uri = "content://docs/study.pdf"
        val created = store.registerOrOpenDocument(uri = uri, fileName = "study.pdf", pageCount = 10, nowMillis = 1000L)

        store.saveCustomDocumentSpeed(uri, 165.0f)

        // Update reading progress with out-of-range values -> must clamp and NOT erase customDefaultSpeedPxPerSec
        store.updateReadingProgress(
            documentId = created.id,
            currentPageIndex = 99,
            normalizedScrollRatio = 2.5f,
            zoomScale = 9.0f,
            preferredSpeedPxPerSec = 999.0f,
            nowMillis = 5000L
        )

        val reloaded = store.getById(created.id)
        assertNotNull(reloaded)
        assertEquals(9, reloaded!!.currentPageIndex) // pageCount - 1
        assertEquals(1.0f, reloaded.normalizedScrollRatio, 0.001f)
        assertEquals(4.0f, reloaded.zoomScale, 0.001f)
        assertEquals(500.0f, reloaded.preferredSpeedPxPerSec, 0.001f)
        assertEquals(165.0f, reloaded.customDefaultSpeedPxPerSec ?: 0f, 0.001f)

        // Also verify saveSession(session) with customDefaultSpeedPxPerSec = null does NOT erase saved custom speed
        store.saveSession(
            ReadingSession(
                documentId = created.id,
                documentUri = uri,
                currentPageIndex = 4,
                normalizedScrollRatio = 0.4f,
                zoomScale = 1.5f,
                preferredSpeedPxPerSec = 140.0f,
                customDefaultSpeedPxPerSec = null,
                lastReadTimestamp = 6000L,
                fileName = "study.pdf"
            )
        )
        val afterSaveSession = store.getById(created.id)!!
        assertEquals(4, afterSaveSession.currentPageIndex)
        assertEquals(1.5f, afterSaveSession.zoomScale, 0.001f)
        assertEquals(165.0f, afterSaveSession.customDefaultSpeedPxPerSec ?: 0f, 0.001f)
    }

    @Test
    fun `clearCustomDocumentSpeed resets only targeted document custom speed`() = runTest {
        val docA = store.registerOrOpenDocument("content://docs/a.pdf", "same.pdf", 5, 1000L)
        val docB = store.registerOrOpenDocument("content://docs/b.pdf", "same.pdf", 5, 1000L)

        store.saveCustomDocumentSpeed(docA.documentUri, 150.0f)
        store.saveCustomDocumentSpeed(docB.documentUri, 200.0f)

        store.clearCustomDocumentSpeed(docA.documentUri)

        assertNull(store.getCustomDocumentSpeed(docA.documentUri).first())
        assertEquals(200.0f, store.getCustomDocumentSpeed(docB.documentUri).first() ?: 0f, 0.001f)
    }

    @Test
    fun `globalDefaultSpeed defaults to 90 and clamps to 10-500`() = runTest {
        assertEquals(SpeedPreset.MEDIUM.speedPxPerSec, store.getGlobalDefaultSpeed().first(), 0.001f)

        store.setGlobalDefaultSpeed(145.0f)
        assertEquals(145.0f, store.getGlobalDefaultSpeed().first(), 0.001f)

        store.setGlobalDefaultSpeed(2.0f)
        assertEquals(10.0f, store.getGlobalDefaultSpeed().first(), 0.001f)

        store.setGlobalDefaultSpeed(800.0f)
        assertEquals(500.0f, store.getGlobalDefaultSpeed().first(), 0.001f)
    }

    @Test
    fun `removeFromRecents deletes unavailable document and hides available document`() = runTest {
        val availableDoc = store.registerOrOpenDocument("content://docs/avail.pdf", "avail.pdf", 5, 1000L)
        val unavailableDoc = store.registerOrOpenDocument("content://docs/missing.pdf", "missing.pdf", 5, 2000L)
        store.setDocumentAvailability(unavailableDoc.documentUri, false)

        store.removeFromRecents(availableDoc.documentUri, deleteIfUnavailable = true)
        store.removeFromRecents(unavailableDoc.documentUri, deleteIfUnavailable = true)

        assertTrue(store.observeRecents().first().isEmpty())
        // Available doc keeps its stable id and settings with is_in_recents = 0
        val kept = store.getById(availableDoc.id)
        assertNotNull(kept)
        assertFalse(kept!!.isInRecents)
        // Unavailable doc is permanently deleted (RF-08)
        assertNull(store.getById(unavailableDoc.id))
    }

    @Test
    fun `relinkDocument preserves id and custom speed, adjusts page, and supports conflict consolidation`() = runTest {
        val target = store.registerOrOpenDocument("content://docs/old.pdf", "old.pdf", 10, 1000L)
        store.saveCustomDocumentSpeed(target.documentUri, 160.0f)
        store.updateReadingProgress(target.id, 5, 0.5f, 1.8f, 160.0f, 2000L)
        store.setDocumentAvailability(target.documentUri, false)

        val conflicting = store.registerOrOpenDocument("content://docs/new.pdf", "new.pdf", 3, 3000L)

        // Without consolidation -> fails due to URI conflict
        val conflictAttempt = store.relinkDocument(
            targetDocumentId = target.id,
            newUri = conflicting.documentUri,
            newFileName = "new.pdf",
            newPageCount = 3,
            consolidateConflictingDocumentId = null,
            nowMillis = 4000L
        )
        assertTrue(conflictAttempt.isFailure)

        // With explicit consolidation -> removes conflicting row, updates target keeping same id & settings
        val consolidated = store.relinkDocument(
            targetDocumentId = target.id,
            newUri = conflicting.documentUri,
            newFileName = "new.pdf",
            newPageCount = 3,
            consolidateConflictingDocumentId = conflicting.id,
            nowMillis = 4000L
        ).getOrThrow()

        assertEquals(target.id, consolidated.id)
        assertEquals(conflicting.documentUri, consolidated.documentUri)
        assertEquals("new.pdf", consolidated.fileName)
        assertEquals(3, consolidated.pageCount)
        assertEquals(2, consolidated.currentPageIndex) // min(5, 3 - 1)
        assertEquals(160.0f, consolidated.customDefaultSpeedPxPerSec ?: 0f, 0.001f)
        assertEquals(1.8f, consolidated.zoomScale, 0.001f)
        assertTrue(consolidated.isAvailable)
        assertTrue(consolidated.isInRecents)
        assertNull(store.getById(conflicting.id))
    }

    @Test
    fun `replaceDatabaseSnapshot ensures datastore_migration_completed is set to 1`() = runTest {
        val doc = PdfDocumentRecord.createSanitized(
            id = "11111111-1111-4111-8111-111111111111",
            documentUri = "content://backup/doc.pdf",
            fileName = "doc.pdf",
            pageCount = 12,
            currentPageIndex = 3,
            customDefaultSpeedPxPerSec = 175.0f,
            lastAccessedAt = 9000L
        )
        val folder = FavoriteFolder(
            treeUri = "content://tree/books",
            displayName = "Books",
            pathSummary = "primary:Books",
            pinnedTimestamp = 8000L,
            cachedFileCount = 4
        )

        store.replaceDatabaseSnapshot(
            globalDefaultSpeedPxPerSec = 130.0f,
            documents = listOf(doc),
            favoriteFolders = listOf(folder),
            nowMillis = 10_000L
        )

        assertTrue(store.isDataStoreMigrationCompleted())
        assertEquals(130.0f, store.getGlobalDefaultSpeed().first(), 0.001f)
        assertEquals(listOf(doc.id), store.observeAllDocuments().first().map { it.id })
        assertEquals(listOf(folder.treeUri), store.getFavoriteFoldersSnapshot().map { it.treeUri })

        val migrationFlag = store.getAppSetting(PdfReaderDatabaseSchema.SETTING_DATASTORE_MIGRATION_COMPLETED)
        assertEquals(PdfReaderDatabaseSchema.MIGRATION_COMPLETED_VALUE, migrationFlag)
    }

    @Test
    fun `findCustomSpeedByFileName always returns null per RN-01`() = runTest {
        val doc = store.registerOrOpenDocument("content://docs/a.pdf", "shared.pdf", 10, 1000L)
        store.saveCustomDocumentSpeed(doc.documentUri, 190.0f)

        assertNull(store.findCustomSpeedByFileName("shared.pdf"))
        assertNotEquals("", doc.id)
    }
}
