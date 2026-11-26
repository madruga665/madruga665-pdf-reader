package com.madruga665.pdfreader.core.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidSafBackupFileGatewayTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var mockUri: Uri
    private lateinit var gateway: AndroidSafBackupFileGateway

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        mockUri = mockk(relaxed = true)
        every { Uri.parse(any()) } returns mockUri

        contentResolver = mockk(relaxed = true)
        context = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver

        gateway = AndroidSafBackupFileGateway(
            context = context,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun copyUriToTempFile_validBinaryWithinLimit_copiesBytes() = runTest(testDispatcher) {
        val expected = byteArrayOf(0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x00, 0x01, 0x02)
        every { contentResolver.openInputStream(mockUri) } returns ByteArrayInputStream(expected)

        val dest = tempFolder.newFile("staging.db")
        val result = gateway.copyUriToTempFile("content://backup/valid.db", dest)

        assertTrue(result.isSuccess)
        assertArrayEquals(expected, dest.readBytes())
    }

    @Test
    fun copyUriToTempFile_exceedingMaxBytes_returnsIoExceptionFailure() = runTest(testDispatcher) {
        val oversized = ByteArray(1024) { 0x42 }
        every { contentResolver.openInputStream(mockUri) } returns ByteArrayInputStream(oversized)

        val dest = tempFolder.newFile("too_big.db")
        val result = gateway.copyUriToTempFile("content://backup/huge.db", dest, maxBytes = 512L)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun writeFileToUri_wtModeSucceeds_writesBytesWithoutFallback() = runTest(testDispatcher) {
        val source = tempFolder.newFile("export.db").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        }
        val output = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(mockUri, "wt") } returns output

        val result = gateway.writeFileToUri("content://backup/out.db", source)

        assertTrue(result.isSuccess)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), output.toByteArray())
        verify(exactly = 1) { contentResolver.openOutputStream(mockUri, "wt") }
        verify(exactly = 0) { contentResolver.openOutputStream(mockUri) }
    }

    @Test
    fun writeFileToUri_wtModeThrows_fallsBackToDefaultMode() = runTest(testDispatcher) {
        val source = tempFolder.newFile("export_fb.db").apply {
            writeBytes(byteArrayOf(9, 8, 7))
        }
        val fallbackOutput = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(mockUri, "wt") } throws IllegalArgumentException("Mode wt not supported")
        every { contentResolver.openOutputStream(mockUri) } returns fallbackOutput

        val result = gateway.writeFileToUri("content://backup/out.db", source)

        assertTrue(result.isSuccess)
        assertArrayEquals(byteArrayOf(9, 8, 7), fallbackOutput.toByteArray())
        verify(exactly = 1) { contentResolver.openOutputStream(mockUri, "wt") }
        verify(exactly = 1) { contentResolver.openOutputStream(mockUri) }
    }

    @Test
    fun writeFileToUri_bothModesFailOrReturnNull_returnsFailure() = runTest(testDispatcher) {
        val source = tempFolder.newFile("export_fail.db").apply { writeBytes(byteArrayOf(1)) }
        every { contentResolver.openOutputStream(mockUri, "wt") } throws FileNotFoundException("No wt")
        every { contentResolver.openOutputStream(mockUri) } returns null

        val result = gateway.writeFileToUri("content://backup/out.db", source)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
