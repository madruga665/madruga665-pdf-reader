package com.madruga665.pdfreader.core.database

import java.io.Closeable
import java.io.File

interface SqliteCursor : Closeable {
    fun moveToNext(): Boolean
    fun getString(columnIndex: Int): String
    fun getStringOrNull(columnIndex: Int): String?
    fun getInt(columnIndex: Int): Int
    fun getLong(columnIndex: Int): Long
    fun getDouble(columnIndex: Int): Double
    fun getFloatOrNull(columnIndex: Int): Float?
}

interface SqliteConnection : Closeable {
    fun execSQL(sql: String, bindArgs: Array<Any?> = emptyArray())
    fun rawQuery(sql: String, selectionArgs: Array<String?> = emptyArray()): SqliteCursor
    fun <T> inTransaction(exclusive: Boolean = true, block: (SqliteConnection) -> T): T
}

interface SqliteDriver : Closeable {
    suspend fun <T> withConnection(readOnly: Boolean = false, block: (SqliteConnection) -> T): T
    suspend fun exportDatabaseFileTo(destinationFile: File): Result<Unit>
    suspend fun <T> withReadOnlyExternalDatabase(
        sourceFile: File,
        block: (SqliteConnection) -> T
    ): Result<T>
}
