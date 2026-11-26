package com.madruga665.pdfreader.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.madruga665.pdfreader.core.model.ReadingSession
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreReadingSessionStoreTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: DataStoreReadingSessionStore

    @Before
    fun setUp() {
        val testFile = tmpFolder.newFile("test_session.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        val mockContext = mockk<Context>(relaxed = true)
        store = DataStoreReadingSessionStore(
            context = mockContext,
            dataStore = dataStore
        )
    }

    @Test
    fun getCustomDocumentSpeed_nullByDefault() = runTest(testDispatcher) {
        val speed = store.getCustomDocumentSpeed("content://pdf/1").first()
        assertNull(speed)
    }

    @Test
    fun saveCustomDocumentSpeed_persistsAndCanBeRetrieved() = runTest(testDispatcher) {
        store.saveCustomDocumentSpeed("content://pdf/1", 160.0f)
        val speed = store.getCustomDocumentSpeed("content://pdf/1").first()
        assertEquals(160.0f, speed)
    }

    @Test
    fun clearCustomDocumentSpeed_removesCustomValue() = runTest(testDispatcher) {
        store.saveCustomDocumentSpeed("content://pdf/1", 160.0f)
        store.clearCustomDocumentSpeed("content://pdf/1")
        val speed = store.getCustomDocumentSpeed("content://pdf/1").first()
        assertNull(speed)
    }

    @Test
    fun getSession_mapsCustomDefaultSpeedIfPresent() = runTest(testDispatcher) {
        val session = ReadingSession(
            documentUri = "content://pdf/1",
            currentPageIndex = 2,
            normalizedScrollRatio = 0.5f,
            preferredSpeedPxPerSec = 110f
        )
        store.saveSession(session)
        store.saveCustomDocumentSpeed("content://pdf/1", 185.0f)

        val loaded = store.getSession("content://pdf/1").first()
        assertEquals(2, loaded?.currentPageIndex)
        assertEquals(185.0f, loaded?.customDefaultSpeedPxPerSec)
    }

    @Test
    fun saveSession_persistsFileNameAndSnapshotIncludesAllSessions() = runTest(testDispatcher) {
        val session1 = ReadingSession(
            documentUri = "content://pdf/1",
            currentPageIndex = 3,
            normalizedScrollRatio = 0.4f,
            zoomScale = 1.25f,
            preferredSpeedPxPerSec = 130f,
            customDefaultSpeedPxPerSec = 170f,
            lastReadTimestamp = 5000L,
            fileName = "algebra.pdf"
        )
        store.saveSession(session1)
        store.saveCustomDocumentSpeed("content://pdf/2", 210f)
        store.saveDocumentFileName("content://pdf/2", "history.pdf")

        val loaded = store.getSession("content://pdf/1").first()
        assertEquals("algebra.pdf", loaded?.fileName)

        val snapshot = store.getAllSessionsSnapshot()
        assertEquals(2, snapshot.size)
        val byUri = snapshot.associateBy { it.documentUri }
        assertEquals("algebra.pdf", byUri["content://pdf/1"]?.fileName)
        assertEquals(170f, byUri["content://pdf/1"]?.customDefaultSpeedPxPerSec)
        assertEquals("history.pdf", byUri["content://pdf/2"]?.fileName)
        assertEquals(210f, byUri["content://pdf/2"]?.customDefaultSpeedPxPerSec)
    }

    @Test
    fun replaceAllData_replacesGlobalSpeedAndSessionsAtomically() = runTest(testDispatcher) {
        store.setGlobalDefaultSpeed(100f)
        store.saveCustomDocumentSpeed("content://pdf/old", 140f)

        val restoredSession = ReadingSession(
            documentUri = "content://pdf/new",
            currentPageIndex = 7,
            normalizedScrollRatio = 0.8f,
            zoomScale = 1.5f,
            preferredSpeedPxPerSec = 160f,
            customDefaultSpeedPxPerSec = 195f,
            lastReadTimestamp = 9000L,
            fileName = "restored.pdf"
        )
        store.replaceAllData(
            globalDefaultSpeedPxPerSec = 135f,
            sessions = listOf(restoredSession)
        )

        assertEquals(135f, store.getGlobalDefaultSpeed().first(), 0.001f)
        assertNull(store.getCustomDocumentSpeed("content://pdf/old").first())
        val snapshot = store.getAllSessionsSnapshot()
        assertEquals(1, snapshot.size)
        assertEquals("content://pdf/new", snapshot.first().documentUri)
        assertEquals("restored.pdf", snapshot.first().fileName)
        assertEquals(195f, snapshot.first().customDefaultSpeedPxPerSec)
    }

    @Test
    fun findCustomSpeedByFileName_returnsMostRecentMatchingCustomSpeed() = runTest(testDispatcher) {
        store.saveSession(
            ReadingSession(
                documentUri = "content://pdf/older",
                currentPageIndex = 1,
                normalizedScrollRatio = 0.1f,
                preferredSpeedPxPerSec = 100f,
                customDefaultSpeedPxPerSec = 140f,
                lastReadTimestamp = 1000L,
                fileName = "Livro.pdf"
            )
        )
        store.saveSession(
            ReadingSession(
                documentUri = "content://pdf/newer",
                currentPageIndex = 2,
                normalizedScrollRatio = 0.2f,
                preferredSpeedPxPerSec = 100f,
                customDefaultSpeedPxPerSec = 220f,
                lastReadTimestamp = 2000L,
                fileName = "livro.pdf"
            )
        )

        val matched = store.findCustomSpeedByFileName("LIVRO.PDF")
        assertEquals(220f, matched)
        assertNull(store.findCustomSpeedByFileName("inexistente.pdf"))
    }

    @Test
    fun clearCustomDocumentSpeed_clearsAllUrisWithSameFileNameAndPreservesOtherPdfs() = runTest(testDispatcher) {
        store.saveSession(
            ReadingSession(
                documentUri = "content://pdf/old_restored",
                currentPageIndex = 1,
                normalizedScrollRatio = 0.1f,
                preferredSpeedPxPerSec = 100f,
                customDefaultSpeedPxPerSec = 175f,
                lastReadTimestamp = 1000L,
                fileName = "Estudo.pdf"
            )
        )
        store.saveSession(
            ReadingSession(
                documentUri = "content://pdf/new_reconciled",
                currentPageIndex = 2,
                normalizedScrollRatio = 0.2f,
                preferredSpeedPxPerSec = 100f,
                customDefaultSpeedPxPerSec = 175f,
                lastReadTimestamp = 2000L,
                fileName = "estudo.PDF"
            )
        )
        store.saveSession(
            ReadingSession(
                documentUri = "content://pdf/other",
                currentPageIndex = 3,
                normalizedScrollRatio = 0.3f,
                preferredSpeedPxPerSec = 110f,
                customDefaultSpeedPxPerSec = 240f,
                lastReadTimestamp = 3000L,
                fileName = "outro.pdf"
            )
        )

        store.clearCustomDocumentSpeed("content://pdf/new_reconciled")

        assertNull(store.getCustomDocumentSpeed("content://pdf/new_reconciled").first())
        assertNull(store.getCustomDocumentSpeed("content://pdf/old_restored").first())
        assertNull(store.findCustomSpeedByFileName("estudo.pdf"))
        assertEquals(240f, store.getCustomDocumentSpeed("content://pdf/other").first())
    }
}
