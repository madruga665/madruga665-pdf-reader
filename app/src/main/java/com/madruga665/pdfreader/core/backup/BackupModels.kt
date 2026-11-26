package com.madruga665.pdfreader.core.backup

import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.RecentDocument
import com.madruga665.pdfreader.core.model.SpeedPreset

const val CURRENT_BACKUP_SCHEMA_VERSION = 1

data class BackupReadingSession(
    val documentUri: String,
    val fileName: String = "",
    val currentPageIndex: Int = 0,
    val normalizedScrollRatio: Float = 0.0f,
    val zoomScale: Float = 1.0f,
    val preferredSpeedPxPerSec: Float = SpeedPreset.MEDIUM.speedPxPerSec,
    val customDefaultSpeedPxPerSec: Float? = null,
    val lastReadTimestamp: Long = 1L
)

data class BackupPayload(
    val schemaVersion: Int = CURRENT_BACKUP_SCHEMA_VERSION,
    val exportedAt: Long,
    val appVersion: String,
    val globalDefaultSpeedPxPerSec: Float = SpeedPreset.MEDIUM.speedPxPerSec,
    val readingSessions: List<BackupReadingSession> = emptyList(),
    val recentDocuments: List<RecentDocument> = emptyList(),
    val favoriteFolders: List<FavoriteFolder> = emptyList()
) {
    fun toSummary(): BackupSummary = BackupSummary(
        exportedAt = exportedAt,
        appVersion = appVersion,
        globalDefaultSpeedPxPerSec = globalDefaultSpeedPxPerSec,
        sessionsCount = readingSessions.size,
        customSpeedsCount = readingSessions.count { it.customDefaultSpeedPxPerSec != null },
        recentsCount = recentDocuments.size,
        favoriteFoldersCount = favoriteFolders.size
    )
}

enum class RestoreMode {
    MERGE,
    REPLACE
}

data class BackupSummary(
    val exportedAt: Long,
    val appVersion: String,
    val globalDefaultSpeedPxPerSec: Float,
    val sessionsCount: Int,
    val customSpeedsCount: Int,
    val recentsCount: Int,
    val favoriteFoldersCount: Int
)

enum class BackupErrorReason {
    EMPTY_FILE,
    INVALID_JSON,
    UNSUPPORTED_SCHEMA_VERSION,
    INVALID_STRUCTURE,
    IO_ERROR
}

sealed interface BackupParseResult {
    data class Success(
        val payload: BackupPayload,
        val summary: BackupSummary
    ) : BackupParseResult

    data class Failure(
        val reason: BackupErrorReason,
        val message: String
    ) : BackupParseResult
}

data class BackupExportResult(
    val summary: BackupSummary
)

data class BackupRestoreResult(
    val mode: RestoreMode,
    val summary: BackupSummary
)
