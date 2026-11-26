package com.madruga665.pdfreader.core.home

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.madruga665.pdfreader.core.model.RecentDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.recentsDataStore by preferencesDataStore(name = "recents_prefs")

class DataStoreRecentsRepository(
    private val dataStore: DataStore<Preferences>
) : RecentsRepository {

    constructor(context: Context) : this(context.recentsDataStore)

    companion object {
        val KEY_RECENTS_JSON = stringPreferencesKey("recent_documents_json")
        const val MAX_RECENTS = 30
    }

    override fun getRecentDocuments(): Flow<List<RecentDocument>> {
        return dataStore.data.map { prefs ->
            val jsonString = prefs[KEY_RECENTS_JSON] ?: return@map emptyList()
            parseRecentsJson(jsonString)
        }
    }

    override suspend fun recordDocumentAccess(document: RecentDocument) {
        dataStore.edit { prefs ->
            val currentJson = prefs[KEY_RECENTS_JSON] ?: "[]"
            val currentList = parseRecentsJson(currentJson).toMutableList()

            // Remove existing entry with same URI to deduplicate
            currentList.removeAll { it.uri == document.uri }

            // Add updated document at top
            currentList.add(0, document)

            // Enforce max cap
            val cappedList = if (currentList.size > MAX_RECENTS) {
                currentList.take(MAX_RECENTS)
            } else {
                currentList
            }

            prefs[KEY_RECENTS_JSON] = serializeRecentsJson(cappedList)
        }
    }

    override suspend fun removeRecentDocument(uri: String) {
        dataStore.edit { prefs ->
            val currentJson = prefs[KEY_RECENTS_JSON] ?: return@edit
            val currentList = parseRecentsJson(currentJson).toMutableList()
            currentList.removeAll { it.uri == uri }
            prefs[KEY_RECENTS_JSON] = serializeRecentsJson(currentList)
        }
    }

    override suspend fun clearAllRecents() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_RECENTS_JSON)
        }
    }

    override suspend fun replaceAllRecents(documents: List<RecentDocument>) {
        val capped = documents
            .distinctBy { it.uri }
            .sortedByDescending { it.lastAccessedTimestamp }
            .take(MAX_RECENTS)
        dataStore.edit { prefs ->
            prefs[KEY_RECENTS_JSON] = serializeRecentsJson(capped)
        }
    }

    private fun parseRecentsJson(jsonString: String): List<RecentDocument> {
        val result = mutableListOf<RecentDocument>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val uri = obj.optString("uri")
                val fileName = obj.optString("fileName")
                val pageCount = obj.optInt("pageCount", 1)
                val lastReadPage = obj.optInt("lastReadPageIndex", 0)
                val progressRatio = obj.optDouble("progressRatio", 0.0).toFloat()
                val timestamp = obj.optLong("lastAccessedTimestamp", System.currentTimeMillis())
                val isAvailable = obj.optBoolean("isAvailable", true)

                if (uri.isNotBlank() && fileName.isNotBlank()) {
                    result.add(
                        RecentDocument(
                            uri = uri,
                            fileName = fileName,
                            pageCount = pageCount.coerceAtLeast(1),
                            lastReadPageIndex = lastReadPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0)),
                            progressRatio = progressRatio.coerceIn(0.0f, 1.0f),
                            lastAccessedTimestamp = timestamp,
                            isAvailable = isAvailable
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return result
    }

    private fun serializeRecentsJson(list: List<RecentDocument>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("uri", item.uri)
                put("fileName", item.fileName)
                put("pageCount", item.pageCount)
                put("lastReadPageIndex", item.lastReadPageIndex)
                put("progressRatio", item.progressRatio.toDouble())
                put("lastAccessedTimestamp", item.lastAccessedTimestamp)
                put("isAvailable", item.isAvailable)
            }
            array.put(obj)
        }
        return array.toString()
    }
}
