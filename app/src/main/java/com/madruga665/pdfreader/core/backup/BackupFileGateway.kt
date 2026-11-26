package com.madruga665.pdfreader.core.backup

import java.io.File

interface BackupFileGateway {
    suspend fun writeFileToUri(uriString: String, sourceFile: File): Result<Unit>
    suspend fun copyUriToTempFile(
        uriString: String,
        destinationFile: File,
        maxBytes: Long = MAX_BACKUP_BYTES
    ): Result<Unit>

    companion object {
        const val MAX_BACKUP_BYTES: Long = 20L * 1024L * 1024L
    }
}
