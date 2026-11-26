package com.madruga665.pdfreader.core.database

object PdfReaderDatabaseSchema {
    const val DATABASE_NAME = "pdf_reader.db"
    const val SCHEMA_VERSION = 1

    const val TABLE_PDF_DOCUMENTS = "pdf_documents"
    const val TABLE_FAVORITE_FOLDERS = "favorite_folders"
    const val TABLE_APP_SETTINGS = "app_settings"

    const val SETTING_GLOBAL_DEFAULT_SPEED = "global_default_speed"
    const val SETTING_DATASTORE_MIGRATION_COMPLETED = "datastore_migration_completed"
    const val SETTING_DATASTORE_MIGRATION_AT = "datastore_migration_at"
    const val SETTING_APP_VERSION = "app_version"
    const val SETTING_LAST_BACKUP_EXPORTED_AT = "last_backup_exported_at"

    const val MIGRATION_COMPLETED_VALUE = "1"

    val SQLITE_MAGIC_HEADER: ByteArray = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    val REQUIRED_TABLES: Set<String> = setOf(
        TABLE_PDF_DOCUMENTS,
        TABLE_FAVORITE_FOLDERS,
        TABLE_APP_SETTINGS
    )

    val REQUIRED_PDF_DOCUMENTS_COLUMNS: Set<String> = setOf(
        "id",
        "document_uri",
        "file_name",
        "page_count",
        "current_page_index",
        "normalized_scroll_ratio",
        "zoom_scale",
        "preferred_speed_px_per_sec",
        "custom_default_speed_px_per_sec",
        "last_accessed_at",
        "is_in_recents",
        "is_available",
        "created_at",
        "updated_at"
    )

    val REQUIRED_FAVORITE_FOLDERS_COLUMNS: Set<String> = setOf(
        "tree_uri",
        "display_name",
        "path_summary",
        "pinned_timestamp",
        "cached_file_count"
    )

    val REQUIRED_APP_SETTINGS_COLUMNS: Set<String> = setOf(
        "key",
        "value"
    )

    val DDL_STATEMENTS: List<String> = listOf(
        """
        CREATE TABLE IF NOT EXISTS $TABLE_PDF_DOCUMENTS (
            id TEXT PRIMARY KEY NOT NULL,
            document_uri TEXT NOT NULL UNIQUE,
            file_name TEXT NOT NULL,
            page_count INTEGER NOT NULL DEFAULT 1 CHECK (page_count >= 1),
            current_page_index INTEGER NOT NULL DEFAULT 0 CHECK (current_page_index >= 0),
            normalized_scroll_ratio REAL NOT NULL DEFAULT 0.0 CHECK (normalized_scroll_ratio >= 0.0 AND normalized_scroll_ratio <= 1.0),
            zoom_scale REAL NOT NULL DEFAULT 1.0 CHECK (zoom_scale >= 0.5 AND zoom_scale <= 4.0),
            preferred_speed_px_per_sec REAL NOT NULL DEFAULT 90.0 CHECK (preferred_speed_px_per_sec >= 10.0 AND preferred_speed_px_per_sec <= 500.0),
            custom_default_speed_px_per_sec REAL NULL CHECK (custom_default_speed_px_per_sec IS NULL OR (custom_default_speed_px_per_sec >= 10.0 AND custom_default_speed_px_per_sec <= 500.0)),
            last_accessed_at INTEGER NOT NULL,
            is_in_recents INTEGER NOT NULL DEFAULT 1 CHECK (is_in_recents IN (0, 1)),
            is_available INTEGER NOT NULL DEFAULT 1 CHECK (is_available IN (0, 1)),
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS idx_pdf_documents_recents
            ON $TABLE_PDF_DOCUMENTS (is_in_recents, last_accessed_at DESC)
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS idx_pdf_documents_file_name
            ON $TABLE_PDF_DOCUMENTS (file_name COLLATE NOCASE)
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS $TABLE_FAVORITE_FOLDERS (
            tree_uri TEXT PRIMARY KEY NOT NULL,
            display_name TEXT NOT NULL,
            path_summary TEXT NOT NULL DEFAULT '',
            pinned_timestamp INTEGER NOT NULL,
            cached_file_count INTEGER NOT NULL DEFAULT 0 CHECK (cached_file_count >= 0)
        )
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS idx_favorite_folders_pinned
            ON $TABLE_FAVORITE_FOLDERS (pinned_timestamp DESC)
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS $TABLE_APP_SETTINGS (
            key TEXT PRIMARY KEY NOT NULL,
            value TEXT NOT NULL
        )
        """.trimIndent()
    )

    fun createSchema(connection: SqliteConnection) {
        DDL_STATEMENTS.forEach { sql ->
            connection.execSQL(sql)
        }
        connection.execSQL("PRAGMA user_version = $SCHEMA_VERSION")
    }
}
