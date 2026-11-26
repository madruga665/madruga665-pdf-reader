package com.madruga665.pdfreader.core.database

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class JdbcSqliteDriver(
    private val databaseFile: File,
    initializeSchema: Boolean = true
) : SqliteDriver {

    private val mutex = Mutex()
    private val connection: Connection

    init {
        databaseFile.parentFile?.mkdirs()
        connection = DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}")
        connection.autoCommit = true
        connection.createStatement().use { stmt ->
            stmt.execute("PRAGMA journal_mode = DELETE")
            stmt.execute("PRAGMA foreign_keys = ON")
        }
        if (initializeSchema) {
            val wrapped = JdbcSqliteConnection(connection)
            PdfReaderDatabaseSchema.createSchema(wrapped)
        }
    }

    override suspend fun <T> withConnection(
        readOnly: Boolean,
        block: (SqliteConnection) -> T
    ): T = mutex.withLock {
        val wrapped = JdbcSqliteConnection(connection)
        block(wrapped)
    }

    override suspend fun exportDatabaseFileTo(destinationFile: File): Result<Unit> = mutex.withLock {
        runCatching {
            connection.createStatement().use { stmt ->
                runCatching { stmt.execute("PRAGMA wal_checkpoint(TRUNCATE)") }
            }
            destinationFile.parentFile?.mkdirs()
            databaseFile.copyTo(destinationFile, overwrite = true)

            // Verify exported file passes PRAGMA integrity_check == "ok"
            val integrity = withReadOnlyExternalDatabaseUnlocked(destinationFile) { extConn ->
                extConn.rawQuery("PRAGMA integrity_check").use { cursor ->
                    if (cursor.moveToNext()) cursor.getString(0) else ""
                }
            }.getOrThrow()
            check(integrity.equals("ok", ignoreCase = true)) {
                "Exported SQLite file failed integrity_check: $integrity"
            }
        }
    }

    override suspend fun <T> withReadOnlyExternalDatabase(
        sourceFile: File,
        block: (SqliteConnection) -> T
    ): Result<T> = mutex.withLock {
        withReadOnlyExternalDatabaseUnlocked(sourceFile, block)
    }

    private fun <T> withReadOnlyExternalDatabaseUnlocked(
        sourceFile: File,
        block: (SqliteConnection) -> T
    ): Result<T> = runCatching {
        val uri = "jdbc:sqlite:file:${sourceFile.absolutePath}?mode=ro"
        DriverManager.getConnection(uri).use { extConn ->
            extConn.autoCommit = true
            block(JdbcSqliteConnection(extConn))
        }
    }

    override fun close() {
        runCatching { connection.close() }
    }

    private class JdbcSqliteConnection(
        private val jdbc: Connection
    ) : SqliteConnection {

        override fun execSQL(sql: String, bindArgs: Array<Any?>) {
            if (bindArgs.isEmpty()) {
                jdbc.createStatement().use { stmt ->
                    stmt.execute(sql)
                }
            } else {
                jdbc.prepareStatement(sql).use { ps ->
                    bindValues(ps, bindArgs)
                    ps.executeUpdate()
                }
            }
        }

        override fun rawQuery(sql: String, selectionArgs: Array<String?>): SqliteCursor {
            val ps = jdbc.prepareStatement(sql)
            selectionArgs.forEachIndexed { idx, value ->
                if (value == null) {
                    ps.setNull(idx + 1, Types.VARCHAR)
                } else {
                    ps.setString(idx + 1, value)
                }
            }
            val rs = ps.executeQuery()
            return JdbcSqliteCursor(ps, rs)
        }

        override fun <T> inTransaction(exclusive: Boolean, block: (SqliteConnection) -> T): T {
            val beginSql = if (exclusive) "BEGIN IMMEDIATE" else "BEGIN"
            jdbc.createStatement().use { it.execute(beginSql) }
            try {
                val result = block(this)
                jdbc.createStatement().use { it.execute("COMMIT") }
                return result
            } catch (t: Throwable) {
                runCatching {
                    jdbc.createStatement().use { it.execute("ROLLBACK") }
                }
                throw t
            }
        }

        override fun close() {
            // Managed by JdbcSqliteDriver
        }

        private fun bindValues(ps: PreparedStatement, bindArgs: Array<Any?>) {
            bindArgs.forEachIndexed { index, value ->
                val paramIndex = index + 1
                when (value) {
                    null -> ps.setNull(paramIndex, Types.NULL)
                    is String -> ps.setString(paramIndex, value)
                    is Int -> ps.setInt(paramIndex, value)
                    is Long -> ps.setLong(paramIndex, value)
                    is Float -> ps.setDouble(paramIndex, value.toDouble())
                    is Double -> ps.setDouble(paramIndex, value)
                    is Boolean -> ps.setInt(paramIndex, if (value) 1 else 0)
                    is ByteArray -> ps.setBytes(paramIndex, value)
                    else -> ps.setObject(paramIndex, value)
                }
            }
        }
    }

    private class JdbcSqliteCursor(
        private val ps: PreparedStatement,
        private val rs: ResultSet
    ) : SqliteCursor {

        override fun moveToNext(): Boolean = rs.next()

        override fun getString(columnIndex: Int): String =
            rs.getString(columnIndex + 1) ?: ""

        override fun getStringOrNull(columnIndex: Int): String? =
            rs.getString(columnIndex + 1)

        override fun getInt(columnIndex: Int): Int = rs.getInt(columnIndex + 1)

        override fun getLong(columnIndex: Int): Long = rs.getLong(columnIndex + 1)

        override fun getDouble(columnIndex: Int): Double = rs.getDouble(columnIndex + 1)

        override fun getFloatOrNull(columnIndex: Int): Float? {
            val value = rs.getObject(columnIndex + 1) ?: return null
            return (value as Number).toFloat()
        }

        override fun close() {
            runCatching { rs.close() }
            runCatching { ps.close() }
        }
    }
}
