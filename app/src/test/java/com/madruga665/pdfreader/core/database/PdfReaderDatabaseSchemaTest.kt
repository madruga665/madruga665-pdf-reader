package com.madruga665.pdfreader.core.database

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PdfReaderDatabaseSchemaTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `creates all required tables indices and sets user_version to 1`() = runTest {
        val dbFile = tempFolder.newFile("schema_test.db")
        JdbcSqliteDriver(dbFile).use { driver ->
            driver.withConnection { conn ->
                val userVersion = conn.rawQuery("PRAGMA user_version").use { cursor ->
                    assertTrue(cursor.moveToNext())
                    cursor.getInt(0)
                }
                assertEquals(PdfReaderDatabaseSchema.SCHEMA_VERSION, userVersion)

                val tables = mutableSetOf<String>()
                conn.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
                    while (cursor.moveToNext()) {
                        tables.add(cursor.getString(0))
                    }
                }
                assertTrue(tables.containsAll(PdfReaderDatabaseSchema.REQUIRED_TABLES))

                val indices = mutableSetOf<String>()
                conn.rawQuery("SELECT name FROM sqlite_master WHERE type = 'index'").use { cursor ->
                    while (cursor.moveToNext()) {
                        indices.add(cursor.getString(0))
                    }
                }
                assertTrue(indices.contains("idx_pdf_documents_recents"))
                assertTrue(indices.contains("idx_pdf_documents_file_name"))
                assertTrue(indices.contains("idx_favorite_folders_pinned"))
            }
        }
    }

    @Test
    fun `enforces unique document_uri constraint`() = runTest {
        val dbFile = tempFolder.newFile("unique_uri.db")
        JdbcSqliteDriver(dbFile).use { driver ->
            driver.withConnection { conn ->
                conn.execSQL(
                    """
                    INSERT INTO pdf_documents (
                        id, document_uri, file_name, page_count, current_page_index,
                        normalized_scroll_ratio, zoom_scale, preferred_speed_px_per_sec,
                        custom_default_speed_px_per_sec, last_accessed_at,
                        is_in_recents, is_available, created_at, updated_at
                    ) VALUES (?, ?, ?, 5, 0, 0.0, 1.0, 90.0, NULL, 1000, 1, 1, 1000, 1000)
                    """.trimIndent(),
                    arrayOf("id-1", "content://doc/a.pdf", "a.pdf")
                )

                try {
                    conn.execSQL(
                        """
                        INSERT INTO pdf_documents (
                            id, document_uri, file_name, page_count, current_page_index,
                            normalized_scroll_ratio, zoom_scale, preferred_speed_px_per_sec,
                            custom_default_speed_px_per_sec, last_accessed_at,
                            is_in_recents, is_available, created_at, updated_at
                        ) VALUES (?, ?, ?, 5, 0, 0.0, 1.0, 90.0, NULL, 1001, 1, 1, 1001, 1001)
                        """.trimIndent(),
                        arrayOf("id-2", "content://doc/a.pdf", "a.pdf")
                    )
                    fail("Expected UNIQUE constraint violation for duplicate document_uri")
                } catch (expected: Exception) {
                    assertTrue(expected.message?.contains("UNIQUE", ignoreCase = true) == true)
                }
            }
        }
    }

    @Test
    fun `enforces check constraints on speed zoom ratio page_count and boolean flags`() = runTest {
        val dbFile = tempFolder.newFile("check_constraints.db")
        JdbcSqliteDriver(dbFile).use { driver ->
            driver.withConnection { conn ->
                val invalidStatements = listOf(
                    // preferred_speed out of range (> 500)
                    """INSERT INTO pdf_documents VALUES ('id1', 'uri1', 'a.pdf', 1, 0, 0.0, 1.0, 600.0, NULL, 1, 1, 1, 1, 1)""",
                    // custom_default_speed out of range (< 10)
                    """INSERT INTO pdf_documents VALUES ('id2', 'uri2', 'a.pdf', 1, 0, 0.0, 1.0, 90.0, 5.0, 1, 1, 1, 1, 1)""",
                    // zoom_scale out of range (> 4.0)
                    """INSERT INTO pdf_documents VALUES ('id3', 'uri3', 'a.pdf', 1, 0, 0.0, 5.0, 90.0, NULL, 1, 1, 1, 1, 1)""",
                    // normalized_scroll_ratio out of range (> 1.0)
                    """INSERT INTO pdf_documents VALUES ('id4', 'uri4', 'a.pdf', 1, 0, 1.5, 1.0, 90.0, NULL, 1, 1, 1, 1, 1)""",
                    // page_count < 1
                    """INSERT INTO pdf_documents VALUES ('id5', 'uri5', 'a.pdf', 0, 0, 0.0, 1.0, 90.0, NULL, 1, 1, 1, 1, 1)""",
                    // is_available not in (0, 1)
                    """INSERT INTO pdf_documents VALUES ('id6', 'uri6', 'a.pdf', 1, 0, 0.0, 1.0, 90.0, NULL, 1, 1, 2, 1, 1)"""
                )

                invalidStatements.forEach { sql ->
                    try {
                        conn.execSQL(sql)
                        fail("Expected CHECK constraint failure for SQL: $sql")
                    } catch (expected: Exception) {
                        assertTrue(expected.message?.contains("CHECK", ignoreCase = true) == true)
                    }
                }
            }
        }
    }

    @Test
    fun `rolls back transaction on failure`() = runTest {
        val dbFile = tempFolder.newFile("rollback.db")
        JdbcSqliteDriver(dbFile).use { driver ->
            driver.withConnection { conn ->
                try {
                    conn.inTransaction { tx ->
                        tx.execSQL(
                            "INSERT INTO app_settings (key, value) VALUES (?, ?)",
                            arrayOf("k1", "v1")
                        )
                        throw IllegalStateException("Boom")
                    }
                    fail("Expected exception")
                } catch (_: IllegalStateException) {
                    // Expected
                }

                val count = conn.rawQuery("SELECT COUNT(*) FROM app_settings").use { cursor ->
                    assertTrue(cursor.moveToNext())
                    cursor.getInt(0)
                }
                assertEquals(0, count)
            }
        }
    }
}
