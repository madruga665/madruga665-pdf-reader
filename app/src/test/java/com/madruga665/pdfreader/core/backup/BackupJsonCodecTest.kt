package com.madruga665.pdfreader.core.backup

import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.RecentDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class BackupJsonCodecTest {

    @Test
    fun encodeAndDecode_roundTripsCompletePayload() {
        val payload = BackupPayload(
            schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION,
            exportedAt = 1764100000000L,
            appVersion = "1.0.0",
            globalDefaultSpeedPxPerSec = 125.0f,
            readingSessions = listOf(
                BackupReadingSession(
                    documentUri = "content://pdf/1",
                    fileName = "ebook.pdf",
                    currentPageIndex = 12,
                    normalizedScrollRatio = 0.45f,
                    zoomScale = 1.5f,
                    preferredSpeedPxPerSec = 140.0f,
                    customDefaultSpeedPxPerSec = 165.0f,
                    lastReadTimestamp = 1764099000000L
                ),
                BackupReadingSession(
                    documentUri = "content://pdf/2",
                    fileName = "manual.pdf",
                    currentPageIndex = 0,
                    normalizedScrollRatio = 0.0f,
                    zoomScale = 1.0f,
                    preferredSpeedPxPerSec = 90.0f,
                    customDefaultSpeedPxPerSec = null,
                    lastReadTimestamp = 1764098000000L
                )
            ),
            recentDocuments = listOf(
                RecentDocument(
                    uri = "content://pdf/1",
                    fileName = "ebook.pdf",
                    pageCount = 100,
                    lastReadPageIndex = 12,
                    progressRatio = 0.45f,
                    lastAccessedTimestamp = 1764099000000L,
                    isAvailable = true
                )
            ),
            favoriteFolders = listOf(
                FavoriteFolder(
                    treeUri = "content://tree/books",
                    displayName = "Books",
                    pathSummary = "primary:Books",
                    pinnedTimestamp = 1764097000000L,
                    cachedFileCount = 8
                )
            )
        )

        val json = BackupJsonCodec.encode(payload)
        val decoded = BackupJsonCodec.decode(json)

        assertTrue(decoded is BackupParseResult.Success)
        val success = decoded as BackupParseResult.Success
        assertEquals(1, success.payload.schemaVersion)
        assertEquals(1764100000000L, success.payload.exportedAt)
        assertEquals("1.0.0", success.payload.appVersion)
        assertEquals(125.0f, success.payload.globalDefaultSpeedPxPerSec, 0.001f)
        assertEquals(2, success.payload.readingSessions.size)
        assertEquals("ebook.pdf", success.payload.readingSessions[0].fileName)
        assertEquals(165.0f, success.payload.readingSessions[0].customDefaultSpeedPxPerSec)
        assertNull(success.payload.readingSessions[1].customDefaultSpeedPxPerSec)
        assertEquals(1, success.payload.recentDocuments.size)
        assertEquals(1, success.payload.favoriteFolders.size)
        assertEquals(2, success.summary.sessionsCount)
        assertEquals(1, success.summary.customSpeedsCount)
        assertEquals(1, success.summary.recentsCount)
        assertEquals(1, success.summary.favoriteFoldersCount)
    }

    @Test
    fun decode_rejectsEmptyOrBlankInput() {
        val result = BackupJsonCodec.decode("   \n\t  ")
        assertTrue(result is BackupParseResult.Failure)
        assertEquals(BackupErrorReason.EMPTY_FILE, (result as BackupParseResult.Failure).reason)
    }

    @Test
    fun decode_rejectsInvalidJson() {
        val result = BackupJsonCodec.decode("{ not valid json [")
        assertTrue(result is BackupParseResult.Failure)
        assertEquals(BackupErrorReason.INVALID_JSON, (result as BackupParseResult.Failure).reason)
    }

    @Test
    fun decode_rejectsIncompatibleSchemaVersion() {
        val futureJson = """
            {
              "schemaVersion": 99,
              "exportedAt": 1764100000000,
              "appVersion": "2.0.0",
              "preferences": { "globalDefaultSpeedPxPerSec": 90.0, "readingSessions": [] },
              "recentDocuments": [],
              "favoriteFolders": []
            }
        """.trimIndent()

        val result = BackupJsonCodec.decode(futureJson)
        assertTrue(result is BackupParseResult.Failure)
        assertEquals(
            BackupErrorReason.UNSUPPORTED_SCHEMA_VERSION,
            (result as BackupParseResult.Failure).reason
        )
    }

    @Test
    fun decode_rejectsMissingRequiredStructure() {
        val missingPrefs = """
            {
              "schemaVersion": 1,
              "exportedAt": 1764100000000
            }
        """.trimIndent()

        val result = BackupJsonCodec.decode(missingPrefs)
        assertTrue(result is BackupParseResult.Failure)
        assertEquals(
            BackupErrorReason.INVALID_STRUCTURE,
            (result as BackupParseResult.Failure).reason
        )
    }

    @Test
    fun decode_sanitizesOutOfRangeNumericValuesAndCapsRecentsAt30() {
        val recentsJson = (1..35).joinToString(",") { idx ->
            """
                {
                  "uri": "content://pdf/$idx",
                  "fileName": "doc_$idx.pdf",
                  "pageCount": 0,
                  "lastReadPageIndex": 99,
                  "progressRatio": 5.0,
                  "lastAccessedTimestamp": ${idx * 1000L},
                  "isAvailable": true
                }
            """.trimIndent()
        }

        val dirtyJson = """
            {
              "schemaVersion": 1,
              "exportedAt": 1764100000000,
              "appVersion": "1.0.0",
              "preferences": {
                "globalDefaultSpeedPxPerSec": 9999.0,
                "readingSessions": [
                  {
                    "documentUri": "content://pdf/clamped",
                    "fileName": "  clamped.pdf  ",
                    "currentPageIndex": -5,
                    "normalizedScrollRatio": -0.8,
                    "zoomScale": 99.0,
                    "preferredSpeedPxPerSec": 2.0,
                    "customDefaultSpeedPxPerSec": 800.0,
                    "lastReadTimestamp": 0
                  }
                ]
              },
              "recentDocuments": [$recentsJson],
              "favoriteFolders": [
                {
                  "treeUri": "content://tree/a",
                  "displayName": "Folder A",
                  "pathSummary": "primary:A",
                  "pinnedTimestamp": 1000,
                  "cachedFileCount": -4
                }
              ]
            }
        """.trimIndent()

        val result = BackupJsonCodec.decode(dirtyJson)
        assertTrue(result is BackupParseResult.Success)
        val payload = (result as BackupParseResult.Success).payload

        assertEquals(500.0f, payload.globalDefaultSpeedPxPerSec, 0.001f)
        val session = payload.readingSessions.single()
        assertEquals("clamped.pdf", session.fileName)
        assertEquals(0, session.currentPageIndex)
        assertEquals(0.0f, session.normalizedScrollRatio, 0.001f)
        assertEquals(4.0f, session.zoomScale, 0.001f)
        assertEquals(10.0f, session.preferredSpeedPxPerSec, 0.001f)
        assertEquals(500.0f, session.customDefaultSpeedPxPerSec ?: 0f, 0.001f)
        assertTrue(session.lastReadTimestamp > 0L)

        assertEquals(30, payload.recentDocuments.size)
        assertEquals("content://pdf/35", payload.recentDocuments.first().uri)
        assertEquals(1, payload.recentDocuments.first().pageCount)
        assertEquals(0, payload.recentDocuments.first().lastReadPageIndex)
        assertEquals(1.0f, payload.recentDocuments.first().progressRatio, 0.001f)

        assertEquals(0, payload.favoriteFolders.single().cachedFileCount)
    }

    @Test
    fun suggestedBackupFileName_formatsTimestampCorrectly() {
        // 2026-11-25 18:30:00 UTC
        val epochMillis = 1795631400000L
        val fileName = BackupJsonCodec.suggestedBackupFileName(epochMillis, ZoneOffset.UTC)
        assertEquals("pdf-reader-backup-20261125-1830.json", fileName)
    }

    @Test
    fun decode_deduplicatesDuplicateUrisWithinSameBackupFile() {
        val jsonWithDuplicates = """
            {
              "schemaVersion": 1,
              "exportedAt": 1764100000000,
              "appVersion": "1.0.0",
              "preferences": {
                "globalDefaultSpeedPxPerSec": 100.0,
                "readingSessions": [
                  {
                    "documentUri": "content://pdf/dup",
                    "fileName": "dup.pdf",
                    "currentPageIndex": 2,
                    "normalizedScrollRatio": 0.2,
                    "zoomScale": 1.0,
                    "preferredSpeedPxPerSec": 110.0,
                    "customDefaultSpeedPxPerSec": 175.0,
                    "lastReadTimestamp": 1000
                  },
                  {
                    "documentUri": "content://pdf/dup",
                    "fileName": "",
                    "currentPageIndex": 9,
                    "normalizedScrollRatio": 0.9,
                    "zoomScale": 1.5,
                    "preferredSpeedPxPerSec": 120.0,
                    "customDefaultSpeedPxPerSec": null,
                    "lastReadTimestamp": 5000
                  }
                ]
              },
              "recentDocuments": [
                {
                  "uri": "content://pdf/dup",
                  "fileName": "dup.pdf",
                  "pageCount": 20,
                  "lastReadPageIndex": 2,
                  "progressRatio": 0.1,
                  "lastAccessedTimestamp": 1000
                },
                {
                  "uri": "content://pdf/dup",
                  "fileName": "dup.pdf",
                  "pageCount": 20,
                  "lastReadPageIndex": 9,
                  "progressRatio": 0.45,
                  "lastAccessedTimestamp": 5000
                }
              ],
              "favoriteFolders": [
                {
                  "treeUri": "content://tree/dup",
                  "displayName": "Old Name",
                  "pathSummary": "primary:Old",
                  "pinnedTimestamp": 1000,
                  "cachedFileCount": 1
                },
                {
                  "treeUri": "content://tree/dup",
                  "displayName": "New Name",
                  "pathSummary": "primary:New",
                  "pinnedTimestamp": 5000,
                  "cachedFileCount": 5
                }
              ]
            }
        """.trimIndent()

        val result = BackupJsonCodec.decode(jsonWithDuplicates)
        assertTrue(result is BackupParseResult.Success)
        val payload = (result as BackupParseResult.Success).payload

        assertEquals(1, payload.readingSessions.size)
        val session = payload.readingSessions.single()
        assertEquals(9, session.currentPageIndex)
        assertEquals("dup.pdf", session.fileName)
        assertEquals(175.0f, session.customDefaultSpeedPxPerSec ?: 0f, 0.001f)

        assertEquals(1, payload.recentDocuments.size)
        assertEquals(9, payload.recentDocuments.single().lastReadPageIndex)

        assertEquals(1, payload.favoriteFolders.size)
        assertEquals("New Name", payload.favoriteFolders.single().displayName)
    }

    @Test
    fun decode_rejectsNonIntegerSchemaVersion() {
        val stringVersionJson = """
            {
              "schemaVersion": "1",
              "exportedAt": 1764100000000,
              "appVersion": "1.0.0",
              "preferences": { "globalDefaultSpeedPxPerSec": 90.0, "readingSessions": [] },
              "recentDocuments": [],
              "favoriteFolders": []
            }
        """.trimIndent()

        val floatVersionJson = """
            {
              "schemaVersion": 1.5,
              "exportedAt": 1764100000000,
              "appVersion": "1.0.0",
              "preferences": { "globalDefaultSpeedPxPerSec": 90.0, "readingSessions": [] },
              "recentDocuments": [],
              "favoriteFolders": []
            }
        """.trimIndent()

        val resString = BackupJsonCodec.decode(stringVersionJson)
        assertTrue(resString is BackupParseResult.Failure)
        assertEquals(BackupErrorReason.INVALID_STRUCTURE, (resString as BackupParseResult.Failure).reason)

        val resFloat = BackupJsonCodec.decode(floatVersionJson)
        assertTrue(resFloat is BackupParseResult.Failure)
        assertEquals(BackupErrorReason.INVALID_STRUCTURE, (resFloat as BackupParseResult.Failure).reason)
    }
}
