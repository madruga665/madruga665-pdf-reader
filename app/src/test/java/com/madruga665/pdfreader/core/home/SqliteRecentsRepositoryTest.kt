package com.madruga665.pdfreader.core.home

import com.madruga665.pdfreader.core.database.JdbcSqliteDriver
import com.madruga665.pdfreader.core.database.SqliteAppDatabase
import com.madruga665.pdfreader.core.model.RecentDocument
import com.madruga665.pdfreader.core.storage.SqlitePdfDocumentStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SqliteRecentsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var database: SqliteAppDatabase
    private lateinit var store: SqlitePdfDocumentStore
    private lateinit var repository: SqliteRecentsRepository

    @Before
    fun setUp() {
        val dbFile = tempFolder.newFile("recents_test.db")
        database = SqliteAppDatabase(JdbcSqliteDriver(dbFile))
        store = SqlitePdfDocumentStore(database)
        repository = SqliteRecentsRepository(store)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `recordDocumentAccess persists document with stable UUID and orders by recency`() = runTest {
        repository.recordDocumentAccess(
            RecentDocument(
                uri = "content://pdf/1",
                fileName = "one.pdf",
                pageCount = 10,
                lastReadPageIndex = 2,
                progressRatio = 0.2f,
                lastAccessedTimestamp = 1000L
            )
        )
        repository.recordDocumentAccess(
            RecentDocument(
                uri = "content://pdf/2",
                fileName = "two.pdf",
                pageCount = 8,
                lastReadPageIndex = 1,
                progressRatio = 0.12f,
                lastAccessedTimestamp = 2000L
            )
        )

        val recents = repository.getRecentDocuments().first()
        assertEquals(2, recents.size)
        assertEquals("content://pdf/2", recents[0].uri)
        assertTrue(recents[0].id.isNotBlank())
        assertEquals("content://pdf/1", recents[1].uri)
        assertTrue(recents[1].id.isNotBlank())
    }

    @Test
    fun `updateAvailability marks unavailable without losing custom speed and remove deletes unavailable`() = runTest {
        val created = store.registerOrOpenDocument("content://pdf/missing", "missing.pdf", 5, 1000L)
        store.saveCustomDocumentSpeed(created.documentUri, 160.0f)

        repository.updateAvailability("content://pdf/missing", false)

        val recents = repository.getRecentDocuments().first()
        assertEquals(1, recents.size)
        assertFalse(recents[0].isAvailable)
        assertEquals(created.id, recents[0].id)
        assertEquals(160.0f, store.getById(created.id)?.customDefaultSpeedPxPerSec ?: 0f, 0.001f)

        repository.removeRecentDocument("content://pdf/missing")
        assertTrue(repository.getRecentDocuments().first().isEmpty())
        assertNull(store.getById(created.id))
    }

    @Test
    fun `removeRecentDocument keeps available document record for future reopen`() = runTest {
        val created = store.registerOrOpenDocument("content://pdf/keep", "keep.pdf", 5, 1000L)
        store.saveCustomDocumentSpeed(created.documentUri, 145.0f)

        repository.removeRecentDocument("content://pdf/keep")

        assertTrue(repository.getRecentDocuments().first().isEmpty())
        val stillInDb = store.getById(created.id)
        assertNotNull(stillInDb)
        assertEquals(145.0f, stillInDb!!.customDefaultSpeedPxPerSec ?: 0f, 0.001f)
    }

    @Test
    fun `enforces max 30 recents cap in observable list`() = runTest {
        for (i in 1..35) {
            repository.recordDocumentAccess(
                RecentDocument(
                    uri = "content://pdf/$i",
                    fileName = "doc_$i.pdf",
                    pageCount = 5,
                    lastReadPageIndex = 0,
                    progressRatio = 0f,
                    lastAccessedTimestamp = i * 1000L
                )
            )
        }

        val recents = repository.getRecentDocuments().first()
        assertEquals(SqliteRecentsRepository.MAX_RECENTS, recents.size)
        assertEquals("content://pdf/35", recents.first().uri)
    }
}
