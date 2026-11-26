package com.madruga665.pdfreader.core.storage

import com.madruga665.pdfreader.core.model.ReadingSession
import kotlinx.coroutines.flow.Flow

interface ReadingSessionStore {
    fun getSession(documentUri: String): Flow<ReadingSession?>
    suspend fun saveSession(session: ReadingSession)
    fun getGlobalDefaultSpeed(): Flow<Float>
    suspend fun setGlobalDefaultSpeed(speedPxPerSec: Float)
    fun getCustomDocumentSpeed(documentUri: String): Flow<Float?>
    suspend fun saveCustomDocumentSpeed(documentUri: String, speedPxPerSec: Float)
    suspend fun clearCustomDocumentSpeed(documentUri: String)
    suspend fun saveDocumentFileName(documentUri: String, fileName: String) {}
    suspend fun getAllSessionsSnapshot(): List<ReadingSession> = emptyList()
    suspend fun replaceAllData(globalDefaultSpeedPxPerSec: Float, sessions: List<ReadingSession>) {}
    suspend fun findCustomSpeedByFileName(fileName: String): Float? = null
}
