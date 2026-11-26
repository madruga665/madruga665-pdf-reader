package com.madruga665.pdfreader.core.backup

import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import com.madruga665.pdfreader.core.model.SpeedPreset

object BackupMerger {

    fun merge(
        local: BackupPayload,
        incoming: BackupPayload,
        mode: RestoreMode
    ): BackupPayload {
        if (mode == RestoreMode.REPLACE) {
            val deduplicatedDocs = deduplicateDocuments(incoming.documents)
            return incoming.copy(
                schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION,
                globalDefaultSpeedPxPerSec = PdfDocumentRecord.sanitizeSpeed(incoming.globalDefaultSpeedPxPerSec),
                documents = deduplicatedDocs,
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
        }.let(PdfDocumentRecord::sanitizeSpeed)

        val mergedDocuments = mergeDocuments(local.documents, incoming.documents)
        val mergedFolders = mergeFolders(local.favoriteFolders, incoming.favoriteFolders)

        return BackupPayload(
            schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION,
            exportedAt = maxOf(local.exportedAt, incoming.exportedAt),
            appVersion = local.appVersion.ifBlank { incoming.appVersion },
            globalDefaultSpeedPxPerSec = resolvedGlobalSpeed,
            documents = mergedDocuments,
            favoriteFolders = mergedFolders
        )
    }

    private fun mergeDocuments(
        local: List<PdfDocumentRecord>,
        incoming: List<PdfDocumentRecord>
    ): List<PdfDocumentRecord> {
        val merged = mutableListOf<PdfDocumentRecord>()
        merged.addAll(deduplicateDocuments(local))

        for (inc in deduplicateDocuments(incoming)) {
            val matchIndex = merged.indexOfFirst { existing ->
                existing.id == inc.id || existing.documentUri == inc.documentUri
            }
            if (matchIndex == -1) {
                merged.add(inc)
            } else {
                val existing = merged[matchIndex]
                val winner = if (inc.lastAccessedAt > existing.lastAccessedAt) inc else existing
                val loser = if (winner === inc) existing else inc
                merged[matchIndex] = winner.copy(
                    id = existing.id, // Preserve stable local identity on collision
                    customDefaultSpeedPxPerSec = winner.customDefaultSpeedPxPerSec ?: loser.customDefaultSpeedPxPerSec,
                    fileName = winner.fileName.ifBlank { loser.fileName },
                    isInRecents = existing.isInRecents || inc.isInRecents
                )
            }
        }

        return merged.sortedByDescending { it.lastAccessedAt }
    }

    private fun deduplicateDocuments(documents: List<PdfDocumentRecord>): List<PdfDocumentRecord> {
        val seenIds = mutableSetOf<String>()
        val seenUris = mutableSetOf<String>()
        val result = mutableListOf<PdfDocumentRecord>()
        for (doc in documents.sortedByDescending { it.lastAccessedAt }) {
            if (doc.id in seenIds || doc.documentUri in seenUris) continue
            seenIds.add(doc.id)
            seenUris.add(doc.documentUri)
            result.add(doc)
        }
        return result
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
