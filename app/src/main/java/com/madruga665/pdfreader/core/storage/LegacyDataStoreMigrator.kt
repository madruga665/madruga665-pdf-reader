package com.madruga665.pdfreader.core.storage

import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import java.util.UUID

sealed interface MigrationOutcome {
    data object AlreadyCompleted : MigrationOutcome
    data class Migrated(
        val documentsCount: Int,
        val foldersCount: Int
    ) : MigrationOutcome
}

class LegacyDataStoreMigrator(
    private val legacyReader: LegacyDataStoreReader,
    private val documentStore: PdfDocumentStore,
    private val uuidGenerator: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun migrateIfNeeded(): MigrationOutcome {
        if (documentStore.isDataStoreMigrationCompleted()) {
            return MigrationOutcome.AlreadyCompleted
        }

        val snapshot = legacyReader.readSnapshot()
        val now = PdfDocumentRecord.sanitizeTimestamp(clock())
        val documents = assembleDocuments(snapshot, now)
        val folders = snapshot.favoriteFolders
            .distinctBy { it.treeUri }
            .sortedByDescending { it.pinnedTimestamp }

        documentStore.importLegacyDataOnce(
            globalDefaultSpeedPxPerSec = snapshot.globalDefaultSpeedPxPerSec,
            documents = documents,
            favoriteFolders = folders,
            migratedAtMillis = now
        )

        return MigrationOutcome.Migrated(
            documentsCount = documents.size,
            foldersCount = folders.size
        )
    }

    private fun assembleDocuments(
        snapshot: LegacyMigrationSnapshot,
        now: Long
    ): List<PdfDocumentRecord> {
        val sessionsByUri = snapshot.sessions.associateBy { it.documentUri }
        val recentsByUri = snapshot.recents.associateBy { it.uri }
        val allUris = linkedSetOf<String>().apply {
            addAll(sessionsByUri.keys)
            addAll(recentsByUri.keys)
        }

        return allUris.mapNotNull { uri ->
            if (uri.isBlank()) return@mapNotNull null
            val session = sessionsByUri[uri]
            val recent = recentsByUri[uri]

            val pageCount = PdfDocumentRecord.sanitizePageCount(
                maxOf(
                    recent?.pageCount ?: 1,
                    (session?.currentPageIndex ?: 0) + 1
                )
            )
            val rawPageIndex = session?.currentPageIndex ?: recent?.lastReadPageIndex ?: 0
            val rawRatio = session?.normalizedScrollRatio ?: recent?.progressRatio ?: 0.0f
            val fileName = recent?.fileName?.takeIf { it.isNotBlank() }
                ?: session?.fileName?.takeIf { it.isNotBlank() }
                ?: PdfDocumentRecord.DEFAULT_FILE_NAME
            val lastAccessed = maxOf(
                session?.lastReadTimestamp ?: 0L,
                recent?.lastAccessedTimestamp ?: 0L
            ).let { if (it > 0L) it else now }

            PdfDocumentRecord.createSanitized(
                id = uuidGenerator(),
                documentUri = uri,
                fileName = fileName,
                pageCount = pageCount,
                currentPageIndex = rawPageIndex,
                normalizedScrollRatio = rawRatio,
                zoomScale = session?.zoomScale ?: PdfDocumentRecord.DEFAULT_ZOOM_SCALE,
                preferredSpeedPxPerSec = session?.preferredSpeedPxPerSec ?: snapshot.globalDefaultSpeedPxPerSec,
                customDefaultSpeedPxPerSec = session?.customDefaultSpeedPxPerSec,
                lastAccessedAt = lastAccessed,
                isInRecents = recent != null,
                isAvailable = recent?.isAvailable ?: true,
                createdAt = lastAccessed,
                updatedAt = now
            )
        }
    }
}
