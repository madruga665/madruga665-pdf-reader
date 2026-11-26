package com.madruga665.pdfreader.core.backup

import com.madruga665.pdfreader.core.database.JdbcSqliteDriver
import com.madruga665.pdfreader.core.database.PdfReaderDatabaseSchema
import com.madruga665.pdfreader.core.database.SqliteAppDatabase
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.storage.SqlitePdfDocumentStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.sql.DriverManager

class SqliteBackupValidatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createValidator(dbFileName: String = "host.db"): Pair<SqliteAppDatabase, SqliteBackupValidator> {
        val hostFile = tempFolder.newFile(dbFileName)
        val db = SqliteAppDatabase(JdbcSqliteDriver(hostFile))
        return db to SqliteBackupValidator(db)
    }

    @Test
    fun `rejects empty file with EMPTY_FILE`() = runTest {
        val (db, validator) = createValidator("host_empty.db")
        db.use {
            val emptyFile = tempFolder.newFile("empty.db")
            val result = validator.validateAndRead(emptyFile)
            assertTrue(result is BackupParseResult.Failure)
            val failure = result as BackupParseResult.Failure
            assertEquals(BackupErrorReason.EMPTY_FILE, failure.reason)
            assertFalse(failure.message.contains("json", ignoreCase = true))
        }
    }

    @Test
    fun `rejects non-sqlite json or text file with INVALID_SQLITE_FILE`() = runTest {
        val (db, validator) = createValidator("host_text.db")
        db.use {
            val jsonFile = tempFolder.newFile("legacy_backup.json").apply {
                writeText("""{"schemaVersion":1,"appVersion":"1.0.0"}""")
            }
            val resultJson = validator.validateAndRead(jsonFile)
            assertTrue(resultJson is BackupParseResult.Failure)
            val failureJson = resultJson as BackupParseResult.Failure
            assertEquals(BackupErrorReason.INVALID_SQLITE_FILE, failureJson.reason)
            assertFalse(failureJson.message.contains("json", ignoreCase = true))

            val txtFile = tempFolder.newFile("notes.txt").apply {
                writeText("just plain text")
            }
            val resultTxt = validator.validateAndRead(txtFile)
            assertTrue(resultTxt is BackupParseResult.Failure)
            assertEquals(BackupErrorReason.INVALID_SQLITE_FILE, (resultTxt as BackupParseResult.Failure).reason)
        }
    }

    @Test
    fun `rejects corrupted sqlite file with CORRUPTED_DATABASE`() = runTest {
        val (db, validator) = createValidator("host_corrupt.db")
        db.use {
            val corruptFile = tempFolder.newFile("corrupt.db")
            val header = PdfReaderDatabaseSchema.SQLITE_MAGIC_HEADER
            val garbage = ByteArray(4096) { 0x5A }
            corruptFile.writeBytes(header + garbage)

            val result = validator.validateAndRead(corruptFile)
            assertTrue(result is BackupParseResult.Failure)
            assertEquals(BackupErrorReason.CORRUPTED_DATABASE, (result as BackupParseResult.Failure).reason)
        }
    }

    @Test
    fun `rejects sqlite database missing required tables with INVALID_STRUCTURE`() = runTest {
        val (db, validator) = createValidator("host_schema.db")
        db.use {
            val wrongSchemaFile = tempFolder.newFile("other_app.db")
            DriverManager.getConnection("jdbc:sqlite:${wrongSchemaFile.absolutePath}").use { conn ->
                conn.createStatement().use { st ->
                    st.execute("PRAGMA user_version = 1")
                    st.execute("CREATE TABLE notes (id INTEGER PRIMARY KEY, body TEXT)")
                }
            }

            val result = validator.validateAndRead(wrongSchemaFile)
            assertTrue(result is BackupParseResult.Failure)
            assertEquals(BackupErrorReason.INVALID_STRUCTURE, (result as BackupParseResult.Failure).reason)
        }
    }

    @Test
    fun `rejects unsupported schema version with UNSUPPORTED_SCHEMA_VERSION`() = runTest {
        val (db, validator) = createValidator("host_ver.db")
        db.use {
            val futureFile = tempFolder.newFile("future_v99.db")
            JdbcSqliteDriver(futureFile).use { futureDriver ->
                futureDriver.withConnection { conn ->
                    conn.execSQL("PRAGMA user_version = 99")
                }
            }

            val result = validator.validateAndRead(futureFile)
            assertTrue(result is BackupParseResult.Failure)
            assertEquals(BackupErrorReason.UNSUPPORTED_SCHEMA_VERSION, (result as BackupParseResult.Failure).reason)
        }
    }

    @Test
    fun `reads valid sqlite backup file and returns exact summary counts`() = runTest {
        val (db, validator) = createValidator("host_valid.db")
        db.use {
            val backupFile = tempFolder.newFile("valid_backup.db")
            SqliteAppDatabase(JdbcSqliteDriver(backupFile)).use { backupDb ->
                val store = SqlitePdfDocumentStore(backupDb)
                store.setGlobalDefaultSpeed(135.0f)
                store.setAppSetting(PdfReaderDatabaseSchema.SETTING_APP_VERSION, "1.0.0")
                store.setAppSetting(PdfReaderDatabaseSchema.SETTING_LAST_BACKUP_EXPORTED_AT, "1764100000000")

                val doc1 = store.registerOrOpenDocument("content://pdf/1.pdf", "1.pdf", 10, 1000L)
                store.saveCustomDocumentSpeed(doc1.documentUri, 160.0f)
                val doc2 = store.registerOrOpenDocument("content://pdf/2.pdf", "2.pdf", 5, 2000L)
                store.removeFromRecents(doc2.documentUri, deleteIfUnavailable = false) // isInRecents = false
                store.upsertFavoriteFolder(
                    FavoriteFolder("content://tree/books", "Books", "primary:Books", 3000L, 4)
                )
            }

            val result = validator.validateAndRead(backupFile)
            assertTrue(result is BackupParseResult.Success)
            val success = result as BackupParseResult.Success
            assertEquals(135.0f, success.summary.globalDefaultSpeedPxPerSec, 0.001f)
            assertEquals(2, success.summary.sessionsCount)
            assertEquals(1, success.summary.customSpeedsCount)
            assertEquals(1, success.summary.recentsCount)
            assertEquals(1, success.summary.favoriteFoldersCount)
            assertEquals(1764100000000L, success.summary.exportedAt)
            assertEquals("1.0.0", success.summary.appVersion)
        }
    }
}
