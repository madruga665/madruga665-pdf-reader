package com.madruga665.pdfreader.core.backup

import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.RecentDocument
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
            readingSessions = listOf(
                BackupReadingSession(documentUri = "content://pdf/local", fileName = "local.pdf", lastReadTimestamp = 5000L)
            ),
            recentDocuments = listOf(
                RecentDocument("content://pdf/local", "local.pdf", 10, 1, 0.1f, 5000L)
            ),
            favoriteFolders = listOf(
                FavoriteFolder("content://tree/local", "Local", "", 5000L, 1)
            )
        )
        val incoming = BackupPayload(
            exportedAt = 2000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 145f,
            readingSessions = listOf(
                BackupReadingSession(documentUri = "content://pdf/backup", fileName = "backup.pdf", customDefaultSpeedPxPerSec = 180f, lastReadTimestamp = 2000L)
            ),
            recentDocuments = listOf(
                RecentDocument("content://pdf/backup", "backup.pdf", 20, 4, 0.2f, 2000L)
            ),
            favoriteFolders = listOf(
                FavoriteFolder("content://tree/backup", "Backup", "", 2000L, 3)
            )
        )

        val result = BackupMerger.merge(local, incoming, RestoreMode.REPLACE)

        assertEquals(145f, result.globalDefaultSpeedPxPerSec, 0.001f)
        assertEquals(listOf("content://pdf/backup"), result.readingSessions.map { it.documentUri })
        assertEquals(listOf("content://pdf/backup"), result.recentDocuments.map { it.uri })
        assertEquals(listOf("content://tree/backup"), result.favoriteFolders.map { it.treeUri })
    }

    @Test
    fun mergeMode_preservesLocalAndResolvesSameUriByTimestampAndCustomSpeed() {
        val local = BackupPayload(
            exportedAt = 1000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 90f, // built-in default -> adopts incoming custom global speed
            readingSessions = listOf(
                BackupReadingSession(
                    documentUri = "content://pdf/shared",
                    fileName = "shared.pdf",
                    currentPageIndex = 10,
                    preferredSpeedPxPerSec = 120f,
                    customDefaultSpeedPxPerSec = null,
                    lastReadTimestamp = 9000L // newer locally, but no custom speed
                ),
                BackupReadingSession(
                    documentUri = "content://pdf/local_only",
                    fileName = "local_only.pdf",
                    currentPageIndex = 2,
                    lastReadTimestamp = 3000L
                )
            ),
            recentDocuments = listOf(
                RecentDocument("content://pdf/shared", "shared.pdf", 50, 10, 0.2f, 9000L)
            ),
            favoriteFolders = listOf(
                FavoriteFolder("content://tree/local", "Local", "", 4000L, 2)
            )
        )

        val incoming = BackupPayload(
            exportedAt = 2000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 130f,
            readingSessions = listOf(
                BackupReadingSession(
                    documentUri = "content://pdf/shared",
                    fileName = "shared.pdf",
                    currentPageIndex = 3,
                    preferredSpeedPxPerSec = 100f,
                    customDefaultSpeedPxPerSec = 175f, // older, but has custom speed
                    lastReadTimestamp = 5000L
                ),
                BackupReadingSession(
                    documentUri = "content://pdf/incoming_only",
                    fileName = "incoming.pdf",
                    currentPageIndex = 5,
                    customDefaultSpeedPxPerSec = 200f,
                    lastReadTimestamp = 6000L
                )
            ),
            recentDocuments = listOf(
                RecentDocument("content://pdf/shared", "shared.pdf", 50, 3, 0.06f, 5000L),
                RecentDocument("content://pdf/incoming_only", "incoming.pdf", 30, 5, 0.16f, 6000L)
            ),
            favoriteFolders = listOf(
                FavoriteFolder("content://tree/backup", "Backup", "", 7000L, 5)
            )
        )

        val merged = BackupMerger.merge(local, incoming, RestoreMode.MERGE)

        assertEquals(130f, merged.globalDefaultSpeedPxPerSec, 0.001f)
        assertEquals(3, merged.readingSessions.size)
        val shared = merged.readingSessions.first { it.documentUri == "content://pdf/shared" }
        assertEquals(10, shared.currentPageIndex) // kept newer local reading position
        assertEquals(175f, shared.customDefaultSpeedPxPerSec) // preserved custom speed from backup

        assertEquals(2, merged.recentDocuments.size)
        assertEquals("content://pdf/shared", merged.recentDocuments.first().uri)
        assertEquals(10, merged.recentDocuments.first().lastReadPageIndex)

        assertEquals(2, merged.favoriteFolders.size)
        assertTrue(merged.favoriteFolders.any { it.treeUri == "content://tree/local" })
        assertTrue(merged.favoriteFolders.any { it.treeUri == "content://tree/backup" })
    }

    @Test
    fun mergeMode_capsRecentDocumentsAt30OrderedByRecency() {
        val localRecents = (1..20).map { i ->
            RecentDocument("content://pdf/local_$i", "l_$i.pdf", 10, 0, 0f, i * 1000L)
        }
        val incomingRecents = (1..20).map { i ->
            RecentDocument("content://pdf/in_$i", "i_$i.pdf", 10, 0, 0f, (i + 20) * 1000L)
        }

        val merged = BackupMerger.merge(
            local = BackupPayload(exportedAt = 1L, appVersion = "1.0.0", recentDocuments = localRecents),
            incoming = BackupPayload(exportedAt = 2L, appVersion = "1.0.0", recentDocuments = incomingRecents),
            mode = RestoreMode.MERGE
        )

        assertEquals(30, merged.recentDocuments.size)
        assertEquals("content://pdf/in_20", merged.recentDocuments.first().uri)
        assertEquals(40000L, merged.recentDocuments.first().lastAccessedTimestamp)
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
            readingSessions = listOf(
                BackupReadingSession(
                    documentUri = "content://pdf/local",
                    fileName = "local.pdf",
                    currentPageIndex = 8,
                    customDefaultSpeedPxPerSec = 160f,
                    lastReadTimestamp = 5000L
                )
            ),
            recentDocuments = listOf(
                RecentDocument("content://pdf/local", "local.pdf", 40, 8, 0.2f, 5000L)
            ),
            favoriteFolders = listOf(
                FavoriteFolder("content://tree/local", "Local", "primary:Local", 5000L, 2)
            )
        )
        val emptyIncoming = BackupPayload(
            exportedAt = 6000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 90f,
            readingSessions = emptyList(),
            recentDocuments = emptyList(),
            favoriteFolders = emptyList()
        )

        val merged = BackupMerger.merge(local, emptyIncoming, RestoreMode.MERGE)

        assertEquals(135f, merged.globalDefaultSpeedPxPerSec, 0.001f)
        assertEquals(local.readingSessions, merged.readingSessions)
        assertEquals(local.recentDocuments, merged.recentDocuments)
        assertEquals(local.favoriteFolders, merged.favoriteFolders)
    }
}
