package com.madruga665.pdfreader.core.home

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.madruga665.pdfreader.core.model.RecentDocument
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreRecentsRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: DataStoreRecentsRepository

    @Before
    fun setUp() {
        val testDataStoreFile = tmpFolder.newFile("test_recents.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testDataStoreFile }
        )
        val mockContext = mockk<Context>(relaxed = true)
        repository = DataStoreRecentsRepository(dataStore)
    }

    @Test
    fun recordDocumentAccess_appendsAndEnforcesCap30() = runTest {
        // Record 35 documents
        for (i in 1..35) {
            val doc = RecentDocument(
                uri = "content://pdf/$i",
                fileName = "doc_$i.pdf",
                pageCount = 10,
                lastReadPageIndex = 1,
                progressRatio = 0.1f,
                lastAccessedTimestamp = i.toLong() * 1000L
            )
            repository.recordDocumentAccess(doc)
        }

        val recents = repository.getRecentDocuments().first()
        assertEquals(30, recents.size)
        // Most recent should be doc_35
        assertEquals("content://pdf/35", recents.first().uri)
    }

    @Test
    fun recordDocumentAccess_deduplicatesAndMovesToTop() = runTest {
        val doc1 = RecentDocument(
            uri = "content://pdf/1",
            fileName = "book.pdf",
            pageCount = 20,
            lastReadPageIndex = 2,
            progressRatio = 0.1f,
            lastAccessedTimestamp = 1000L
        )
        val doc2 = RecentDocument(
            uri = "content://pdf/2",
            fileName = "notes.pdf",
            pageCount = 5,
            lastReadPageIndex = 1,
            progressRatio = 0.2f,
            lastAccessedTimestamp = 2000L
        )
        repository.recordDocumentAccess(doc1)
        repository.recordDocumentAccess(doc2)

        // Re-access doc1 with updated page progress
        val doc1Updated = doc1.copy(
            lastReadPageIndex = 15,
            progressRatio = 0.75f,
            lastAccessedTimestamp = 3000L
        )
        repository.recordDocumentAccess(doc1Updated)

        val recents = repository.getRecentDocuments().first()
        assertEquals(2, recents.size)
        assertEquals("content://pdf/1", recents.first().uri)
        assertEquals(15, recents.first().lastReadPageIndex)
        assertEquals(0.75f, recents.first().progressRatio, 0.001f)
    }

    @Test
    fun removeRecentDocument_removesSpecifiedItem() = runTest {
        val doc = RecentDocument(
            uri = "content://pdf/delete_me",
            fileName = "delete_me.pdf",
            pageCount = 10,
            lastReadPageIndex = 0,
            progressRatio = 0.0f
        )
        repository.recordDocumentAccess(doc)
        assertEquals(1, repository.getRecentDocuments().first().size)

        repository.removeRecentDocument("content://pdf/delete_me")
        assertTrue(repository.getRecentDocuments().first().isEmpty())
    }

    @Test
    fun clearAllRecents_clearsList() = runTest {
        val doc = RecentDocument(
            uri = "content://pdf/1",
            fileName = "test.pdf",
            pageCount = 5,
            lastReadPageIndex = 0,
            progressRatio = 0.0f
        )
        repository.recordDocumentAccess(doc)
        repository.clearAllRecents()
        assertTrue(repository.getRecentDocuments().first().isEmpty())
    }

    @Test
    fun getRecentDocuments_handlesCorruptedJson_returnsEmptyList() = runTest {
        dataStore.edit { prefs ->
            prefs[DataStoreRecentsRepository.KEY_RECENTS_JSON] = "{ corrupted_invalid_json ["
        }
        val recents = repository.getRecentDocuments().first()
        assertTrue(recents.isEmpty())
    }

    @Test
    fun recordDocumentAccess_preservesAvailabilityFlag() = runTest {
        val doc = RecentDocument(
            uri = "content://pdf/missing",
            fileName = "missing.pdf",
            pageCount = 10,
            lastReadPageIndex = 2,
            progressRatio = 0.2f,
            isAvailable = false
        )
        repository.recordDocumentAccess(doc)
        val recents = repository.getRecentDocuments().first()
        assertEquals(1, recents.size)
        assertEquals(false, recents.first().isAvailable)
    }

    @Test
    fun replaceAllRecents_replacesExistingAndEnforcesMaxCap30() = runTest {
        repository.recordDocumentAccess(
            RecentDocument(
                uri = "content://pdf/old",
                fileName = "old.pdf",
                pageCount = 5,
                lastReadPageIndex = 0,
                progressRatio = 0.0f,
                lastAccessedTimestamp = 500L
            )
        )

        val incoming = (1..35).map { idx ->
            RecentDocument(
                uri = "content://pdf/new_$idx",
                fileName = "new_$idx.pdf",
                pageCount = 10,
                lastReadPageIndex = 1,
                progressRatio = 0.1f,
                lastAccessedTimestamp = idx.toLong() * 1000L
            )
        }
        repository.replaceAllRecents(incoming)

        val recents = repository.getRecentDocuments().first()
        assertEquals(30, recents.size)
        assertTrue(recents.none { it.uri == "content://pdf/old" })
    }
}
