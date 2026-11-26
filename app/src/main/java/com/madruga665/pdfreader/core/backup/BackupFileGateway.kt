package com.madruga665.pdfreader.core.backup

interface BackupFileGateway {
    suspend fun writeText(uriString: String, content: String): Result<Unit>
    suspend fun readText(uriString: String): Result<String>
}
