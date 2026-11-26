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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidSafBackupFileGatewayTest {

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
    fun readText_validContentWithinLimit_returnsSuccess() = runTest(testDispatcher) {
        val expected = """{"schemaVersion":1,"appVersion":"1.0.0"}"""
        every { contentResolver.openInputStream(mockUri) } returns
            ByteArrayInputStream(expected.toByteArray(Charsets.UTF_8))

        val result = gateway.readText("content://backup/valid.json")

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrThrow())
    }

    @Test
    fun readText_exceedingMaxBackupBytes_returnsIoExceptionFailure() = runTest(testDispatcher) {
        val oversized = ByteArray(AndroidSafBackupFileGateway.MAX_BACKUP_BYTES + 1) { 'a'.code.toByte() }
        every { contentResolver.openInputStream(mockUri) } returns ByteArrayInputStream(oversized)

        val result = gateway.readText("content://backup/huge.json")

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is IOException)
        assertEquals("Arquivo excede o tamanho máximo de backup (5 MB).", error?.message)
    }

    @Test
    fun writeText_wtModeSucceeds_writesBytesWithoutFallback() = runTest(testDispatcher) {
        val output = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(mockUri, "wt") } returns output

        val result = gateway.writeText("content://backup/out.json", "hello-backup")

        assertTrue(result.isSuccess)
        assertEquals("hello-backup", output.toString(Charsets.UTF_8.name()))
        verify(exactly = 1) { contentResolver.openOutputStream(mockUri, "wt") }
        verify(exactly = 0) { contentResolver.openOutputStream(mockUri) }
    }

    @Test
    fun writeText_wtModeThrowsException_fallsBackToDefaultMode() = runTest(testDispatcher) {
        val fallbackOutput = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(mockUri, "wt") } throws IllegalArgumentException("Mode wt not supported")
        every { contentResolver.openOutputStream(mockUri) } returns fallbackOutput

        val result = gateway.writeText("content://backup/out.json", "fallback-after-throw")

        assertTrue(result.isSuccess)
        assertEquals("fallback-after-throw", fallbackOutput.toString(Charsets.UTF_8.name()))
        verify(exactly = 1) { contentResolver.openOutputStream(mockUri, "wt") }
        verify(exactly = 1) { contentResolver.openOutputStream(mockUri) }
    }

    @Test
    fun writeText_wtModeReturnsNull_fallsBackToDefaultMode() = runTest(testDispatcher) {
        val fallbackOutput = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(mockUri, "wt") } returns null
        every { contentResolver.openOutputStream(mockUri) } returns fallbackOutput

        val result = gateway.writeText("content://backup/out.json", "fallback-after-null")

        assertTrue(result.isSuccess)
        assertEquals("fallback-after-null", fallbackOutput.toString(Charsets.UTF_8.name()))
        verify(exactly = 1) { contentResolver.openOutputStream(mockUri, "wt") }
        verify(exactly = 1) { contentResolver.openOutputStream(mockUri) }
    }

    @Test
    fun writeText_bothModesFailOrReturnNull_returnsFailure() = runTest(testDispatcher) {
        every { contentResolver.openOutputStream(mockUri, "wt") } throws FileNotFoundException("No wt")
        every { contentResolver.openOutputStream(mockUri) } returns null

        val result = gateway.writeText("content://backup/out.json", "will-fail")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
