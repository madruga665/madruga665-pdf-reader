package com.madruga665.pdfreader.core.storage

import com.madruga665.pdfreader.core.database.PdfReaderDatabaseSchema
import com.madruga665.pdfreader.core.database.SqliteAppDatabase
import com.madruga665.pdfreader.core.database.SqliteConnection
import com.madruga665.pdfreader.core.database.SqliteCursor
import com.madruga665.pdfreader.core.documents.DocumentRelinkPolicy
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import com.madruga665.pdfreader.core.model.ReadingSession
import com.madruga665.pdfreader.core.model.SpeedPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SqlitePdfDocumentStore(
    private val database: SqliteAppDatabase,
    private val uuidGenerator: () -> String = { UUID.randomUUID().toString() },
    private val awaitReady: suspend () -> Unit = {}
) : PdfDocumentStore {

    private val changeSignal = MutableStateFlow(0L)

    private fun notifyChanged() {
        changeSignal.value = changeSignal.value + 1L
    }

    override fun observeRecents(limit: Int): Flow<List<PdfDocumentRecord>> =
        changeSignal.map {
            awaitReady()
            database.withConnection(readOnly = true) { conn ->
                queryDocuments(
                    conn = conn,
                    sql = """
                        SELECT $DOCUMENT_COLUMNS
                        FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}
                        WHERE is_in_recents = 1
                        ORDER BY last_accessed_at DESC
                        LIMIT ?
                    """.trimIndent(),
                    args = arrayOf(limit.coerceAtLeast(1).toString())
                )
            }
        }

    override fun observeAllDocuments(): Flow<List<PdfDocumentRecord>> =
        changeSignal.map { getAllDocumentsSnapshot() }

    override suspend fun getAllDocumentsSnapshot(): List<PdfDocumentRecord> {
        awaitReady()
        return database.withConnection(readOnly = true) { conn ->
            queryDocuments(
                conn = conn,
                sql = """
                    SELECT $DOCUMENT_COLUMNS
                    FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}
                    ORDER BY last_accessed_at DESC
                """.trimIndent()
            )
        }
    }

    override suspend fun getById(id: String): PdfDocumentRecord? {
        if (id.isBlank()) return null
        awaitReady()
        return database.withConnection(readOnly = true) { conn ->
            querySingleDocument(
                conn = conn,
                sql = "SELECT $DOCUMENT_COLUMNS FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} WHERE id = ? LIMIT 1",
                args = arrayOf(id)
            )
        }
    }

    override suspend fun getByUri(documentUri: String): PdfDocumentRecord? {
        if (documentUri.isBlank()) return null
        awaitReady()
        return database.withConnection(readOnly = true) { conn ->
            querySingleDocument(
                conn = conn,
                sql = "SELECT $DOCUMENT_COLUMNS FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} WHERE document_uri = ? LIMIT 1",
                args = arrayOf(documentUri)
            )
        }
    }

    override suspend fun registerOrOpenDocument(
        uri: String,
        fileName: String,
        pageCount: Int,
        nowMillis: Long
    ): PdfDocumentRecord {
        val safeUri = uri.trim()
        require(safeUri.isNotBlank()) { "documentUri cannot be blank" }
        awaitReady()
        val safeFileName = PdfDocumentRecord.sanitizeFileName(fileName)
        val safePageCount = PdfDocumentRecord.sanitizePageCount(pageCount)
        val safeNow = PdfDocumentRecord.sanitizeTimestamp(nowMillis)

        val record = database.withConnection { conn ->
            conn.inTransaction { tx ->
                val existing = querySingleDocument(
                    conn = tx,
                    sql = "SELECT $DOCUMENT_COLUMNS FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} WHERE document_uri = ? LIMIT 1",
                    args = arrayOf(safeUri)
                )
                if (existing != null) {
                    val adjustedPage = PdfDocumentRecord.sanitizePageIndex(existing.currentPageIndex, safePageCount)
                    val updated = existing.copy(
                        fileName = safeFileName,
                        pageCount = safePageCount,
                        currentPageIndex = adjustedPage,
                        lastAccessedAt = safeNow,
                        isInRecents = true,
                        isAvailable = true,
                        updatedAt = safeNow
                    )
                    tx.execSQL(
                        """
                        UPDATE ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}
                        SET file_name = ?,
                            page_count = ?,
                            current_page_index = ?,
                            last_accessed_at = ?,
                            is_in_recents = 1,
                            is_available = 1,
                            updated_at = ?
                        WHERE id = ?
                        """.trimIndent(),
                        arrayOf(
                            updated.fileName,
                            updated.pageCount,
                            updated.currentPageIndex,
                            updated.lastAccessedAt,
                            updated.updatedAt,
                            updated.id
                        )
                    )
                    updated
                } else {
                    val globalSpeed = readGlobalDefaultSpeed(tx)
                    val created = PdfDocumentRecord.createSanitized(
                        id = uuidGenerator(),
                        documentUri = safeUri,
                        fileName = safeFileName,
                        pageCount = safePageCount,
                        currentPageIndex = 0,
                        normalizedScrollRatio = 0.0f,
                        zoomScale = PdfDocumentRecord.DEFAULT_ZOOM_SCALE,
                        preferredSpeedPxPerSec = globalSpeed,
                        customDefaultSpeedPxPerSec = null,
                        lastAccessedAt = safeNow,
                        isInRecents = true,
                        isAvailable = true,
                        createdAt = safeNow,
                        updatedAt = safeNow
                    )
                    insertDocumentRow(tx, created)
                    created
                }
            }
        }
        notifyChanged()
        return record
    }

    override suspend fun updateReadingProgress(
        documentId: String,
        currentPageIndex: Int,
        normalizedScrollRatio: Float,
        zoomScale: Float,
        preferredSpeedPxPerSec: Float,
        nowMillis: Long
    ) {
        if (documentId.isBlank()) return
        awaitReady()
        val safeNow = PdfDocumentRecord.sanitizeTimestamp(nowMillis)
        database.withConnection { conn ->
            conn.inTransaction { tx ->
                val existing = querySingleDocument(
                    conn = tx,
                    sql = "SELECT $DOCUMENT_COLUMNS FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} WHERE id = ? LIMIT 1",
                    args = arrayOf(documentId)
                ) ?: return@inTransaction

                val safePage = PdfDocumentRecord.sanitizePageIndex(currentPageIndex, existing.pageCount)
                val safeRatio = PdfDocumentRecord.sanitizeScrollRatio(normalizedScrollRatio)
                val safeZoom = PdfDocumentRecord.sanitizeZoom(zoomScale)
                val safeSpeed = PdfDocumentRecord.sanitizeSpeed(preferredSpeedPxPerSec)

                // CRITICAL: Never overwrite custom_default_speed_px_per_sec during progress updates.
                tx.execSQL(
                    """
                    UPDATE ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}
                    SET current_page_index = ?,
                        normalized_scroll_ratio = ?,
                        zoom_scale = ?,
                        preferred_speed_px_per_sec = ?,
                        last_accessed_at = ?,
                        updated_at = ?
                    WHERE id = ?
                    """.trimIndent(),
                    arrayOf(
                        safePage,
                        safeRatio.toDouble(),
                        safeZoom.toDouble(),
                        safeSpeed.toDouble(),
                        safeNow,
                        safeNow,
                        documentId
                    )
                )
            }
        }
        notifyChanged()
    }

    override suspend fun setDocumentAvailability(documentUri: String, isAvailable: Boolean) {
        if (documentUri.isBlank()) return
        awaitReady()
        val now = System.currentTimeMillis()
        database.withConnection { conn ->
            conn.execSQL(
                """
                UPDATE ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}
                SET is_available = ?, updated_at = ?
                WHERE document_uri = ?
                """.trimIndent(),
                arrayOf(if (isAvailable) 1 else 0, now, documentUri)
            )
        }
        notifyChanged()
    }

    override suspend fun updateAvailabilityBatch(updates: Map<String, Boolean>) {
        if (updates.isEmpty()) return
        awaitReady()
        val now = System.currentTimeMillis()
        database.withConnection { conn ->
            conn.inTransaction { tx ->
                for ((uri, available) in updates) {
                    if (uri.isBlank()) continue
                    tx.execSQL(
                        """
                        UPDATE ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}
                        SET is_available = ?, updated_at = ?
                        WHERE document_uri = ?
                        """.trimIndent(),
                        arrayOf(if (available) 1 else 0, now, uri)
                    )
                }
            }
        }
        notifyChanged()
    }

    override suspend fun relinkDocument(
        targetDocumentId: String,
        newUri: String,
        newFileName: String,
        newPageCount: Int,
        consolidateConflictingDocumentId: String?,
        nowMillis: Long
    ): Result<PdfDocumentRecord> {
        val safeUri = newUri.trim()
        if (targetDocumentId.isBlank() || safeUri.isBlank()) {
            return Result.failure(IllegalArgumentException("targetDocumentId and newUri must not be blank"))
        }
        awaitReady()
        val safeFileName = PdfDocumentRecord.sanitizeFileName(newFileName)
        val safePageCount = PdfDocumentRecord.sanitizePageCount(newPageCount)
        val safeNow = PdfDocumentRecord.sanitizeTimestamp(nowMillis)

        val result = runCatching {
            database.withConnection { conn ->
                conn.inTransaction { tx ->
                    val target = querySingleDocument(
                        conn = tx,
                        sql = "SELECT $DOCUMENT_COLUMNS FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} WHERE id = ? LIMIT 1",
                        args = arrayOf(targetDocumentId)
                    ) ?: throw IllegalArgumentException("Document not found: $targetDocumentId")

                    val conflicting = querySingleDocument(
                        conn = tx,
                        sql = "SELECT $DOCUMENT_COLUMNS FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} WHERE document_uri = ? LIMIT 1",
                        args = arrayOf(safeUri)
                    )

                    if (conflicting != null && conflicting.id != target.id) {
                        if (consolidateConflictingDocumentId != conflicting.id) {
                            throw IllegalStateException("URI already registered to another document: ${conflicting.id}")
                        }
                        tx.execSQL(
                            "DELETE FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} WHERE id = ?",
                            arrayOf(conflicting.id)
                        )
                    }

                    val adjustedPage = DocumentRelinkPolicy.adjustPageIndexForNewPageCount(
                        savedPageIndex = target.currentPageIndex,
                        newPageCount = safePageCount
                    )
                    val adjustedRatio = DocumentRelinkPolicy.adjustProgressRatio(
                        adjustedPageIndex = adjustedPage,
                        savedPageIndex = target.currentPageIndex,
                        newPageCount = safePageCount,
                        savedRatio = target.normalizedScrollRatio
                    )

                    val updated = target.copy(
                        documentUri = safeUri,
                        fileName = safeFileName,
                        pageCount = safePageCount,
                        currentPageIndex = adjustedPage,
                        normalizedScrollRatio = adjustedRatio,
                        lastAccessedAt = safeNow,
                        isInRecents = true,
                        isAvailable = true,
                        updatedAt = safeNow
                    )

                    tx.execSQL(
                        """
                        UPDATE ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}
                        SET document_uri = ?,
                            file_name = ?,
                            page_count = ?,
                            current_page_index = ?,
                            normalized_scroll_ratio = ?,
                            last_accessed_at = ?,
                            is_in_recents = 1,
                            is_available = 1,
                            updated_at = ?
                        WHERE id = ?
                        """.trimIndent(),
                        arrayOf(
                            updated.documentUri,
                            updated.fileName,
                            updated.pageCount,
                            updated.currentPageIndex,
                            updated.normalizedScrollRatio.toDouble(),
                            updated.lastAccessedAt,
                            updated.updatedAt,
                            updated.id
                        )
                    )
                    updated
                }
            }
        }
        if (result.isSuccess) {
            notifyChanged()
        }
        return result
    }

    override suspend fun removeFromRecents(documentIdOrUri: String, deleteIfUnavailable: Boolean) {
        if (documentIdOrUri.isBlank()) return
        awaitReady()
        val now = System.currentTimeMillis()
        database.withConnection { conn ->
            conn.inTransaction { tx ->
                val record = querySingleDocument(
                    conn = tx,
                    sql = """
                        SELECT $DOCUMENT_COLUMNS
                        FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}
                        WHERE id = ? OR document_uri = ?
                        LIMIT 1
                    """.trimIndent(),
                    args = arrayOf(documentIdOrUri, documentIdOrUri)
                ) ?: return@inTransaction

                if (deleteIfUnavailable && !record.isAvailable) {
                    tx.execSQL(
                        "DELETE FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} WHERE id = ?",
                        arrayOf(record.id)
                    )
                } else {
                    tx.execSQL(
                        """
                        UPDATE ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}
                        SET is_in_recents = 0, updated_at = ?
                        WHERE id = ?
                        """.trimIndent(),
                        arrayOf(now, record.id)
                    )
                }
            }
        }
        notifyChanged()
    }

    override suspend fun clearAllRecents() {
        awaitReady()
        val now = System.currentTimeMillis()
        database.withConnection { conn ->
            conn.inTransaction { tx ->
                tx.execSQL(
                    "DELETE FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} WHERE is_in_recents = 1 AND is_available = 0"
                )
                tx.execSQL(
                    "UPDATE ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} SET is_in_recents = 0, updated_at = ? WHERE is_in_recents = 1",
                    arrayOf(now)
                )
            }
        }
        notifyChanged()
    }

    // Intentionally bypasses awaitReady() so LegacyDataStoreMigrator can check migration state without deadlock.
    override suspend fun isDataStoreMigrationCompleted(): Boolean =
        database.withConnection(readOnly = true) { conn ->
            readAppSetting(conn, PdfReaderDatabaseSchema.SETTING_DATASTORE_MIGRATION_COMPLETED) ==
                PdfReaderDatabaseSchema.MIGRATION_COMPLETED_VALUE
        }

    override suspend fun getAppSetting(key: String): String? {
        awaitReady()
        return database.withConnection(readOnly = true) { conn ->
            readAppSetting(conn, key)
        }
    }

    override suspend fun setAppSetting(key: String, value: String) {
        awaitReady()
        database.withConnection { conn ->
            upsertAppSetting(conn, key, value)
        }
        notifyChanged()
    }

    override suspend fun getFavoriteFoldersSnapshot(): List<FavoriteFolder> {
        awaitReady()
        return database.withConnection(readOnly = true) { conn ->
            queryFavoriteFolders(conn)
        }
    }

    override fun observeFavoriteFolders(): Flow<List<FavoriteFolder>> =
        changeSignal.map { getFavoriteFoldersSnapshot() }

    override suspend fun upsertFavoriteFolder(folder: FavoriteFolder) {
        awaitReady()
        database.withConnection { conn ->
            upsertFavoriteFolderRow(conn, folder)
        }
        notifyChanged()
    }

    override suspend fun updateFavoriteFolderFileCount(treeUri: String, count: Int) {
        awaitReady()
        database.withConnection { conn ->
            conn.execSQL(
                """
                UPDATE ${PdfReaderDatabaseSchema.TABLE_FAVORITE_FOLDERS}
                SET cached_file_count = ?
                WHERE tree_uri = ?
                """.trimIndent(),
                arrayOf(count.coerceAtLeast(0), treeUri)
            )
        }
        notifyChanged()
    }

    override suspend fun removeFavoriteFolder(treeUri: String) {
        awaitReady()
        database.withConnection { conn ->
            conn.execSQL(
                "DELETE FROM ${PdfReaderDatabaseSchema.TABLE_FAVORITE_FOLDERS} WHERE tree_uri = ?",
                arrayOf(treeUri)
            )
        }
        notifyChanged()
    }

    override suspend fun replaceFavoriteFolders(folders: List<FavoriteFolder>) {
        awaitReady()
        val deduplicated = folders
            .distinctBy { it.treeUri }
            .sortedByDescending { it.pinnedTimestamp }
        database.withConnection { conn ->
            conn.inTransaction { tx ->
                tx.execSQL("DELETE FROM ${PdfReaderDatabaseSchema.TABLE_FAVORITE_FOLDERS}")
                deduplicated.forEach { folder ->
                    upsertFavoriteFolderRow(tx, folder)
                }
            }
        }
        notifyChanged()
    }

    // Intentionally bypasses awaitReady() so LegacyDataStoreMigrator can populate SQLite before releasing the gate.
    override suspend fun importLegacyDataOnce(
        globalDefaultSpeedPxPerSec: Float,
        documents: List<PdfDocumentRecord>,
        favoriteFolders: List<FavoriteFolder>,
        migratedAtMillis: Long
    ) {
        val safeGlobalSpeed = PdfDocumentRecord.sanitizeSpeed(globalDefaultSpeedPxPerSec)
        val safeMigratedAt = PdfDocumentRecord.sanitizeTimestamp(migratedAtMillis)
        database.withConnection { conn ->
            conn.inTransaction { tx ->
                if (readAppSetting(tx, PdfReaderDatabaseSchema.SETTING_DATASTORE_MIGRATION_COMPLETED) ==
                    PdfReaderDatabaseSchema.MIGRATION_COMPLETED_VALUE
                ) {
                    return@inTransaction
                }

                upsertAppSetting(
                    tx,
                    PdfReaderDatabaseSchema.SETTING_GLOBAL_DEFAULT_SPEED,
                    safeGlobalSpeed.toString()
                )

                for (doc in documents) {
                    val existing = querySingleDocument(
                        conn = tx,
                        sql = "SELECT $DOCUMENT_COLUMNS FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} WHERE document_uri = ? LIMIT 1",
                        args = arrayOf(doc.documentUri)
                    )
                    if (existing == null) {
                        insertDocumentRow(tx, doc)
                    }
                }

                for (folder in favoriteFolders.distinctBy { it.treeUri }) {
                    upsertFavoriteFolderRow(tx, folder)
                }

                upsertAppSetting(
                    tx,
                    PdfReaderDatabaseSchema.SETTING_DATASTORE_MIGRATION_COMPLETED,
                    PdfReaderDatabaseSchema.MIGRATION_COMPLETED_VALUE
                )
                upsertAppSetting(
                    tx,
                    PdfReaderDatabaseSchema.SETTING_DATASTORE_MIGRATION_AT,
                    safeMigratedAt.toString()
                )
            }
        }
        notifyChanged()
    }

    override suspend fun replaceDatabaseSnapshot(
        globalDefaultSpeedPxPerSec: Float,
        documents: List<PdfDocumentRecord>,
        favoriteFolders: List<FavoriteFolder>,
        nowMillis: Long
    ) {
        awaitReady()
        val safeGlobalSpeed = PdfDocumentRecord.sanitizeSpeed(globalDefaultSpeedPxPerSec)
        val deduplicatedDocs = deduplicateDocuments(documents)
        val deduplicatedFolders = favoriteFolders
            .distinctBy { it.treeUri }
            .sortedByDescending { it.pinnedTimestamp }

        database.withConnection { conn ->
            conn.inTransaction { tx ->
                tx.execSQL("DELETE FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}")
                tx.execSQL("DELETE FROM ${PdfReaderDatabaseSchema.TABLE_FAVORITE_FOLDERS}")

                for (doc in deduplicatedDocs) {
                    insertDocumentRow(tx, doc)
                }

                for (folder in deduplicatedFolders) {
                    upsertFavoriteFolderRow(tx, folder)
                }

                upsertAppSetting(
                    tx,
                    PdfReaderDatabaseSchema.SETTING_GLOBAL_DEFAULT_SPEED,
                    safeGlobalSpeed.toString()
                )
                // Always ensure migration flag remains "1" so legacy DataStore is never re-imported over a restored backup
                upsertAppSetting(
                    tx,
                    PdfReaderDatabaseSchema.SETTING_DATASTORE_MIGRATION_COMPLETED,
                    PdfReaderDatabaseSchema.MIGRATION_COMPLETED_VALUE
                )
            }
        }
        notifyChanged()
    }

    // --- ReadingSessionStore compatibility methods ---

    override fun getSession(documentUri: String): Flow<ReadingSession?> =
        changeSignal.map {
            getByUri(documentUri)?.toReadingSession()
        }

    override suspend fun saveSession(session: ReadingSession) {
        awaitReady()
        val safeNow = PdfDocumentRecord.sanitizeTimestamp(session.lastReadTimestamp)
        database.withConnection { conn ->
            conn.inTransaction { tx ->
                val existing = (if (session.documentId.isNotBlank()) {
                    querySingleDocument(
                        conn = tx,
                        sql = "SELECT $DOCUMENT_COLUMNS FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} WHERE id = ? LIMIT 1",
                        args = arrayOf(session.documentId)
                    )
                } else null) ?: querySingleDocument(
                    conn = tx,
                    sql = "SELECT $DOCUMENT_COLUMNS FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} WHERE document_uri = ? LIMIT 1",
                    args = arrayOf(session.documentUri)
                )

                if (existing != null) {
                    val safePageCount = maxOf(existing.pageCount, session.currentPageIndex + 1)
                    val safePage = PdfDocumentRecord.sanitizePageIndex(session.currentPageIndex, safePageCount)
                    val safeRatio = PdfDocumentRecord.sanitizeScrollRatio(session.normalizedScrollRatio)
                    val safeZoom = PdfDocumentRecord.sanitizeZoom(session.zoomScale)
                    val safeSpeed = PdfDocumentRecord.sanitizeSpeed(session.preferredSpeedPxPerSec)
                    val resolvedCustomSpeed = PdfDocumentRecord.sanitizeOptionalSpeed(session.customDefaultSpeedPxPerSec)
                        ?: existing.customDefaultSpeedPxPerSec
                    val resolvedFileName = session.fileName.trim().takeIf { it.isNotEmpty() } ?: existing.fileName

                    tx.execSQL(
                        """
                        UPDATE ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}
                        SET file_name = ?,
                            page_count = ?,
                            current_page_index = ?,
                            normalized_scroll_ratio = ?,
                            zoom_scale = ?,
                            preferred_speed_px_per_sec = ?,
                            custom_default_speed_px_per_sec = ?,
                            last_accessed_at = ?,
                            updated_at = ?
                        WHERE id = ?
                        """.trimIndent(),
                        arrayOf(
                            resolvedFileName,
                            safePageCount,
                            safePage,
                            safeRatio.toDouble(),
                            safeZoom.toDouble(),
                            safeSpeed.toDouble(),
                            resolvedCustomSpeed?.toDouble(),
                            safeNow,
                            safeNow,
                            existing.id
                        )
                    )
                } else {
                    val created = PdfDocumentRecord.createSanitized(
                        id = session.documentId.ifBlank { uuidGenerator() },
                        documentUri = session.documentUri,
                        fileName = session.fileName,
                        pageCount = (session.currentPageIndex + 1).coerceAtLeast(1),
                        currentPageIndex = session.currentPageIndex,
                        normalizedScrollRatio = session.normalizedScrollRatio,
                        zoomScale = session.zoomScale,
                        preferredSpeedPxPerSec = session.preferredSpeedPxPerSec,
                        customDefaultSpeedPxPerSec = session.customDefaultSpeedPxPerSec,
                        lastAccessedAt = safeNow,
                        isInRecents = false,
                        isAvailable = true,
                        createdAt = safeNow,
                        updatedAt = safeNow
                    )
                    insertDocumentRow(tx, created)
                }
            }
        }
        notifyChanged()
    }

    override fun getGlobalDefaultSpeed(): Flow<Float> =
        changeSignal.map {
            awaitReady()
            database.withConnection(readOnly = true) { conn ->
                readGlobalDefaultSpeed(conn)
            }
        }

    override suspend fun setGlobalDefaultSpeed(speedPxPerSec: Float) {
        awaitReady()
        val clamped = PdfDocumentRecord.sanitizeSpeed(speedPxPerSec)
        database.withConnection { conn ->
            upsertAppSetting(
                conn,
                PdfReaderDatabaseSchema.SETTING_GLOBAL_DEFAULT_SPEED,
                clamped.toString()
            )
        }
        notifyChanged()
    }

    override fun getCustomDocumentSpeed(documentUri: String): Flow<Float?> =
        changeSignal.map {
            getByUri(documentUri)?.customDefaultSpeedPxPerSec
        }

    override suspend fun saveCustomDocumentSpeed(documentUri: String, speedPxPerSec: Float) {
        if (documentUri.isBlank()) return
        awaitReady()
        val clamped = PdfDocumentRecord.sanitizeSpeed(speedPxPerSec)
        val now = System.currentTimeMillis()
        database.withConnection { conn ->
            conn.inTransaction { tx ->
                val existing = querySingleDocument(
                    conn = tx,
                    sql = "SELECT $DOCUMENT_COLUMNS FROM ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} WHERE document_uri = ? LIMIT 1",
                    args = arrayOf(documentUri)
                )
                if (existing != null) {
                    tx.execSQL(
                        """
                        UPDATE ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}
                        SET custom_default_speed_px_per_sec = ?,
                            preferred_speed_px_per_sec = ?,
                            updated_at = ?
                        WHERE id = ?
                        """.trimIndent(),
                        arrayOf(clamped.toDouble(), clamped.toDouble(), now, existing.id)
                    )
                } else {
                    val created = PdfDocumentRecord.createSanitized(
                        id = uuidGenerator(),
                        documentUri = documentUri,
                        fileName = PdfDocumentRecord.DEFAULT_FILE_NAME,
                        preferredSpeedPxPerSec = clamped,
                        customDefaultSpeedPxPerSec = clamped,
                        lastAccessedAt = now,
                        isInRecents = false,
                        isAvailable = true,
                        createdAt = now,
                        updatedAt = now
                    )
                    insertDocumentRow(tx, created)
                }
            }
        }
        notifyChanged()
    }

    override suspend fun clearCustomDocumentSpeed(documentUri: String) {
        if (documentUri.isBlank()) return
        awaitReady()
        val now = System.currentTimeMillis()
        database.withConnection { conn ->
            conn.inTransaction { tx ->
                val globalDefaultSpeed = readGlobalDefaultSpeed(tx)
                tx.execSQL(
                    """
                    UPDATE ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}
                    SET custom_default_speed_px_per_sec = NULL,
                        preferred_speed_px_per_sec = ?,
                        updated_at = ?
                    WHERE document_uri = ?
                    """.trimIndent(),
                    arrayOf(globalDefaultSpeed.toDouble(), now, documentUri)
                )
            }
        }
        notifyChanged()
    }

    override suspend fun saveDocumentFileName(documentUri: String, fileName: String) {
        val trimmed = fileName.trim()
        if (documentUri.isBlank() || trimmed.isEmpty()) return
        awaitReady()
        val now = System.currentTimeMillis()
        database.withConnection { conn ->
            conn.execSQL(
                """
                UPDATE ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS}
                SET file_name = ?, updated_at = ?
                WHERE document_uri = ?
                """.trimIndent(),
                arrayOf(trimmed, now, documentUri)
            )
        }
        notifyChanged()
    }

    override suspend fun getAllSessionsSnapshot(): List<ReadingSession> =
        getAllDocumentsSnapshot().map { it.toReadingSession() }

    override suspend fun findCustomSpeedByFileName(fileName: String): Float? = null

    // --- Internal SQL helpers ---

    private fun insertDocumentRow(conn: SqliteConnection, record: PdfDocumentRecord) {
        conn.execSQL(
            """
            INSERT INTO ${PdfReaderDatabaseSchema.TABLE_PDF_DOCUMENTS} (
                $DOCUMENT_COLUMNS
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                record.id,
                record.documentUri,
                record.fileName,
                record.pageCount,
                record.currentPageIndex,
                record.normalizedScrollRatio.toDouble(),
                record.zoomScale.toDouble(),
                record.preferredSpeedPxPerSec.toDouble(),
                record.customDefaultSpeedPxPerSec?.toDouble(),
                record.lastAccessedAt,
                if (record.isInRecents) 1 else 0,
                if (record.isAvailable) 1 else 0,
                record.createdAt,
                record.updatedAt
            )
        )
    }

    private fun upsertFavoriteFolderRow(conn: SqliteConnection, folder: FavoriteFolder) {
        conn.execSQL(
            """
            INSERT INTO ${PdfReaderDatabaseSchema.TABLE_FAVORITE_FOLDERS} (
                tree_uri, display_name, path_summary, pinned_timestamp, cached_file_count
            ) VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(tree_uri) DO UPDATE SET
                display_name = excluded.display_name,
                path_summary = excluded.path_summary,
                pinned_timestamp = excluded.pinned_timestamp,
                cached_file_count = excluded.cached_file_count
            """.trimIndent(),
            arrayOf(
                folder.treeUri,
                folder.displayName,
                folder.pathSummary,
                folder.pinnedTimestamp,
                folder.cachedFileCount.coerceAtLeast(0)
            )
        )
    }

    private fun queryFavoriteFolders(conn: SqliteConnection): List<FavoriteFolder> {
        val folders = mutableListOf<FavoriteFolder>()
        conn.rawQuery(
            """
            SELECT tree_uri, display_name, path_summary, pinned_timestamp, cached_file_count
            FROM ${PdfReaderDatabaseSchema.TABLE_FAVORITE_FOLDERS}
            ORDER BY pinned_timestamp DESC
            """.trimIndent()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val treeUri = cursor.getString(0)
                val displayName = cursor.getString(1)
                if (treeUri.isNotBlank() && displayName.isNotBlank()) {
                    folders.add(
                        FavoriteFolder(
                            treeUri = treeUri,
                            displayName = displayName,
                            pathSummary = cursor.getString(2),
                            pinnedTimestamp = cursor.getLong(3),
                            cachedFileCount = cursor.getInt(4).coerceAtLeast(0)
                        )
                    )
                }
            }
        }
        return folders
    }

    private fun readAppSetting(conn: SqliteConnection, key: String): String? =
        conn.rawQuery(
            "SELECT value FROM ${PdfReaderDatabaseSchema.TABLE_APP_SETTINGS} WHERE key = ? LIMIT 1",
            arrayOf(key)
        ).use { cursor ->
            if (cursor.moveToNext()) cursor.getString(0) else null
        }

    private fun upsertAppSetting(conn: SqliteConnection, key: String, value: String) {
        conn.execSQL(
            """
            INSERT INTO ${PdfReaderDatabaseSchema.TABLE_APP_SETTINGS} (key, value)
            VALUES (?, ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value
            """.trimIndent(),
            arrayOf(key, value)
        )
    }

    private fun readGlobalDefaultSpeed(conn: SqliteConnection): Float {
        val raw = readAppSetting(conn, PdfReaderDatabaseSchema.SETTING_GLOBAL_DEFAULT_SPEED)
        val parsed = raw?.toFloatOrNull() ?: SpeedPreset.MEDIUM.speedPxPerSec
        return PdfDocumentRecord.sanitizeSpeed(parsed)
    }

    private fun querySingleDocument(
        conn: SqliteConnection,
        sql: String,
        args: Array<String?>
    ): PdfDocumentRecord? =
        conn.rawQuery(sql, args).use { cursor ->
            if (cursor.moveToNext()) cursor.toPdfDocumentRecord() else null
        }

    private fun queryDocuments(
        conn: SqliteConnection,
        sql: String,
        args: Array<String?> = emptyArray()
    ): List<PdfDocumentRecord> {
        val list = mutableListOf<PdfDocumentRecord>()
        conn.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.toPdfDocumentRecord())
            }
        }
        return list
    }

    private fun SqliteCursor.toPdfDocumentRecord(): PdfDocumentRecord =
        PdfDocumentRecord.createSanitized(
            id = getString(0),
            documentUri = getString(1),
            fileName = getString(2),
            pageCount = getInt(3),
            currentPageIndex = getInt(4),
            normalizedScrollRatio = getDouble(5).toFloat(),
            zoomScale = getDouble(6).toFloat(),
            preferredSpeedPxPerSec = getDouble(7).toFloat(),
            customDefaultSpeedPxPerSec = getFloatOrNull(8),
            lastAccessedAt = getLong(9),
            isInRecents = getInt(10) != 0,
            isAvailable = getInt(11) != 0,
            createdAt = getLong(12),
            updatedAt = getLong(13)
        )

    private fun deduplicateDocuments(documents: List<PdfDocumentRecord>): List<PdfDocumentRecord> {
        val byUri = linkedMapOf<String, PdfDocumentRecord>()
        val usedIds = mutableSetOf<String>()
        for (raw in documents) {
            val existingByUri = byUri[raw.documentUri]
            if (existingByUri != null) {
                if (existingByUri.lastAccessedAt >= raw.lastAccessedAt) {
                    continue
                }
                // Free the replaced row's id before checking collision so the same document keeps its UUID (RN-01).
                usedIds.remove(existingByUri.id)
            }
            val safeId = if (raw.id in usedIds) uuidGenerator() else raw.id
            usedIds.add(safeId)
            byUri[raw.documentUri] = raw.copy(id = safeId)
        }
        return byUri.values.toList()
    }

    companion object {
        private const val DOCUMENT_COLUMNS = """
            id,
            document_uri,
            file_name,
            page_count,
            current_page_index,
            normalized_scroll_ratio,
            zoom_scale,
            preferred_speed_px_per_sec,
            custom_default_speed_px_per_sec,
            last_accessed_at,
            is_in_recents,
            is_available,
            created_at,
            updated_at
        """
    }
}
