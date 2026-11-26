package com.madruga665.pdfreader.core.repository

import android.content.Context
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.FileNotFoundException

class AndroidPdfDocumentRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: AndroidPdfDocumentRepository

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        repository = AndroidPdfDocumentRepository(context)
    }

    @Test
    fun openDocument_withInvalidUri_returnsFailure() = runTest {
        val uri = mockk<Uri>()
        every { context.contentResolver.openFileDescriptor(uri, "r") } returns null

        val result = repository.openDocument(uri)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FileNotFoundException)
    }

    @Test
    fun openDocument_whenExceptionThrown_cleansUpAndReturnsFailure() = runTest {
        val uri = mockk<Uri>()
        every { context.contentResolver.openFileDescriptor(uri, "r") } throws SecurityException("Permission denied")

        val result = repository.openDocument(uri)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun renderPageBitmap_whenNoDocumentOpened_returnsFailure() = runTest {
        val result = repository.renderPageBitmap(0, 100, 100)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun closeCurrentDocument_clearsActiveState() {
        repository.closeCurrentDocument()
        // Successfully closes without throwing exceptions
    }
}
