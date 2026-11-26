package com.madruga665.pdfreader.core.backup

object FilenameSpeedReconciler {

    fun findCustomSpeedByFileName(
        fileName: String,
        sessions: List<BackupReadingSession>
    ): Float? {
        val normalizedTarget = fileName.trim()
        if (normalizedTarget.isEmpty()) return null

        return sessions
            .asSequence()
            .filter { it.customDefaultSpeedPxPerSec != null }
            .filter { it.fileName.trim().equals(normalizedTarget, ignoreCase = true) }
            .maxByOrNull { it.lastReadTimestamp }
            ?.customDefaultSpeedPxPerSec
            ?.takeIf { it.isFinite() }
            ?.coerceIn(10.0f, 500.0f)
    }
}
