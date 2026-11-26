package com.madruga665.pdfreader.core.database

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.Closeable
import java.io.File

/**
 * Facade managing the local SQLite database lifecycle, safe staging exports,
 * and read-only inspection of external `.db` backup files.
 */
class SqliteAppDatabase(
    val driver: SqliteDriver
) : Closeable {

    suspend fun <T> withConnection(readOnly: Boolean = false, block: (SqliteConnection) -> T): T =
        driver.withConnection(readOnly = readOnly, block = block)

    suspend fun exportToStagingFile(destinationFile: File): Result<Unit> =
        driver.exportDatabaseFileTo(destinationFile)

    suspend fun <T> inspectExternalDatabase(
        sourceFile: File,
        block: (SqliteConnection) -> T
    ): Result<T> = driver.withReadOnlyExternalDatabase(sourceFile, block)

    override fun close() {
        driver.close()
    }

    companion object {
        fun createAndroid(
            context: Context,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): SqliteAppDatabase = SqliteAppDatabase(
            driver = AndroidSqliteOpenHelperDriver(
                context = context,
                databaseName = PdfReaderDatabaseSchema.DATABASE_NAME,
                ioDispatcher = ioDispatcher
            )
        )
    }
}
