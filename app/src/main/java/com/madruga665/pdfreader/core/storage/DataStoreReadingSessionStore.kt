package com.madruga665.pdfreader.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.madruga665.pdfreader.core.backup.BackupReadingSession
import com.madruga665.pdfreader.core.backup.FilenameSpeedReconciler
import com.madruga665.pdfreader.core.model.ReadingSession
import com.madruga665.pdfreader.core.model.SpeedPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pdf_reader_prefs")

class DataStoreReadingSessionStore(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore
) : ReadingSessionStore {

    companion object {
        val KEY_DEFAULT_SPEED = floatPreferencesKey("global_default_speed")
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

        private fun pageKey(uri: String) = intPreferencesKey("$PREFIX_PAGE$uri")
        private fun offsetKey(uri: String) = floatPreferencesKey("$PREFIX_OFFSET$uri")
        private fun zoomKey(uri: String) = floatPreferencesKey("$PREFIX_ZOOM$uri")
        private fun speedKey(uri: String) = floatPreferencesKey("$PREFIX_SPEED$uri")
        private fun customSpeedKey(uri: String) = floatPreferencesKey("$PREFIX_CUSTOM_SPEED$uri")
        private fun timeKey(uri: String) = longPreferencesKey("$PREFIX_TIME$uri")
        private fun fileNameKey(uri: String) = stringPreferencesKey("$PREFIX_FILENAME$uri")
    }

    override fun getSession(documentUri: String): Flow<ReadingSession?> {
        return dataStore.data.map { prefs ->
            val page = prefs[pageKey(documentUri)] ?: return@map null
            buildSessionFromPrefs(prefs, documentUri, page)
        }
    }

    override suspend fun saveSession(session: ReadingSession) {
        dataStore.edit { prefs ->
            prefs[pageKey(session.documentUri)] = session.currentPageIndex
            prefs[offsetKey(session.documentUri)] = session.normalizedScrollRatio
            prefs[zoomKey(session.documentUri)] = session.zoomScale
            prefs[speedKey(session.documentUri)] = session.preferredSpeedPxPerSec
            if (session.customDefaultSpeedPxPerSec != null) {
                prefs[customSpeedKey(session.documentUri)] = session.customDefaultSpeedPxPerSec
            }
            if (session.fileName.isNotBlank()) {
                prefs[fileNameKey(session.documentUri)] = session.fileName.trim()
            }
            prefs[timeKey(session.documentUri)] = session.lastReadTimestamp
        }
    }

    override fun getGlobalDefaultSpeed(): Flow<Float> {
        return dataStore.data.map { prefs ->
            prefs[KEY_DEFAULT_SPEED] ?: SpeedPreset.MEDIUM.speedPxPerSec
        }
    }

    override suspend fun setGlobalDefaultSpeed(speedPxPerSec: Float) {
        val clamped = speedPxPerSec.coerceIn(10.0f, 500.0f)
        dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_SPEED] = clamped
        }
    }

    override fun getCustomDocumentSpeed(documentUri: String): Flow<Float?> {
        return dataStore.data.map { prefs ->
            prefs[customSpeedKey(documentUri)]
        }
    }

    override suspend fun saveCustomDocumentSpeed(documentUri: String, speedPxPerSec: Float) {
        val clamped = speedPxPerSec.coerceIn(10.0f, 500.0f)
        dataStore.edit { prefs ->
            prefs[customSpeedKey(documentUri)] = clamped
            if (prefs[timeKey(documentUri)] == null) {
                prefs[timeKey(documentUri)] = System.currentTimeMillis()
            }
        }
    }

    override suspend fun clearCustomDocumentSpeed(documentUri: String) {
        dataStore.edit { prefs ->
            prefs.remove(customSpeedKey(documentUri))
            val targetFileName = prefs[fileNameKey(documentUri)]?.trim()
            if (!targetFileName.isNullOrEmpty()) {
                val keysToRemove = prefs.asMap().entries
                    .filter { (key, value) ->
                        key.name.startsWith(PREFIX_FILENAME) &&
                            value is String &&
                            value.trim().equals(targetFileName, ignoreCase = true)
                    }
                    .map { (key, _) ->
                        val otherUri = key.name.removePrefix(PREFIX_FILENAME)
                        customSpeedKey(otherUri)
                    }
                for (key in keysToRemove) {
                    prefs.remove(key)
                }
            }
        }
    }

    override suspend fun saveDocumentFileName(documentUri: String, fileName: String) {
        val trimmed = fileName.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { prefs ->
            prefs[fileNameKey(documentUri)] = trimmed
        }
    }

    override suspend fun getAllSessionsSnapshot(): List<ReadingSession> {
        val prefs = dataStore.data.first()
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
            val page = prefs[pageKey(uri)] ?: 0
            buildSessionFromPrefs(prefs, uri, page)
        }
    }

    override suspend fun replaceAllData(
        globalDefaultSpeedPxPerSec: Float,
        sessions: List<ReadingSession>
    ) {
        dataStore.edit { prefs ->
            prefs.clear()
            prefs[KEY_DEFAULT_SPEED] = globalDefaultSpeedPxPerSec.coerceIn(10.0f, 500.0f)
            for (session in sessions) {
                prefs[pageKey(session.documentUri)] = session.currentPageIndex.coerceAtLeast(0)
                prefs[offsetKey(session.documentUri)] = session.normalizedScrollRatio.coerceIn(0.0f, 1.0f)
                prefs[zoomKey(session.documentUri)] = session.zoomScale.coerceIn(0.5f, 4.0f)
                prefs[speedKey(session.documentUri)] = session.preferredSpeedPxPerSec.coerceIn(10.0f, 500.0f)
                if (session.customDefaultSpeedPxPerSec != null) {
                    prefs[customSpeedKey(session.documentUri)] =
                        session.customDefaultSpeedPxPerSec.coerceIn(10.0f, 500.0f)
                }
                if (session.fileName.isNotBlank()) {
                    prefs[fileNameKey(session.documentUri)] = session.fileName.trim()
                }
                prefs[timeKey(session.documentUri)] = session.lastReadTimestamp.coerceAtLeast(1L)
            }
        }
    }

    override suspend fun findCustomSpeedByFileName(fileName: String): Float? {
        val sessions = getAllSessionsSnapshot().map { s ->
            BackupReadingSession(
                documentUri = s.documentUri,
                fileName = s.fileName,
                currentPageIndex = s.currentPageIndex,
                normalizedScrollRatio = s.normalizedScrollRatio,
                zoomScale = s.zoomScale,
                preferredSpeedPxPerSec = s.preferredSpeedPxPerSec,
                customDefaultSpeedPxPerSec = s.customDefaultSpeedPxPerSec,
                lastReadTimestamp = s.lastReadTimestamp
            )
        }
        return FilenameSpeedReconciler.findCustomSpeedByFileName(fileName, sessions)
    }

    private fun buildSessionFromPrefs(
        prefs: Preferences,
        documentUri: String,
        page: Int
    ): ReadingSession? {
        val offset = prefs[offsetKey(documentUri)] ?: 0.0f
        val zoom = prefs[zoomKey(documentUri)] ?: 1.0f
        val speed = prefs[speedKey(documentUri)] ?: SpeedPreset.MEDIUM.speedPxPerSec
        val customSpeed = prefs[customSpeedKey(documentUri)]
        val time = prefs[timeKey(documentUri)] ?: System.currentTimeMillis()
        val fileName = prefs[fileNameKey(documentUri)] ?: ""

        return try {
            ReadingSession(
                documentUri = documentUri,
                currentPageIndex = page.coerceAtLeast(0),
                normalizedScrollRatio = offset.coerceIn(0.0f, 1.0f),
                zoomScale = zoom.coerceIn(0.5f, 4.0f),
                preferredSpeedPxPerSec = speed.coerceIn(10.0f, 500.0f),
                customDefaultSpeedPxPerSec = customSpeed?.coerceIn(10.0f, 500.0f),
                lastReadTimestamp = time,
                fileName = fileName
            )
        } catch (_: Exception) {
            null
        }
    }
}
