package com.madruga665.pdfreader.core.backup

import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import com.madruga665.pdfreader.core.model.SpeedPreset

const val CURRENT_BACKUP_SCHEMA_VERSION = 1

data class BackupPayload(
    val schemaVersion: Int = CURRENT_BACKUP_SCHEMA_VERSION,
    val exportedAt: Long,
    val appVersion: String,
    val globalDefaultSpeedPxPerSec: Float = SpeedPreset.MEDIUM.speedPxPerSec,
    val documents: List<PdfDocumentRecord> = emptyList(),
    val favoriteFolders: List<FavoriteFolder> = emptyList()
) {
    fun toSummary(): BackupSummary = BackupSummary(
        exportedAt = exportedAt,
        appVersion = appVersion,
        globalDefaultSpeedPxPerSec = globalDefaultSpeedPxPerSec,
        sessionsCount = documents.size,
        customSpeedsCount = documents.count { it.customDefaultSpeedPxPerSec != null },
        recentsCount = documents.count { it.isInRecents },
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
    INVALID_SQLITE_FILE,
    CORRUPTED_DATABASE,
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
