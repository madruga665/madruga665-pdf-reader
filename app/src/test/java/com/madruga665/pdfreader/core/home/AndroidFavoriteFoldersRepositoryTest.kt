package com.madruga665.pdfreader.core.home

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidFavoriteFoldersRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var context: Context
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: AndroidFavoriteFoldersRepository

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        val defaultMockUri = mockk<Uri>(relaxed = true)
        every { Uri.parse(any()) } returns defaultMockUri

        context = mockk(relaxed = true)
        val testFile = tmpFolder.newFile("test_favorite_folders.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )

        repository = AndroidFavoriteFoldersRepository(
            context = context,
            dataStore = dataStore,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun getFavoriteFolders_emptyInitially() = runTest(testDispatcher) {
        val folders = repository.getFavoriteFolders().first()
        assertTrue(folders.isEmpty())
    }

    @Test
    fun addFavoriteFolder_persistsAndDeduplicates() = runTest(testDispatcher) {
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://tree/folder1"
        every { uri.lastPathSegment } returns "Folder 1"

        val result = repository.addFavoriteFolder(uri, "My Books")
        assertTrue(result.isSuccess)

        val folders = repository.getFavoriteFolders().first()
        assertEquals(1, folders.size)
        assertEquals("content://tree/folder1", folders[0].treeUri)
        assertEquals("My Books", folders[0].displayName)

        // Adding again with updated name replaces/deduplicates
        val result2 = repository.addFavoriteFolder(uri, "Renamed Books")
        assertTrue(result2.isSuccess)

        val updated = repository.getFavoriteFolders().first()
        assertEquals(1, updated.size)
        assertEquals("Renamed Books", updated[0].displayName)
    }

    @Test
    fun removeFavoriteFolder_removesEntry() = runTest(testDispatcher) {
        val uri1 = mockk<Uri>(relaxed = true)
        every { uri1.toString() } returns "content://tree/folder1"
        every { uri1.lastPathSegment } returns "Folder 1"

        val uri2 = mockk<Uri>(relaxed = true)
        every { uri2.toString() } returns "content://tree/folder2"
        every { uri2.lastPathSegment } returns "Folder 2"

        repository.addFavoriteFolder(uri1, "Folder 1")
        repository.addFavoriteFolder(uri2, "Folder 2")

        val folders = repository.getFavoriteFolders().first()
        assertEquals(2, folders.size)

        repository.removeFavoriteFolder("content://tree/folder1")

        val remaining = repository.getFavoriteFolders().first()
        assertEquals(1, remaining.size)
        assertEquals("content://tree/folder2", remaining[0].treeUri)
    }

    @Test
    fun scanFolderPdfs_inaccessibleUri_returnsFailure() = runTest(testDispatcher) {
        val uri = mockk<Uri>(relaxed = true)
        val result = repository.scanFolderPdfs(uri)
        assertTrue(result.isFailure)
    }

    @Test
    fun replaceAllFavoriteFolders_replacesStoredList() = runTest(testDispatcher) {
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://tree/old"
        every { uri.lastPathSegment } returns "Old"
        repository.addFavoriteFolder(uri, "Old Folder")

        val restored = listOf(
            com.madruga665.pdfreader.core.model.FavoriteFolder(
                treeUri = "content://tree/restored1",
                displayName = "Restored Books",
                pathSummary = "primary:Books",
                pinnedTimestamp = 5000L,
                cachedFileCount = 4
            )
        )
        repository.replaceAllFavoriteFolders(restored)

        val folders = repository.getFavoriteFolders().first()
        assertEquals(1, folders.size)
        assertEquals("content://tree/restored1", folders.first().treeUri)
        assertEquals("Restored Books", folders.first().displayName)
    }
}
