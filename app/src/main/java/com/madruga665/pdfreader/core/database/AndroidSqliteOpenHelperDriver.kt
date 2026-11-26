package com.madruga665.pdfreader.core.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class AndroidSqliteOpenHelperDriver(
    context: Context,
    private val databaseName: String = PdfReaderDatabaseSchema.DATABASE_NAME,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SqliteDriver {

    private val appContext = context.applicationContext
    private val mutex = Mutex()

    private val helper: SQLiteOpenHelper = object : SQLiteOpenHelper(
        appContext,
        databaseName,
        null,
        PdfReaderDatabaseSchema.SCHEMA_VERSION
    ) {
        override fun onConfigure(db: SQLiteDatabase) {
            super.onConfigure(db)
            db.disableWriteAheadLogging()
            db.setForeignKeyConstraintsEnabled(true)
        }

        override fun onCreate(db: SQLiteDatabase) {
            PdfReaderDatabaseSchema.createSchema(AndroidSqliteConnection(db))
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Schema v1 initial release; future upgrades will run versioned migrations here.
            PdfReaderDatabaseSchema.createSchema(AndroidSqliteConnection(db))
        }
    }.apply {
        setWriteAheadLoggingEnabled(false)
    }

    override suspend fun <T> withConnection(
        readOnly: Boolean,
        block: (SqliteConnection) -> T
    ): T = withContext(ioDispatcher) {
        mutex.withLock {
            val db = if (readOnly) helper.readableDatabase else helper.writableDatabase
            block(AndroidSqliteConnection(db))
        }
    }

    override suspend fun exportDatabaseFileTo(destinationFile: File): Result<Unit> =
        withContext(ioDispatcher) {
            mutex.withLock {
                runCatching {
                    val db = helper.writableDatabase
                    // Defensive checkpoint in case WAL was ever enabled externally;
                    // journal_mode = DELETE ensures all committed data lives in the single .db file.
                    runCatching {
                        db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
                    }

                    val sourceFile = appContext.getDatabasePath(databaseName)
                    check(sourceFile.exists() && sourceFile.length() > 0L) {
                        "Local SQLite database file does not exist or is empty"
                    }

                    destinationFile.parentFile?.mkdirs()
                    sourceFile.copyTo(destinationFile, overwrite = true)

                    val integrity = openExternalReadOnlyUnlocked(destinationFile) { conn ->
                        conn.rawQuery("PRAGMA integrity_check").use { cursor ->
                            if (cursor.moveToNext()) cursor.getString(0) else ""
                        }
                    }.getOrThrow()

                    check(integrity.equals("ok", ignoreCase = true)) {
                        "Exported SQLite backup failed integrity check: $integrity"
                    }
                }
            }
        }

    override suspend fun <T> withReadOnlyExternalDatabase(
        sourceFile: File,
        block: (SqliteConnection) -> T
    ): Result<T> = withContext(ioDispatcher) {
        mutex.withLock {
            openExternalReadOnlyUnlocked(sourceFile, block)
        }
    }

    private fun <T> openExternalReadOnlyUnlocked(
        sourceFile: File,
        block: (SqliteConnection) -> T
    ): Result<T> = runCatching {
        val db = SQLiteDatabase.openDatabase(
            sourceFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
        )
        db.use { opened ->
            block(AndroidSqliteConnection(opened))
        }
    }

    override fun close() {
        runCatching { helper.close() }
    }

    private class AndroidSqliteConnection(
        private val db: SQLiteDatabase
    ) : SqliteConnection {

        override fun execSQL(sql: String, bindArgs: Array<Any?>) {
            if (bindArgs.isEmpty()) {
                db.execSQL(sql)
            } else {
                db.execSQL(sql, bindArgs)
            }
        }

        override fun rawQuery(sql: String, selectionArgs: Array<String?>): SqliteCursor {
            val cursor = db.rawQuery(sql, selectionArgs)
            return AndroidSqliteCursor(cursor)
        }

        override fun <T> inTransaction(exclusive: Boolean, block: (SqliteConnection) -> T): T {
            if (exclusive) {
                db.beginTransaction()
            } else {
                db.beginTransactionNonExclusive()
            }
            try {
                val result = block(this)
                db.setTransactionSuccessful()
                return result
            } finally {
                db.endTransaction()
            }
        }

        override fun close() {
            // Connection lifecycle is managed by SQLiteOpenHelper or openExternalReadOnlyUnlocked
        }
    }

    private class AndroidSqliteCursor(
        private val cursor: Cursor
    ) : SqliteCursor {

        override fun moveToNext(): Boolean = cursor.moveToNext()

        override fun getString(columnIndex: Int): String =
            if (cursor.isNull(columnIndex)) "" else cursor.getString(columnIndex)

        override fun getStringOrNull(columnIndex: Int): String? =
            if (cursor.isNull(columnIndex)) null else cursor.getString(columnIndex)

        override fun getInt(columnIndex: Int): Int = cursor.getInt(columnIndex)

        override fun getLong(columnIndex: Int): Long = cursor.getLong(columnIndex)

        override fun getDouble(columnIndex: Int): Double = cursor.getDouble(columnIndex)

        override fun getFloatOrNull(columnIndex: Int): Float? =
            if (cursor.isNull(columnIndex)) null else cursor.getFloat(columnIndex)

        override fun close() {
            cursor.close()
        }
    }
}
