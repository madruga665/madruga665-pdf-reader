package com.madruga665.pdfreader.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FilenameSpeedReconcilerTest {

    @Test
    fun findCustomSpeedByFileName_matchesCaseInsensitivelyAndPicksLatestTimestamp() {
        val sessions = listOf(
            BackupReadingSession(
                documentUri = "content://pdf/old",
                fileName = " Clean_Architecture.pdf ",
                customDefaultSpeedPxPerSec = 130f,
                lastReadTimestamp = 1000L
            ),
            BackupReadingSession(
                documentUri = "content://pdf/newest_no_custom",
                fileName = "clean_architecture.pdf",
                customDefaultSpeedPxPerSec = null,
                lastReadTimestamp = 9000L
            ),
            BackupReadingSession(
                documentUri = "content://pdf/latest_with_custom",
                fileName = "CLEAN_ARCHITECTURE.PDF",
                customDefaultSpeedPxPerSec = 190f,
                lastReadTimestamp = 5000L
            )
        )

        val result = FilenameSpeedReconciler.findCustomSpeedByFileName("clean_architecture.pdf", sessions)
        assertEquals(190f, result)
    }

    @Test
    fun findCustomSpeedByFileName_returnsNullForBlankOrUnmatched() {
        val sessions = listOf(
            BackupReadingSession(
                documentUri = "content://pdf/1",
                fileName = "book.pdf",
                customDefaultSpeedPxPerSec = 150f,
                lastReadTimestamp = 1000L
            )
        )

        assertNull(FilenameSpeedReconciler.findCustomSpeedByFileName("   ", sessions))
        assertNull(FilenameSpeedReconciler.findCustomSpeedByFileName("other.pdf", sessions))
    }
}
