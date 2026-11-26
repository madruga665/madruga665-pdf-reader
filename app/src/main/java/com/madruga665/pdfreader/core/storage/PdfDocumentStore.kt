package com.madruga665.pdfreader.core.storage

import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import kotlinx.coroutines.flow.Flow

interface PdfDocumentStore : ReadingSessionStore {
    fun observeRecents(limit: Int = DEFAULT_RECENTS_LIMIT): Flow<List<PdfDocumentRecord>>
    fun observeAllDocuments(): Flow<List<PdfDocumentRecord>>
    suspend fun getAllDocumentsSnapshot(): List<PdfDocumentRecord>
    suspend fun getById(id: String): PdfDocumentRecord?
    suspend fun getByUri(documentUri: String): PdfDocumentRecord?

    suspend fun registerOrOpenDocument(
        uri: String,
        fileName: String,
        pageCount: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): PdfDocumentRecord

    suspend fun updateReadingProgress(
        documentId: String,
        currentPageIndex: Int,
        normalizedScrollRatio: Float,
        zoomScale: Float,
        preferredSpeedPxPerSec: Float,
        nowMillis: Long = System.currentTimeMillis()
    )

    suspend fun setDocumentAvailability(documentUri: String, isAvailable: Boolean)
    suspend fun updateAvailabilityBatch(updates: Map<String, Boolean>)

    suspend fun relinkDocument(
        targetDocumentId: String,
        newUri: String,
        newFileName: String,
        newPageCount: Int,
        consolidateConflictingDocumentId: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Result<PdfDocumentRecord>

    suspend fun removeFromRecents(documentIdOrUri: String, deleteIfUnavailable: Boolean = true)
    suspend fun clearAllRecents()

    suspend fun isDataStoreMigrationCompleted(): Boolean
    suspend fun getAppSetting(key: String): String?
    suspend fun setAppSetting(key: String, value: String)

    suspend fun getFavoriteFoldersSnapshot(): List<FavoriteFolder>
    fun observeFavoriteFolders(): Flow<List<FavoriteFolder>>
    suspend fun upsertFavoriteFolder(folder: FavoriteFolder)
    suspend fun updateFavoriteFolderFileCount(treeUri: String, count: Int)
    suspend fun removeFavoriteFolder(treeUri: String)
    suspend fun replaceFavoriteFolders(folders: List<FavoriteFolder>)

    suspend fun importLegacyDataOnce(
        globalDefaultSpeedPxPerSec: Float,
        documents: List<PdfDocumentRecord>,
        favoriteFolders: List<FavoriteFolder>,
        migratedAtMillis: Long = System.currentTimeMillis()
    )

    suspend fun replaceDatabaseSnapshot(
        globalDefaultSpeedPxPerSec: Float,
        documents: List<PdfDocumentRecord>,
        favoriteFolders: List<FavoriteFolder>,
        nowMillis: Long = System.currentTimeMillis()
    )

    companion object {
        const val DEFAULT_RECENTS_LIMIT = 30
    }
}
