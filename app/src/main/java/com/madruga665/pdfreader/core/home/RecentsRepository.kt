package com.madruga665.pdfreader.core.home

import com.madruga665.pdfreader.core.model.RecentDocument
import kotlinx.coroutines.flow.Flow

interface RecentsRepository {
    fun getRecentDocuments(): Flow<List<RecentDocument>>
    suspend fun recordDocumentAccess(document: RecentDocument)
    suspend fun removeRecentDocument(uri: String)
    suspend fun clearAllRecents()
    suspend fun updateAvailability(uri: String, isAvailable: Boolean) {}
    suspend fun updateAvailabilityBatch(updates: Map<String, Boolean>) {}
    suspend fun getById(id: String): RecentDocument? = null
    suspend fun getByUri(uri: String): RecentDocument? = null
}
