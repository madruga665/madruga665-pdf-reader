package com.madruga665.pdfreader.core.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.FolderPdfItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException

private val Context.favoriteFoldersDataStore by preferencesDataStore(name = "favorite_folders_prefs")

class AndroidFavoriteFoldersRepository(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = context.favoriteFoldersDataStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FavoriteFoldersRepository {

    companion object {
        val KEY_FAVORITE_FOLDERS_JSON = stringPreferencesKey("favorite_folders_json")
    }

    override fun getFavoriteFolders(): Flow<List<FavoriteFolder>> {
        return dataStore.data.map { prefs ->
            val jsonString = prefs[KEY_FAVORITE_FOLDERS_JSON] ?: return@map emptyList()
            parseFoldersJson(jsonString)
        }
    }

    override suspend fun addFavoriteFolder(
        treeUri: Uri,
        displayName: String?
    ): Result<FavoriteFolder> = withContext(ioDispatcher) {
        try {
            // Take persistable permission if possible
            try {
                context.contentResolver?.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val resolvedName = displayName?.takeIf { it.isNotBlank() }
                ?: DocumentFile.fromTreeUri(context, treeUri)?.name?.takeIf { it.isNotBlank() }
                ?: treeUri.lastPathSegment?.substringAfterLast(':')?.takeIf { it.isNotBlank() }
                ?: "Folder"

            val initialFileCount = countFolderPdfs(treeUri)

            val folder = FavoriteFolder(
                treeUri = treeUri.toString(),
                displayName = resolvedName,
                pathSummary = treeUri.lastPathSegment ?: "",
                pinnedTimestamp = System.currentTimeMillis(),
                cachedFileCount = initialFileCount
            )

            dataStore.edit { prefs ->
                val currentJson = prefs[KEY_FAVORITE_FOLDERS_JSON] ?: "[]"
                val currentList = parseFoldersJson(currentJson).toMutableList()

                currentList.removeAll { it.treeUri == folder.treeUri }
                currentList.add(0, folder)

                prefs[KEY_FAVORITE_FOLDERS_JSON] = serializeFoldersJson(currentList)
            }

            Result.success(folder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFavoriteFolder(treeUri: String): Unit = withContext(ioDispatcher) {
        try {
            try {
                context.contentResolver?.releasePersistableUriPermission(
                    Uri.parse(treeUri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            dataStore.edit { prefs ->
                val currentJson = prefs[KEY_FAVORITE_FOLDERS_JSON] ?: return@edit
                val currentList = parseFoldersJson(currentJson).toMutableList()
                currentList.removeAll { it.treeUri == treeUri }
                prefs[KEY_FAVORITE_FOLDERS_JSON] = serializeFoldersJson(currentList)
            }
        } catch (_: Exception) {}
    }

    override suspend fun replaceAllFavoriteFolders(folders: List<FavoriteFolder>): Unit =
        withContext(ioDispatcher) {
            val deduplicated = folders
                .distinctBy { it.treeUri }
                .sortedByDescending { it.pinnedTimestamp }
            dataStore.edit { prefs ->
                prefs[KEY_FAVORITE_FOLDERS_JSON] = serializeFoldersJson(deduplicated)
            }
        }

    override suspend fun scanFolderPdfs(treeUri: Uri): Result<List<FolderPdfItem>> =
        withContext(ioDispatcher) {
            try {
                val docDir = DocumentFile.fromTreeUri(context, treeUri)
                if (docDir == null || !docDir.canRead()) {
                    return@withContext Result.failure(
                        FileNotFoundException("Folder is inaccessible or permission was revoked: $treeUri")
                    )
                }

                val files = docDir.listFiles()
                val pdfItems = files.filter { file ->
                    val name = file.name ?: ""
                    name.endsWith(".pdf", ignoreCase = true) || file.type == "application/pdf"
                }.map { file ->
                    FolderPdfItem(
                        uri = file.uri.toString(),
                        fileName = file.name ?: "Document.pdf",
                        fileSizeBytes = file.length().coerceAtLeast(0L),
                        lastModified = file.lastModified().coerceAtLeast(0L)
                    )
                }.sortedBy { it.fileName.lowercase() }

                // Update cached count in store asynchronously
                updateFolderFileCount(treeUri.toString(), pdfItems.size)

                Result.success(pdfItems)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun countFolderPdfs(treeUri: Uri): Int {
        return try {
            val docDir = DocumentFile.fromTreeUri(context, treeUri) ?: return 0
            if (!docDir.canRead()) return 0
            docDir.listFiles().count { file ->
                val name = file.name ?: ""
                name.endsWith(".pdf", ignoreCase = true) || file.type == "application/pdf"
            }
        } catch (_: Exception) {
            0
        }
    }

    private suspend fun updateFolderFileCount(treeUri: String, count: Int) {
        try {
            dataStore.edit { prefs ->
                val currentJson = prefs[KEY_FAVORITE_FOLDERS_JSON] ?: return@edit
                val list = parseFoldersJson(currentJson).map { folder ->
                    if (folder.treeUri == treeUri) {
                        folder.copy(cachedFileCount = count)
                    } else {
                        folder
                    }
                }
                prefs[KEY_FAVORITE_FOLDERS_JSON] = serializeFoldersJson(list)
            }
        } catch (_: Exception) {}
    }

    private fun parseFoldersJson(jsonString: String): List<FavoriteFolder> {
        val result = mutableListOf<FavoriteFolder>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val treeUri = obj.optString("treeUri")
                val displayName = obj.optString("displayName")
                val pathSummary = obj.optString("pathSummary", "")
                val pinnedTimestamp = obj.optLong("pinnedTimestamp", System.currentTimeMillis())
                val cachedFileCount = obj.optInt("cachedFileCount", 0)

                if (treeUri.isNotBlank() && displayName.isNotBlank()) {
                    result.add(
                        FavoriteFolder(
                            treeUri = treeUri,
                            displayName = displayName,
                            pathSummary = pathSummary,
                            pinnedTimestamp = pinnedTimestamp,
                            cachedFileCount = cachedFileCount.coerceAtLeast(0)
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return result
    }

    private fun serializeFoldersJson(folders: List<FavoriteFolder>): String {
        val array = JSONArray()
        for (folder in folders) {
            val obj = JSONObject()
            obj.put("treeUri", folder.treeUri)
            obj.put("displayName", folder.displayName)
            obj.put("pathSummary", folder.pathSummary)
            obj.put("pinnedTimestamp", folder.pinnedTimestamp)
            obj.put("cachedFileCount", folder.cachedFileCount)
            array.put(obj)
        }
        return array.toString()
    }
}
