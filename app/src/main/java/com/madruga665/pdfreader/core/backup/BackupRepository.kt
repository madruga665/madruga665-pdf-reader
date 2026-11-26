package com.madruga665.pdfreader.core.backup

interface BackupRepository {
    suspend fun exportBackup(targetUri: String): Result<BackupExportResult>
    suspend fun previewBackup(sourceUri: String): BackupParseResult
    suspend fun restoreBackup(payload: BackupPayload, mode: RestoreMode): Result<BackupRestoreResult>
}
