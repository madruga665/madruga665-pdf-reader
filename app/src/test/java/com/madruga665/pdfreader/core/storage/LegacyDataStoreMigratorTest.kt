package com.madruga665.pdfreader.core.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.madruga665.pdfreader.core.database.JdbcSqliteDriver
import com.madruga665.pdfreader.core.database.SqliteAppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

class LegacyDataStoreMigratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var database: SqliteAppDatabase
    private lateinit var store: SqlitePdfDocumentStore

    @Before
    fun setUp() {
        val dbFile = tempFolder.newFile("migration_test.db")
        database = SqliteAppDatabase(JdbcSqliteDriver(dbFile))
        store = SqlitePdfDocumentStore(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `migrates all 3 legacy datastores once with unique UUIDs and is idempotent on second run`() = runTest {
        val sessionsDs = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempFolder.root, "pdf_reader_prefs.preferences_pb")
        }
        val recentsDs = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempFolder.root, "recents_prefs.preferences_pb")
        }
        val foldersDs = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempFolder.root, "favorite_folders_prefs.preferences_pb")
        }

        val uriShared = "content://docs/shared.pdf"
        val uriSessionOnly = "content://docs/session_only.pdf"
        val uriRecentOnly = "content://docs/recent_only.pdf"

        sessionsDs.edit { prefs ->
            prefs[floatPreferencesKey("global_default_speed")] = 135.0f
            // Shared document (present in both sessions & recents)
            prefs[intPreferencesKey("page_$uriShared")] = 4
            prefs[floatPreferencesKey("offset_$uriShared")] = 0.45f
            prefs[floatPreferencesKey("zoom_$uriShared")] = 1.75f
            prefs[floatPreferencesKey("speed_$uriShared")] = 150.0f
            prefs[floatPreferencesKey("custom_default_speed_$uriShared")] = 165.0f
            prefs[longPreferencesKey("time_$uriShared")] = 5000L
            prefs[stringPreferencesKey("filename_$uriShared")] = "shared.pdf"
            // Session-only document (not in recents) with out-of-range values to verify clamping
            prefs[intPreferencesKey("page_$uriSessionOnly")] = 2
            prefs[floatPreferencesKey("offset_$uriSessionOnly")] = 1.8f
            prefs[floatPreferencesKey("zoom_$uriSessionOnly")] = 9.0f
            prefs[floatPreferencesKey("speed_$uriSessionOnly")] = 999.0f
            prefs[longPreferencesKey("time_$uriSessionOnly")] = 3000L
            prefs[stringPreferencesKey("filename_$uriSessionOnly")] = "session_only.pdf"
        }

        recentsDs.edit { prefs ->
            prefs[stringPreferencesKey("recent_documents_json")] = """
                [
                  {
                    "uri": "$uriShared",
                    "fileName": "shared.pdf",
                    "pageCount": 12,
                    "lastReadPageIndex": 4,
                    "progressRatio": 0.45,
                    "lastAccessedTimestamp": 5500,
                    "isAvailable": true
                  },
                  {
                    "uri": "$uriRecentOnly",
                    "fileName": "recent_only.pdf",
                    "pageCount": 8,
                    "lastReadPageIndex": 1,
                    "progressRatio": 0.125,
                    "lastAccessedTimestamp": 4000,
                    "isAvailable": true
                  }
                ]
            """.trimIndent()
        }

        foldersDs.edit { prefs ->
            prefs[stringPreferencesKey("favorite_folders_json")] = """
                [
                  {
                    "treeUri": "content://tree/books",
                    "displayName": "Books",
                    "pathSummary": "primary:Books",
                    "pinnedTimestamp": 7000,
                    "cachedFileCount": 3
                  }
                ]
            """.trimIndent()
        }

        val reader = DataStoreLegacyReader(
            sessionsDataStore = sessionsDs,
            recentsDataStore = recentsDs,
            foldersDataStore = foldersDs
        )
        val migrator = LegacyDataStoreMigrator(
            legacyReader = reader,
            documentStore = store,
            clock = { 10_000L }
        )

        val firstOutcome = migrator.migrateIfNeeded()
        assertTrue(firstOutcome is MigrationOutcome.Migrated)
        val migrated = firstOutcome as MigrationOutcome.Migrated
        assertEquals(3, migrated.documentsCount)
        assertEquals(1, migrated.foldersCount)
        assertTrue(store.isDataStoreMigrationCompleted())
        assertEquals(135.0f, store.getGlobalDefaultSpeed().first(), 0.001f)

        val allDocs = store.getAllDocumentsSnapshot()
        assertEquals(3, allDocs.size)
        // All UUIDs unique and valid
        assertEquals(3, allDocs.map { UUID.fromString(it.id) }.toSet().size)

        val shared = store.getByUri(uriShared)!!
        assertEquals(12, shared.pageCount)
        assertEquals(4, shared.currentPageIndex)
        assertEquals(1.75f, shared.zoomScale, 0.001f)
        assertEquals(165.0f, shared.customDefaultSpeedPxPerSec ?: 0f, 0.001f)
        assertTrue(shared.isInRecents)

        val sessionOnly = store.getByUri(uriSessionOnly)!!
        assertEquals(1.0f, sessionOnly.normalizedScrollRatio, 0.001f)
        assertEquals(4.0f, sessionOnly.zoomScale, 0.001f)
        assertEquals(500.0f, sessionOnly.preferredSpeedPxPerSec, 0.001f)
        assertFalse(sessionOnly.isInRecents)

        val recentOnly = store.getByUri(uriRecentOnly)!!
        assertEquals(8, recentOnly.pageCount)
        assertEquals(1, recentOnly.currentPageIndex)
        assertTrue(recentOnly.isInRecents)

        val folders = store.getFavoriteFoldersSnapshot()
        assertEquals(1, folders.size)
        assertEquals("Books", folders[0].displayName)

        // Modify SQLite after migration, then run migrator again -> must be AlreadyCompleted and NOT overwrite
        store.setGlobalDefaultSpeed(200.0f)
        store.saveCustomDocumentSpeed(uriShared, 220.0f)

        val secondOutcome = migrator.migrateIfNeeded()
        assertEquals(MigrationOutcome.AlreadyCompleted, secondOutcome)
        assertEquals(3, store.getAllDocumentsSnapshot().size)
        assertEquals(200.0f, store.getGlobalDefaultSpeed().first(), 0.001f)
        assertEquals(220.0f, store.getByUri(uriShared)?.customDefaultSpeedPxPerSec ?: 0f, 0.001f)
    }

    @Test
    fun `tolerates corrupted legacy json and still completes migration flag`() = runTest {
        val sessionsDs = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempFolder.root, "corrupt_sessions.preferences_pb")
        }
        val recentsDs = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempFolder.root, "corrupt_recents.preferences_pb")
        }
        val foldersDs = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tempFolder.root, "corrupt_folders.preferences_pb")
        }

        recentsDs.edit { prefs ->
            prefs[stringPreferencesKey("recent_documents_json")] = "{broken-json"
        }
        foldersDs.edit { prefs ->
            prefs[stringPreferencesKey("favorite_folders_json")] = "not-an-array"
        }

        val reader = DataStoreLegacyReader(sessionsDs, recentsDs, foldersDs)
        val migrator = LegacyDataStoreMigrator(reader, store)

        val outcome = migrator.migrateIfNeeded()
        assertNotNull(outcome)
        assertTrue(store.isDataStoreMigrationCompleted())
        assertTrue(store.getAllDocumentsSnapshot().isEmpty())
    }
}
