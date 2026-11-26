package com.madruga665.pdfreader.core.backup

import com.madruga665.pdfreader.core.database.PdfReaderDatabaseSchema
import com.madruga665.pdfreader.core.database.SqliteAppDatabase
import com.madruga665.pdfreader.core.database.SqliteConnection
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import com.madruga665.pdfreader.core.model.SpeedPreset
import java.io.File
import java.io.FileInputStream
import java.util.UUID

class SqliteBackupValidator(
    private val database: SqliteAppDatabase
) {

    suspend fun validateAndRead(stagingFile: File): BackupParseResult {
        if (!stagingFile.exists() || stagingFile.length() == 0L) {
            return BackupParseResult.Failure(
                reason = BackupErrorReason.EMPTY_FILE,
                message = "O arquivo selecionado está vazio."
            )
        }

        if (!hasValidSqliteHeader(stagingFile)) {
            return BackupParseResult.Failure(
                reason = BackupErrorReason.INVALID_SQLITE_FILE,
                message = "Arquivo selecionado não é um banco de dados SQLite válido (.db)."
            )
        }

        val inspected = database.inspectExternalDatabase(stagingFile) { conn ->
            inspectConnection(conn)
        }

        return inspected.getOrElse {
            BackupParseResult.Failure(
                reason = BackupErrorReason.CORRUPTED_DATABASE,
                message = "O banco de dados SQLite selecionado está corrompido ou ilegível."
            )
        }
    }

    private fun hasValidSqliteHeader(file: File): Boolean {
        val expected = PdfReaderDatabaseSchema.SQLITE_MAGIC_HEADER
        if (file.length() < expected.size) return false
        val actual = ByteArray(expected.size)
        return try {
            FileInputStream(file).use { input ->
                val read = input.read(actual)
                read == expected.size && actual.contentEquals(expected)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun inspectConnection(conn: SqliteConnection): BackupParseResult {
        val integrity = try {
            conn.rawQuery("PRAGMA integrity_check").use { cursor ->
                if (cursor.moveToNext()) cursor.getString(0) else ""
            }
        } catch (_: Exception) {
            return BackupParseResult.Failure(
                reason = BackupErrorReason.CORRUPTED_DATABASE,
                message = "O banco de dados SQLite selecionado está corrompido ou ilegível."
            )
        }

        if (!integrity.equals("ok", ignoreCase = true)) {
            return BackupParseResult.Failure(
                reason = BackupErrorReason.CORRUPTED_DATABASE,
                message = "Falha na verificação de integridade do banco de dados SQLite."
            )
        }

        val userVersion = conn.rawQuery("PRAGMA user_version").use { cursor ->
            if (cursor.moveToNext()) cursor.getInt(0) else 0
        }
        if (userVersion !in 1..PdfReaderDatabaseSchema.SCHEMA_VERSION) {
            return BackupParseResult.Failure(
                reason = BackupErrorReason.UNSUPPORTED_SCHEMA_VERSION,
                message = "Versão de esquema SQLite incompatível ($userVersion)."
            )
        }

        val tables = mutableSetOf<String>()
        conn.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0))
            }
        }
        if (!tables.containsAll(PdfReaderDatabaseSchema.REQUIRED_TABLES)) {
            return BackupParseResult.Failure(
                reason = BackupErrorReason.INVALID_STRUCTURE,
                message = "O arquivo SQLite selecionado não possui as tabelas esperadas do PDF Reader."
            )
        }

        if (!hasRequiredColumns(conn, PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS, PdfReaderDatabaseSchema.REQUIRED_PDF_DOCUMENTS_COLUMNS) ||
            !hasRequiredColumns(conn, PdfReaderDatabaseSchema.TABLE_FAVORITE_FOLDERS, PdfReaderDatabaseSchema.REQUIRED_FAVORITE_FOLDERS_COLUMNS) ||
            !hasRequiredColumns(conn, PdfReaderDatabaseSchema.TABLE_APP_SETTINGS, PdfReaderDatabaseSchema.REQUIRED_APP_SETTINGS_COLUMNS)
        ) {
            return BackupParseResult.Failure(
                reason = BackupErrorReason.INVALID_STRUCTURE,
                message = "O arquivo SQLite selecionado possui estrutura incompatível."
            )
        }

        val settings = readSettings(conn)
        val globalSpeed = PdfDocumentRecord.sanitizeSpeed(
            settings[PdfReaderDatabaseSchema.SETTING_GLOBAL_DEFAULT_SPEED]?.toFloatOrNull()
                ?: SpeedPreset.MEDIUM.speedPxPerSec
        )
        val appVersion = settings[PdfReaderDatabaseSchema.SETTING_APP_VERSION]
            ?.takeIf { it.isNotBlank() } ?: "1.0.0"
        val exportedAt = settings[PdfReaderDatabaseSchema.SETTING_LAST_BACKUP_EXPORTED_AT]
            ?.toLongOrNull()
            ?.takeIf { it > 0L }
            ?: System.currentTimeMillis()

        val documents = readDocuments(conn)
        val folders = readFavoriteFolders(conn)

        val payload = BackupPayload(
            schemaVersion = userVersion,
            exportedAt = exportedAt,
            appVersion = appVersion,
            globalDefaultSpeedPxPerSec = globalSpeed,
            documents = documents,
            favoriteFolders = folders
        )
        return BackupParseResult.Success(payload = payload, summary = payload.toSummary())
    }

    private fun hasRequiredColumns(
        conn: SqliteConnection,
        tableName: String,
        requiredColumns: Set<String>
    ): Boolean {
        val found = mutableSetOf<String>()
        conn.rawQuery("PRAGMA table_info($tableName)").use { cursor ->
            while (cursor.moveToNext()) {
                found.add(cursor.getString(1))
            }
        }
        return found.containsAll(requiredColumns)
    }

    private fun readSettings(conn: SqliteConnection): Map<String, String> {
        val map = mutableMapOf<String, String>()
        conn.rawQuery("SELECT key, value FROM ${PdfReaderDatabaseSchema.TABLE_APP_SETTINGS}").use { cursor ->
            while (cursor.moveToNext()) {
                map[cursor.getString(0)] = cursor.getString(1)
            }
        }
        return map
    }

    private fun readDocuments(conn: SqliteConnection): List<PdfDocumentRecord> {
        val list = mutableListOf<PdfDocumentRecord>()
        conn.rawQuery(
            """
            SELECT id, document_uri, file_name, page_count, current_page_index,
                   normalized_scroll_ratio, zoom_scale, preferred_speed_px_per_sec,
                   custom_default_speed_px_per_sec, last_accessed_at,
                   is_in_recents, is_available, created_at, updated_at
            FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}
            ORDER BY last_accessed_at DESC
            """.trimIndent()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val uri = cursor.getString(1).trim()
                if (uri.isBlank()) continue
                list.add(
                    PdfDocumentRecord.createSanitized(
                        id = cursor.getString(0).ifBlank { UUID.randomUUID().toString() },
                        documentUri = uri,
                        fileName = cursor.getString(2),
                        pageCount = cursor.getInt(3),
                        currentPageIndex = cursor.getInt(4),
                        normalizedScrollRatio = cursor.getDouble(5).toFloat(),
                        zoomScale = cursor.getDouble(6).toFloat(),
                        preferredSpeedPxPerSec = cursor.getDouble(7).toFloat(),
                        customDefaultSpeedPxPerSec = cursor.getFloatOrNull(8),
                        lastAccessedAt = cursor.getLong(9),
                        isInRecents = cursor.getInt(10) != 0,
                        isAvailable = cursor.getInt(11) != 0,
                        createdAt = cursor.getLong(12),
                        updatedAt = cursor.getLong(13)
                    )
                )
            }
        }
        return list
    }

    private fun readFavoriteFolders(conn: SqliteConnection): List<FavoriteFolder> {
        val list = mutableListOf<FavoriteFolder>()
        conn.rawQuery(
            """
            SELECT tree_uri, display_name, path_summary, pinned_timestamp, cached_file_count
            FROM ${PdfReaderDatabaseSchema.TABLE_FAVORITE_FOLDERS}
            ORDER BY pinned_timestamp DESC
            """.trimIndent()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val treeUri = cursor.getString(0).trim()
                val displayName = cursor.getString(1).trim()
                if (treeUri.isBlank() || displayName.isBlank()) continue
                list.add(
                    FavoriteFolder(
                        treeUri = treeUri,
                        displayName = displayName,
                        pathSummary = cursor.getString(2),
                        pinnedTimestamp = PdfDocumentRecord.sanitizeTimestamp(cursor.getLong(3)),
                        cachedFileCount = cursor.getInt(4).coerceAtLeast(0)
                    )
                )
            }
        }
        return list
    }
}
