package com.madruga665.pdfreader.core.backup

import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupMergerTest {

    @Test
    fun replaceMode_returnsIncomingDataOnly() {
        val local = BackupPayload(
            exportedAt = 1000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 110f,
            documents = listOf(
                PdfDocumentRecord.createSanitized(
                    id = "local-id",
                    documentUri = "content://pdf/local",
                    fileName = "local.pdf",
                    pageCount = 10,
                    currentPageIndex = 1,
                    normalizedScrollRatio = 0.1f,
                    lastAccessedAt = 5000L
                )
            ),
            favoriteFolders = listOf(
                FavoriteFolder("content://tree/local", "Local", "", 5000L, 1)
            )
        )
        val incoming = BackupPayload(
            exportedAt = 2000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 145f,
            documents = listOf(
                PdfDocumentRecord.createSanitized(
                    id = "backup-id",
                    documentUri = "content://pdf/backup",
                    fileName = "backup.pdf",
                    pageCount = 20,
                    currentPageIndex = 4,
                    normalizedScrollRatio = 0.2f,
                    customDefaultSpeedPxPerSec = 180f,
                    lastAccessedAt = 2000L
                )
            ),
            favoriteFolders = listOf(
                FavoriteFolder("content://tree/backup", "Backup", "", 2000L, 3)
            )
        )

        val result = BackupMerger.merge(local, incoming, RestoreMode.REPLACE)

        assertEquals(145f, result.globalDefaultSpeedPxPerSec, 0.001f)
        assertEquals(listOf("backup-id"), result.documents.map { it.id })
        assertEquals(listOf("content://pdf/backup"), result.documents.map { it.documentUri })
        assertEquals(listOf("content://tree/backup"), result.favoriteFolders.map { it.treeUri })
    }

    @Test
    fun mergeMode_preservesLocalAndResolvesSameUriOrIdWithoutDuplicates() {
        val local = BackupPayload(
            exportedAt = 1000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 90f, // built-in default -> adopts incoming custom global speed
            documents = listOf(
                PdfDocumentRecord.createSanitized(
                    id = "id-shared-local",
                    documentUri = "content://pdf/shared",
                    fileName = "shared.pdf",
                    pageCount = 50,
                    currentPageIndex = 10,
                    normalizedScrollRatio = 0.2f,
                    preferredSpeedPxPerSec = 120f,
                    customDefaultSpeedPxPerSec = null,
                    lastAccessedAt = 9000L
                ),
                PdfDocumentRecord.createSanitized(
                    id = "id-local-only",
                    documentUri = "content://pdf/local_only",
                    fileName = "local_only.pdf",
                    pageCount = 8,
                    currentPageIndex = 2,
                    lastAccessedAt = 3000L
                )
            ),
            favoriteFolders = listOf(
                FavoriteFolder("content://tree/local", "Local", "", 4000L, 2)
            )
        )

        val incoming = BackupPayload(
            exportedAt = 2000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 130f,
            documents = listOf(
                PdfDocumentRecord.createSanitized(
                    id = "id-shared-backup",
                    documentUri = "content://pdf/shared",
                    fileName = "shared.pdf",
                    pageCount = 50,
                    currentPageIndex = 3,
                    normalizedScrollRatio = 0.06f,
                    preferredSpeedPxPerSec = 100f,
                    customDefaultSpeedPxPerSec = 175f,
                    lastAccessedAt = 5000L
                ),
                PdfDocumentRecord.createSanitized(
                    id = "id-incoming-only",
                    documentUri = "content://pdf/incoming_only",
                    fileName = "incoming.pdf",
                    pageCount = 30,
                    currentPageIndex = 5,
                    customDefaultSpeedPxPerSec = 200f,
                    lastAccessedAt = 6000L
                )
            ),
            favoriteFolders = listOf(
                FavoriteFolder("content://tree/backup", "Backup", "", 7000L, 5)
            )
        )

        val merged = BackupMerger.merge(local, incoming, RestoreMode.MERGE)

        assertEquals(130f, merged.globalDefaultSpeedPxPerSec, 0.001f)
        assertEquals(3, merged.documents.size)
        assertEquals(3, merged.documents.map { it.id }.toSet().size)
        assertEquals(3, merged.documents.map { it.documentUri }.toSet().size)

        val shared = merged.documents.first { it.documentUri == "content://pdf/shared" }
        assertEquals(10, shared.currentPageIndex) // kept newer local reading position
        assertEquals(175f, shared.customDefaultSpeedPxPerSec ?: 0f, 0.001f) // preserved custom speed from backup

        assertEquals(2, merged.favoriteFolders.size)
        assertTrue(merged.favoriteFolders.any { it.treeUri == "content://tree/local" })
        assertTrue(merged.favoriteFolders.any { it.treeUri == "content://tree/backup" })
    }

    @Test
    fun mergeMode_matchesBySameIdEvenWhenUriDiffersWithoutDuplicatingId() {
        val local = BackupPayload(
            exportedAt = 1000L,
            appVersion = "1.0.0",
            documents = listOf(
                PdfDocumentRecord.createSanitized(
                    id = "same-uuid",
                    documentUri = "content://pdf/new_location.pdf",
                    fileName = "new_location.pdf",
                    pageCount = 20,
                    currentPageIndex = 8,
                    lastAccessedAt = 9000L
                )
            )
        )
        val incoming = BackupPayload(
            exportedAt = 2000L,
            appVersion = "1.0.0",
            documents = listOf(
                PdfDocumentRecord.createSanitized(
                    id = "same-uuid",
                    documentUri = "content://pdf/old_location.pdf",
                    fileName = "old_location.pdf",
                    pageCount = 20,
                    currentPageIndex = 2,
                    customDefaultSpeedPxPerSec = 165f,
                    lastAccessedAt = 4000L
                )
            )
        )

        val merged = BackupMerger.merge(local, incoming, RestoreMode.MERGE)
        assertEquals(1, merged.documents.size)
        assertEquals("same-uuid", merged.documents[0].id)
        assertEquals("content://pdf/new_location.pdf", merged.documents[0].documentUri)
        assertEquals(165f, merged.documents[0].customDefaultSpeedPxPerSec ?: 0f, 0.001f)
    }

    @Test
    fun mergeMode_preservesAlreadyCustomizedLocalGlobalSpeed() {
        val local = BackupPayload(
            exportedAt = 1000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 140f
        )
        val incoming = BackupPayload(
            exportedAt = 2000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 180f
        )

        val merged = BackupMerger.merge(local, incoming, RestoreMode.MERGE)
        assertEquals(140f, merged.globalDefaultSpeedPxPerSec, 0.001f)
    }

    @Test
    fun mergeMode_withEmptyBackup_preservesAllLocalData() {
        val local = BackupPayload(
            exportedAt = 5000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 135f,
            documents = listOf(
                PdfDocumentRecord.createSanitized(
                    id = "local-1",
                    documentUri = "content://pdf/local",
                    fileName = "local.pdf",
                    pageCount = 40,
                    currentPageIndex = 8,
                    normalizedScrollRatio = 0.2f,
                    customDefaultSpeedPxPerSec = 160f,
                    lastAccessedAt = 5000L
                )
            ),
            favoriteFolders = listOf(
                FavoriteFolder("content://tree/local", "Local", "primary:Local", 5000L, 2)
            )
        )
        val emptyIncoming = BackupPayload(
            exportedAt = 6000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 90f,
            documents = emptyList(),
            favoriteFolders = emptyList()
        )

        val merged = BackupMerger.merge(local, emptyIncoming, RestoreMode.MERGE)

        assertEquals(135f, merged.globalDefaultSpeedPxPerSec, 0.001f)
        assertEquals(local.documents, merged.documents)
        assertEquals(local.favoriteFolders, merged.favoriteFolders)
    }

    @Test
    fun mergeMode_crossCollisionIdAndUri_incomingNewer_mergesWithoutDuplicates() {
        val local = BackupPayload(
            exportedAt = 200L,
            appVersion = "1.0.0",
            documents = listOf(
                PdfDocumentRecord.createSanitized(
                    id = "A",
                    documentUri = "u1",
                    fileName = "a_old.pdf",
                    pageCount = 10,
                    currentPageIndex = 1,
                    customDefaultSpeedPxPerSec = null,
                    isInRecents = false,
                    lastAccessedAt = 100L
                ),
                PdfDocumentRecord.createSanitized(
                    id = "B",
                    documentUri = "u2",
                    fileName = "b_local.pdf",
                    pageCount = 10,
                    currentPageIndex = 4,
                    customDefaultSpeedPxPerSec = 180f,
                    isInRecents = true,
                    lastAccessedAt = 200L
                )
            )
        )
        val incoming = BackupPayload(
            exportedAt = 300L,
            appVersion = "1.0.0",
            documents = listOf(
                PdfDocumentRecord.createSanitized(
                    id = "A",
                    documentUri = "u2",
                    fileName = "a_incoming.pdf",
                    pageCount = 10,
                    currentPageIndex = 6,
                    customDefaultSpeedPxPerSec = null,
                    isInRecents = false,
                    lastAccessedAt = 300L
                )
            )
        )

        val merged = BackupMerger.merge(local, incoming, RestoreMode.MERGE)

        assertEquals(1, merged.documents.size)
        val doc = merged.documents.single()
        assertEquals("A", doc.id)
        assertEquals("u2", doc.documentUri)
        assertEquals(6, doc.currentPageIndex)
        assertEquals(180f, doc.customDefaultSpeedPxPerSec ?: 0f, 0.001f)
        assertTrue(doc.isInRecents)
    }

    @Test
    fun mergeMode_crossCollisionIdAndUri_localNewer_preservesLocalWinnerAndNoDuplicates() {
        val local = BackupPayload(
            exportedAt = 400L,
            appVersion = "1.0.0",
            documents = listOf(
                PdfDocumentRecord.createSanitized(
                    id = "A",
                    documentUri = "u1",
                    fileName = "a_old.pdf",
                    pageCount = 10,
                    currentPageIndex = 1,
                    customDefaultSpeedPxPerSec = 150f,
                    isInRecents = false,
                    lastAccessedAt = 100L
                ),
                PdfDocumentRecord.createSanitized(
                    id = "B",
                    documentUri = "u2",
                    fileName = "b_local_newer.pdf",
                    pageCount = 10,
                    currentPageIndex = 8,
                    customDefaultSpeedPxPerSec = null,
                    isInRecents = true,
                    lastAccessedAt = 400L
                )
            )
        )
        val incoming = BackupPayload(
            exportedAt = 300L,
            appVersion = "1.0.0",
            documents = listOf(
                PdfDocumentRecord.createSanitized(
                    id = "A",
                    documentUri = "u2",
                    fileName = "a_incoming.pdf",
                    pageCount = 10,
                    currentPageIndex = 6,
                    customDefaultSpeedPxPerSec = 180f,
                    isInRecents = false,
                    lastAccessedAt = 300L
                )
            )
        )

        val merged = BackupMerger.merge(local, incoming, RestoreMode.MERGE)

        assertEquals(1, merged.documents.size)
        val doc = merged.documents.single()
        assertEquals("B", doc.id)
        assertEquals("u2", doc.documentUri)
        assertEquals(8, doc.currentPageIndex)
        assertEquals(180f, doc.customDefaultSpeedPxPerSec ?: 0f, 0.001f)
        assertTrue(doc.isInRecents)
    }
}
