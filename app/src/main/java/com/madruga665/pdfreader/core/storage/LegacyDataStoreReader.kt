package com.madruga665.pdfreader.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import com.madruga665.pdfreader.core.model.ReadingSession
import com.madruga665.pdfreader.core.model.RecentDocument
import com.madruga665.pdfreader.core.model.SpeedPreset
import kotlinx.coroutines.flow.first
import org.json.JSONArray

private val Context.legacySessionsDataStore by preferencesDataStore(name = "pdf_reader_prefs")
private val Context.legacyRecentsDataStore by preferencesDataStore(name = "recents_prefs")
private val Context.legacyFoldersDataStore by preferencesDataStore(name = "favorite_folders_prefs")

data class LegacyMigrationSnapshot(
    val hasAnyLegacyData: Boolean,
    val globalDefaultSpeedPxPerSec: Float,
    val sessions: List<ReadingSession>,
    val recents: List<RecentDocument>,
    val favoriteFolders: List<FavoriteFolder>
)

interface LegacyDataStoreReader {
    suspend fun readSnapshot(): LegacyMigrationSnapshot
}

class DataStoreLegacyReader(
    private val sessionsDataStore: DataStore<Preferences>,
    private val recentsDataStore: DataStore<Preferences>,
    private val foldersDataStore: DataStore<Preferences>
) : LegacyDataStoreReader {

    constructor(context: Context) : this(
        sessionsDataStore = context.applicationContext.legacySessionsDataStore,
        recentsDataStore = context.applicationContext.legacyRecentsDataStore,
        foldersDataStore = context.applicationContext.legacyFoldersDataStore
    )

    override suspend fun readSnapshot(): LegacyMigrationSnapshot {
        val sessionsPrefs = runCatching { sessionsDataStore.data.first() }.getOrNull()
        val recentsPrefs = runCatching { recentsDataStore.data.first() }.getOrNull()
        val foldersPrefs = runCatching { foldersDataStore.data.first() }.getOrNull()

        val hasGlobalSpeed = sessionsPrefs?.get(KEY_DEFAULT_SPEED) != null
        val globalSpeed = PdfDocumentRecord.sanitizeSpeed(
            sessionsPrefs?.get(KEY_DEFAULT_SPEED) ?: SpeedPreset.MEDIUM.speedPxPerSec
        )

        val sessions = if (sessionsPrefs != null) parseSessions(sessionsPrefs) else emptyList()
        val recents = recentsPrefs?.get(KEY_RECENTS_JSON)?.let(::parseRecents) ?: emptyList()
        val folders = foldersPrefs?.get(KEY_FAVORITE_FOLDERS_JSON)?.let(::parseFolders) ?: emptyList()

        val hasAny = hasGlobalSpeed || sessions.isNotEmpty() || recents.isNotEmpty() || folders.isNotEmpty()
        return LegacyMigrationSnapshot(
            hasAnyLegacyData = hasAny,
            globalDefaultSpeedPxPerSec = globalSpeed,
            sessions = sessions,
            recents = recents,
            favoriteFolders = folders
        )
    }

    private fun parseSessions(prefs: Preferences): List<ReadingSession> {
        val uris = linkedSetOf<String>()
        for (key in prefs.asMap().keys) {
            val name = key.name
            if (name == KEY_DEFAULT_SPEED.name) continue
            val prefix = ALL_PREFIXES.firstOrNull { name.startsWith(it) } ?: continue
            val uri = name.removePrefix(prefix)
            if (uri.isNotBlank()) {
                uris.add(uri)
            }
        }

        return uris.mapNotNull { uri ->
            val page = (prefs[intPreferencesKey("$PREFIX_PAGE$uri")] ?: 0).coerceAtLeast(0)
            val offset = PdfDocumentRecord.sanitizeScrollRatio(
                prefs[floatPreferencesKey("$PREFIX_OFFSET$uri")] ?: 0.0f
            )
            val zoom = PdfDocumentRecord.sanitizeZoom(
                prefs[floatPreferencesKey("$PREFIX_ZOOM$uri")] ?: PdfDocumentRecord.DEFAULT_ZOOM_SCALE
            )
            val speed = PdfDocumentRecord.sanitizeSpeed(
                prefs[floatPreferencesKey("$PREFIX_SPEED$uri")] ?: SpeedPreset.MEDIUM.speedPxPerSec
            )
            val customSpeed = PdfDocumentRecord.sanitizeOptionalSpeed(
                prefs[floatPreferencesKey("$PREFIX_CUSTOM_SPEED$uri")]
            )
            val time = PdfDocumentRecord.sanitizeTimestamp(
                prefs[longPreferencesKey("$PREFIX_TIME$uri")] ?: System.currentTimeMillis()
            )
            val fileName = prefs[stringPreferencesKey("$PREFIX_FILENAME$uri")] ?: ""

            runCatching {
                ReadingSession(
                    documentUri = uri,
                    currentPageIndex = page,
                    normalizedScrollRatio = offset,
                    zoomScale = zoom,
                    preferredSpeedPxPerSec = speed,
                    customDefaultSpeedPxPerSec = customSpeed,
                    lastReadTimestamp = time,
                    fileName = fileName
                )
            }.getOrNull()
        }
    }

    private fun parseRecents(raw: String): List<RecentDocument> {
        val result = mutableListOf<RecentDocument>()
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val uri = obj.optString("uri").trim()
                val fileName = PdfDocumentRecord.sanitizeFileName(obj.optString("fileName"))
                if (uri.isBlank()) continue

                val pageCount = PdfDocumentRecord.sanitizePageCount(obj.optInt("pageCount", 1))
                val lastReadPage = PdfDocumentRecord.sanitizePageIndex(
                    obj.optInt("lastReadPageIndex", 0),
                    pageCount
                )
                val progressRatio = PdfDocumentRecord.sanitizeScrollRatio(
                    obj.optDouble("progressRatio", 0.0).toFloat()
                )
                val timestamp = PdfDocumentRecord.sanitizeTimestamp(
                    obj.optLong("lastAccessedTimestamp", System.currentTimeMillis())
                )
                val isAvailable = obj.optBoolean("isAvailable", true)

                result.add(
                    RecentDocument(
                        uri = uri,
                        fileName = fileName,
                        pageCount = pageCount,
                        lastReadPageIndex = lastReadPage,
                        progressRatio = progressRatio,
                        lastAccessedTimestamp = timestamp,
                        isAvailable = isAvailable
                    )
                )
            }
        }
        return result
    }

    private fun parseFolders(raw: String): List<FavoriteFolder> {
        val result = mutableListOf<FavoriteFolder>()
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val treeUri = obj.optString("treeUri").trim()
                val displayName = obj.optString("displayName").trim()
                if (treeUri.isBlank() || displayName.isBlank()) continue

                val pathSummary = obj.optString("pathSummary", "")
                val pinnedTimestamp = PdfDocumentRecord.sanitizeTimestamp(
                    obj.optLong("pinnedTimestamp", System.currentTimeMillis())
                )
                val cachedFileCount = obj.optInt("cachedFileCount", 0).coerceAtLeast(0)

                result.add(
                    FavoriteFolder(
                        treeUri = treeUri,
                        displayName = displayName,
                        pathSummary = pathSummary,
                        pinnedTimestamp = pinnedTimestamp,
                        cachedFileCount = cachedFileCount
                    )
                )
            }
        }
        return result
    }

    companion object {
        private val KEY_DEFAULT_SPEED = floatPreferencesKey("global_default_speed")
        private val KEY_RECENTS_JSON = stringPreferencesKey("recent_documents_json")
        private val KEY_FAVORITE_FOLDERS_JSON = stringPreferencesKey("favorite_folders_json")

        private const val PREFIX_PAGE = "page_"
        private const val PREFIX_OFFSET = "offset_"
        private const val PREFIX_ZOOM = "zoom_"
        private const val PREFIX_SPEED = "speed_"
        private const val PREFIX_CUSTOM_SPEED = "custom_default_speed_"
        private const val PREFIX_TIME = "time_"
        private const val PREFIX_FILENAME = "filename_"

        private val ALL_PREFIXES = listOf(
            PREFIX_CUSTOM_SPEED,
            PREFIX_FILENAME,
            PREFIX_PAGE,
            PREFIX_OFFSET,
            PREFIX_ZOOM,
            PREFIX_SPEED,
            PREFIX_TIME
        )
    }
}
