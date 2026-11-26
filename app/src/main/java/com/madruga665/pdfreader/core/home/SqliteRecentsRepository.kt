package com.madruga665.pdfreader.core.home

import com.madruga665.pdfreader.core.model.RecentDocument
import com.madruga665.pdfreader.core.storage.PdfDocumentStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SqliteRecentsRepository(
    private val documentStore: PdfDocumentStore
) : RecentsRepository {

    companion object {
        const val MAX_RECENTS = 30
    }

    override fun getRecentDocuments(): Flow<List<RecentDocument>> =
        documentStore.observeRecents(limit = MAX_RECENTS).map { records ->
            records.map { it.toRecentDocument() }
        }

    override suspend fun recordDocumentAccess(document: RecentDocument) {
        val registered = documentStore.registerOrOpenDocument(
            uri = document.uri,
            fileName = document.fileName,
            pageCount = document.pageCount,
            nowMillis = document.lastAccessedTimestamp
        )
        documentStore.updateReadingProgress(
            documentId = registered.id,
            currentPageIndex = document.lastReadPageIndex,
            normalizedScrollRatio = document.progressRatio,
            zoomScale = registered.zoomScale,
            preferredSpeedPxPerSec = registered.preferredSpeedPxPerSec,
            nowMillis = document.lastAccessedTimestamp
        )
        if (!document.isAvailable) {
            documentStore.setDocumentAvailability(document.uri, false)
        }
    }

    override suspend fun removeRecentDocument(uri: String) {
        documentStore.removeFromRecents(uri, deleteIfUnavailable = true)
    }

    override suspend fun clearAllRecents() {
        documentStore.clearAllRecents()
    }

    override suspend fun updateAvailability(uri: String, isAvailable: Boolean) {
        documentStore.setDocumentAvailability(uri, isAvailable)
    }

    override suspend fun updateAvailabilityBatch(updates: Map<String, Boolean>) {
        documentStore.updateAvailabilityBatch(updates)
    }

    override suspend fun getById(id: String): RecentDocument? =
        documentStore.getById(id)?.toRecentDocument()

    override suspend fun getByUri(uri: String): RecentDocument? =
        documentStore.getByUri(uri)?.toRecentDocument()
}
