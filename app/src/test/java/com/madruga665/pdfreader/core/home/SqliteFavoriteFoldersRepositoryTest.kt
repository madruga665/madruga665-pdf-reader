package com.madruga665.pdfreader.core.home

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.madruga665.pdfreader.core.database.JdbcSqliteDriver
import com.madruga665.pdfreader.core.database.SqliteAppDatabase
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.storage.SqlitePdfDocumentStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SqliteFavoriteFoldersRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: SqliteAppDatabase
    private lateinit var store: SqlitePdfDocumentStore
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var repository: SqliteFavoriteFoldersRepository

    @Before
    fun setUp() {
        mockkStatic(DocumentFile::class)
        mockkStatic(Uri::class)

        val dbFile = tempFolder.newFile("folders_test.db")
        database = SqliteAppDatabase(JdbcSqliteDriver(dbFile))
        store = SqlitePdfDocumentStore(database)
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { Uri.parse(any()) } returns mockk(relaxed = true)

        repository = SqliteFavoriteFoldersRepository(
            context = context,
            documentStore = store,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(DocumentFile::class)
        unmockkStatic(Uri::class)
        database.close()
    }

    @Test
    fun `addFavoriteFolder and removeFavoriteFolder persist in SQLite`() = runTest(testDispatcher) {
        val treeUri = mockk<Uri>(relaxed = true)
        every { treeUri.toString() } returns "content://tree/books"
        every { treeUri.lastPathSegment } returns "primary:Books"
        every { DocumentFile.fromTreeUri(context, treeUri) } returns null

        val result = repository.addFavoriteFolder(treeUri, displayName = "Books")
        assertTrue(result.isSuccess)

        val folders = repository.getFavoriteFolders().first()
        assertEquals(1, folders.size)
        assertEquals("Books", folders[0].displayName)
        assertEquals("content://tree/books", folders[0].treeUri)

        repository.removeFavoriteFolder("content://tree/books")
        assertTrue(repository.getFavoriteFolders().first().isEmpty())
    }

    @Test
    fun `scanFolderPdfs returns sorted pdf items and updates cachedFileCount`() = runTest(testDispatcher) {
        val treeUri = mockk<Uri>(relaxed = true)
        every { treeUri.toString() } returns "content://tree/study"
        every { treeUri.lastPathSegment } returns "primary:Study"

        val pdfA = mockk<DocumentFile>(relaxed = true)
        val pdfB = mockk<DocumentFile>(relaxed = true)
        val txtFile = mockk<DocumentFile>(relaxed = true)
        val uriA = mockk<Uri>(relaxed = true)
        val uriB = mockk<Uri>(relaxed = true)
        every { uriA.toString() } returns "content://doc/a.pdf"
        every { uriB.toString() } returns "content://doc/b.pdf"
        every { pdfA.name } returns "B_notes.pdf"
        every { pdfA.type } returns "application/pdf"
        every { pdfA.uri } returns uriA
        every { pdfA.length() } returns 1024L
        every { pdfA.lastModified() } returns 100L
        every { pdfB.name } returns "a_book.pdf"
        every { pdfB.type } returns "application/pdf"
        every { pdfB.uri } returns uriB
        every { pdfB.length() } returns 2048L
        every { pdfB.lastModified() } returns 200L
        every { txtFile.name } returns "readme.txt"
        every { txtFile.type } returns "text/plain"

        val docDir = mockk<DocumentFile>(relaxed = true)
        every { docDir.canRead() } returns true
        every { docDir.name } returns "Study"
        every { docDir.listFiles() } returns arrayOf(pdfA, txtFile, pdfB)
        every { DocumentFile.fromTreeUri(context, treeUri) } returns docDir

        repository.addFavoriteFolder(treeUri)
        val scanResult = repository.scanFolderPdfs(treeUri)

        assertTrue(scanResult.isSuccess)
        val items = scanResult.getOrThrow()
        assertEquals(2, items.size)
        assertEquals("a_book.pdf", items[0].fileName)
        assertEquals("B_notes.pdf", items[1].fileName)

        val updatedFolder = repository.getFavoriteFolders().first().first()
        assertEquals(2, updatedFolder.cachedFileCount)
    }

    @Test
    fun `replaceAllFavoriteFolders deduplicates and sorts by pinnedTimestamp desc`() = runTest(testDispatcher) {
        repository.replaceAllFavoriteFolders(
            listOf(
                FavoriteFolder("content://tree/1", "Old", "", 1000L, 1),
                FavoriteFolder("content://tree/2", "New", "", 3000L, 5),
                FavoriteFolder("content://tree/1", "OldDup", "", 500L, 0)
            )
        )

        val folders = repository.getFavoriteFolders().first()
        assertEquals(2, folders.size)
        assertEquals("content://tree/2", folders[0].treeUri)
        assertEquals("content://tree/1", folders[1].treeUri)
    }
}
