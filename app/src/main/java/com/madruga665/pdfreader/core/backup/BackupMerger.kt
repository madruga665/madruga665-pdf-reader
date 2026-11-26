package com.madruga665.pdfreader.core.backup

import com.madruga665.pdfreader.core.home.DataStoreRecentsRepository
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.RecentDocument
import com.madruga665.pdfreader.core.model.SpeedPreset

object BackupMerger {

    fun merge(
        local: BackupPayload,
        incoming: BackupPayload,
        mode: RestoreMode
    ): BackupPayload {
        if (mode == RestoreMode.REPLACE) {
            return incoming.copy(
                globalDefaultSpeedPxPerSec = incoming.globalDefaultSpeedPxPerSec.coerceIn(10.0f, 500.0f),
                recentDocuments = incoming.recentDocuments
                    .distinctBy { it.uri }
                    .sortedByDescending { it.lastAccessedTimestamp }
                    .take(DataStoreRecentsRepository.MAX_RECENTS),
                favoriteFolders = incoming.favoriteFolders
                    .distinctBy { it.treeUri }
                    .sortedByDescending { it.pinnedTimestamp }
            )
        }

        val defaultSpeed = SpeedPreset.MEDIUM.speedPxPerSec
        val resolvedGlobalSpeed = if (
            local.globalDefaultSpeedPxPerSec == defaultSpeed &&
            incoming.globalDefaultSpeedPxPerSec != defaultSpeed
        ) {
            incoming.globalDefaultSpeedPxPerSec
        } else {
            local.globalDefaultSpeedPxPerSec
        }.coerceIn(10.0f, 500.0f)

        val mergedSessions = mergeSessions(local.readingSessions, incoming.readingSessions)
        val mergedRecents = mergeRecents(local.recentDocuments, incoming.recentDocuments)
        val mergedFolders = mergeFolders(local.favoriteFolders, incoming.favoriteFolders)

        return BackupPayload(
            schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION,
            exportedAt = maxOf(local.exportedAt, incoming.exportedAt),
            appVersion = local.appVersion.ifBlank { incoming.appVersion },
            globalDefaultSpeedPxPerSec = resolvedGlobalSpeed,
            readingSessions = mergedSessions,
            recentDocuments = mergedRecents,
            favoriteFolders = mergedFolders
        )
    }

    private fun mergeSessions(
        local: List<BackupReadingSession>,
        incoming: List<BackupReadingSession>
    ): List<BackupReadingSession> {
        val result = linkedMapOf<String, BackupReadingSession>()
        local.forEach { result[it.documentUri] = it }

        incoming.forEach { inc ->
            val existing = result[inc.documentUri]
            if (existing == null) {
                result[inc.documentUri] = inc
            } else {
                val winner = if (inc.lastReadTimestamp > existing.lastReadTimestamp) inc else existing
                val loser = if (winner === inc) existing else inc
                result[inc.documentUri] = winner.copy(
                    customDefaultSpeedPxPerSec = winner.customDefaultSpeedPxPerSec ?: loser.customDefaultSpeedPxPerSec,
                    fileName = winner.fileName.ifBlank { loser.fileName }
                )
            }
        }
        return result.values.toList()
    }

    private fun mergeRecents(
        local: List<RecentDocument>,
        incoming: List<RecentDocument>
    ): List<RecentDocument> {
        val result = linkedMapOf<String, RecentDocument>()
        local.forEach { result[it.uri] = it }

        incoming.forEach { inc ->
            val existing = result[inc.uri]
            if (existing == null || inc.lastAccessedTimestamp > existing.lastAccessedTimestamp) {
                result[inc.uri] = inc
            }
        }
        return result.values
            .sortedByDescending { it.lastAccessedTimestamp }
            .take(DataStoreRecentsRepository.MAX_RECENTS)
    }

    private fun mergeFolders(
        local: List<FavoriteFolder>,
        incoming: List<FavoriteFolder>
    ): List<FavoriteFolder> {
        val result = linkedMapOf<String, FavoriteFolder>()
        local.forEach { result[it.treeUri] = it }

        incoming.forEach { inc ->
            val existing = result[inc.treeUri]
            if (existing == null || inc.pinnedTimestamp > existing.pinnedTimestamp) {
                result[inc.treeUri] = inc
            }
        }
        return result.values.sortedByDescending { it.pinnedTimestamp }
    }
}
