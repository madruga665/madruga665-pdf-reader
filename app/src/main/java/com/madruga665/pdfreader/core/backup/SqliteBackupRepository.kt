package com.madruga665.pdfreader.core.backup

import com.madruga665.pdfreader.core.database.PdfReaderDatabaseSchema
import com.madruga665.pdfreader.core.database.SqliteAppDatabase
import com.madruga665.pdfreader.core.model.SpeedPreset
import com.madruga665.pdfreader.core.storage.PdfDocumentStore
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class SqliteBackupRepository(
    private val database: SqliteAppDatabase,
    private val documentStore: PdfDocumentStore,
    private val validator: SqliteBackupValidator,
    private val fileGateway: BackupFileGateway,
    private val stagingDirectory: File,
    private val appVersion: String = "1.0.0",
    private val clock: () -> Long = { System.currentTimeMillis() }
) : BackupRepository {

    override suspend fun exportBackup(targetUri: String): Result<BackupExportResult> {
        val now = clock()
        val stagingFile = File(stagingDirectory, "export-${UUID.randomUUID()}.db")
        return try {
            documentStore.setAppSetting(PdfReaderDatabaseSchema.SETTING_APP_VERSION, appVersion)
            documentStore.setAppSetting(PdfReaderDatabaseSchema.SETTING_LAST_BACKUP_EXPORTED_AT, now.toString())

            val payload = captureLocalPayload(now)

            database.exportToStagingFile(stagingFile).getOrThrow()
            fileGateway.writeFileToUri(targetUri, stagingFile).getOrThrow()

            Result.success(BackupExportResult(summary = payload.toSummary()))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            deleteStagingFiles(stagingFile)
        }
    }

    override suspend fun previewBackup(sourceUri: String): BackupParseResult {
        val stagingFile = File(stagingDirectory, "import-${UUID.randomUUID()}.db")
        return try {
            val copyResult = fileGateway.copyUriToTempFile(
                uriString = sourceUri,
                destinationFile = stagingFile,
                maxBytes = BackupFileGateway.MAX_BACKUP_BYTES
            )
            val copyError = copyResult.exceptionOrNull()
            if (copyError != null) {
                return BackupParseResult.Failure(
                    reason = BackupErrorReason.IO_ERROR,
                    message = copyError.localizedMessage ?: "Não foi possível ler o arquivo de backup selecionado."
                )
            }
            validator.validateAndRead(stagingFile)
        } finally {
            deleteStagingFiles(stagingFile)
        }
    }

    private fun deleteStagingFiles(stagingFile: File) {
        runCatching { stagingFile.delete() }
        runCatching { File("${stagingFile.path}-wal").delete() }
        runCatching { File("${stagingFile.path}-shm").delete() }
    }

    override suspend fun restoreBackup(
        payload: BackupPayload,
        mode: RestoreMode
    ): Result<BackupRestoreResult> = runCatching {
        val now = clock()
        val localPayload = captureLocalPayload(now)
        val merged = BackupMerger.merge(
            local = localPayload,
            incoming = payload,
            mode = mode
        )
        documentStore.replaceDatabaseSnapshot(
            globalDefaultSpeedPxPerSec = merged.globalDefaultSpeedPxPerSec,
            documents = merged.documents,
            favoriteFolders = merged.favoriteFolders,
            nowMillis = now
        )
        BackupRestoreResult(
            mode = mode,
            summary = merged.toSummary()
        )
    }

    private suspend fun captureLocalPayload(exportedAt: Long): BackupPayload {
        val globalSpeed = documentStore.getGlobalDefaultSpeed().firstOrNull()
            ?: SpeedPreset.MEDIUM.speedPxPerSec
        val documents = documentStore.getAllDocumentsSnapshot()
        val folders = documentStore.getFavoriteFoldersSnapshot()
        return BackupPayload(
            schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION,
            exportedAt = exportedAt,
            appVersion = appVersion,
            globalDefaultSpeedPxPerSec = globalSpeed,
            documents = documents,
            favoriteFolders = folders
        )
    }

    companion object {
        private val FILE_NAME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")

        fun suggestedBackupFileName(timestampMillis: Long = System.currentTimeMillis()): String {
            val formatted = Instant.ofEpochMilli(timestampMillis)
                .atZone(ZoneId.systemDefault())
                .format(FILE_NAME_FORMATTER)
            return "pdf-reader-backup-$formatted.db"
        }
    }
}
