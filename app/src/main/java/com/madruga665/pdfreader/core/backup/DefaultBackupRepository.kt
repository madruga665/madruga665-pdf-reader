package com.madruga665.pdfreader.core.backup

import com.madruga665.pdfreader.core.home.FavoriteFoldersRepository
import com.madruga665.pdfreader.core.home.RecentsRepository
import com.madruga665.pdfreader.core.model.ReadingSession
import com.madruga665.pdfreader.core.model.SpeedPreset
import com.madruga665.pdfreader.core.storage.ReadingSessionStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class DefaultBackupRepository(
    private val sessionStore: ReadingSessionStore,
    private val recentsRepository: RecentsRepository,
    private val favoriteFoldersRepository: FavoriteFoldersRepository,
    private val fileGateway: BackupFileGateway,
    private val appVersion: String = "1.0.0",
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BackupRepository {

    override suspend fun exportBackup(targetUri: String): Result<BackupExportResult> =
        withContext(ioDispatcher) {
            try {
                val payload = captureLocalSnapshot()
                val encodedJson = BackupJsonCodec.encode(payload)
                fileGateway.writeText(targetUri, encodedJson).map {
                    BackupExportResult(summary = payload.toSummary())
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun previewBackup(sourceUri: String): BackupParseResult =
        withContext(ioDispatcher) {
            val readResult = fileGateway.readText(sourceUri)
            readResult.fold(
                onSuccess = { rawJson -> BackupJsonCodec.decode(rawJson) },
                onFailure = { error ->
                    BackupParseResult.Failure(
                        reason = BackupErrorReason.IO_ERROR,
                        message = error.localizedMessage ?: "Não foi possível ler o arquivo de backup."
                    )
                }
            )
        }

    override suspend fun restoreBackup(
        payload: BackupPayload,
        mode: RestoreMode
    ): Result<BackupRestoreResult> = withContext(ioDispatcher) {
        try {
            val localSnapshot = captureLocalSnapshot()
            val merged = BackupMerger.merge(
                local = localSnapshot,
                incoming = payload,
                mode = mode
            )

            val readingSessions = merged.readingSessions.map { s ->
                ReadingSession(
                    documentUri = s.documentUri,
                    currentPageIndex = s.currentPageIndex.coerceAtLeast(0),
                    normalizedScrollRatio = s.normalizedScrollRatio.coerceIn(0.0f, 1.0f),
                    zoomScale = s.zoomScale.coerceIn(0.5f, 4.0f),
                    preferredSpeedPxPerSec = s.preferredSpeedPxPerSec.coerceIn(10.0f, 500.0f),
                    customDefaultSpeedPxPerSec = s.customDefaultSpeedPxPerSec?.coerceIn(10.0f, 500.0f),
                    lastReadTimestamp = s.lastReadTimestamp.coerceAtLeast(1L),
                    fileName = s.fileName
                )
            }

            sessionStore.replaceAllData(
                globalDefaultSpeedPxPerSec = merged.globalDefaultSpeedPxPerSec,
                sessions = readingSessions
            )
            recentsRepository.replaceAllRecents(merged.recentDocuments)
            favoriteFoldersRepository.replaceAllFavoriteFolders(merged.favoriteFolders)

            Result.success(
                BackupRestoreResult(
                    mode = mode,
                    summary = merged.toSummary()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun captureLocalSnapshot(): BackupPayload {
        val globalSpeed = sessionStore.getGlobalDefaultSpeed().firstOrNull()
            ?: SpeedPreset.MEDIUM.speedPxPerSec
        val sessions = sessionStore.getAllSessionsSnapshot()
        val recents = recentsRepository.getRecentDocuments().firstOrNull().orEmpty()
        val folders = favoriteFoldersRepository.getFavoriteFolders().firstOrNull().orEmpty()

        val recentNamesByUri = recents.associate { it.uri to it.fileName }
        val backupSessions = sessions.map { session ->
            val resolvedName = session.fileName.ifBlank { recentNamesByUri[session.documentUri].orEmpty() }
            BackupReadingSession(
                documentUri = session.documentUri,
                fileName = resolvedName,
                currentPageIndex = session.currentPageIndex,
                normalizedScrollRatio = session.normalizedScrollRatio,
                zoomScale = session.zoomScale,
                preferredSpeedPxPerSec = session.preferredSpeedPxPerSec,
                customDefaultSpeedPxPerSec = session.customDefaultSpeedPxPerSec,
                lastReadTimestamp = session.lastReadTimestamp
            )
        }

        return BackupPayload(
            schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION,
            exportedAt = clock(),
            appVersion = appVersion,
            globalDefaultSpeedPxPerSec = globalSpeed,
            readingSessions = backupSessions,
            recentDocuments = recents,
            favoriteFolders = folders
        )
    }
}
