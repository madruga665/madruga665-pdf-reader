package com.madruga665.pdfreader.ui.reader

import android.net.Uri
import app.cash.turbine.test
import com.madruga665.pdfreader.core.engine.DefaultAutoScrollEngine
import com.madruga665.pdfreader.core.home.RecentsRepository
import com.madruga665.pdfreader.core.model.PdfDocument
import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import com.madruga665.pdfreader.core.model.ReaderErrorState
import com.madruga665.pdfreader.core.model.ReadingSession
import com.madruga665.pdfreader.core.model.SpeedPreset
import com.madruga665.pdfreader.core.repository.PdfDocumentRepository
import com.madruga665.pdfreader.core.storage.PdfDocumentStore
import com.madruga665.pdfreader.core.storage.ReadingSessionStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.FileNotFoundException

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: PdfDocumentRepository
    private lateinit var sessionStore: ReadingSessionStore
    private lateinit var autoScrollEngine: DefaultAutoScrollEngine
    private lateinit var viewModel: ReaderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        sessionStore = mockk(relaxed = true)
        autoScrollEngine = DefaultAutoScrollEngine()

        coEvery { sessionStore.getSession(any()) } returns flowOf(null)
        coEvery { sessionStore.getGlobalDefaultSpeed() } returns flowOf(90.0f)
        coEvery { sessionStore.getCustomDocumentSpeed(any()) } returns flowOf(null)
        coEvery { sessionStore.findCustomSpeedByFileName(any()) } returns null

        viewModel = ReaderViewModel(
            repository = repository,
            sessionStore = sessionStore,
            autoScrollEngine = autoScrollEngine
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun openDocument_success_updatesViewState() = runTest {
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://docs/sample.pdf"
        val doc = PdfDocument(
            uri = uri,
            fileName = "sample.pdf",
            pageCount = 10,
            fileSizeBytes = 1024,
            pageAspectRatios = List(10) { 0.75f }
        )
        coEvery { repository.openDocument(uri) } returns Result.success(doc)

        viewModel.onIntent(ReaderIntent.OpenDocument(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.viewState.value
        assertFalse(state.isLoading)
        assertEquals(doc, state.document)
        assertEquals(10, state.totalPages)
    }

    @Test
    fun openDocument_withPdfDocumentStore_registersStableIdAndPersistsProgressById() = runTest {
        val pdfStore = mockk<PdfDocumentStore>(relaxed = true)
        val uriString = "content://docs/manual.pdf"
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns uriString
        every { uri.lastPathSegment } returns "manual.pdf"
        val doc = PdfDocument(
            uri = uri,
            fileName = "manual.pdf",
            pageCount = 12,
            fileSizeBytes = 2048,
            pageAspectRatios = List(12) { 0.75f }
        )
        val storedRecord = PdfDocumentRecord.createSanitized(
            id = "uuid-manual-1",
            documentUri = uriString,
            fileName = "manual.pdf",
            pageCount = 12,
            currentPageIndex = 4,
            normalizedScrollRatio = 0.35f,
            zoomScale = 1.6f,
            preferredSpeedPxPerSec = 130.0f,
            customDefaultSpeedPxPerSec = 160.0f,
            lastAccessedAt = 5000L
        )
        coEvery { repository.openDocument(uri) } returns Result.success(doc)
        coEvery { pdfStore.registerOrOpenDocument(uriString, "manual.pdf", 12, any()) } returns storedRecord
        coEvery { pdfStore.getGlobalDefaultSpeed() } returns flowOf(90.0f)

        val vm = ReaderViewModel(
            repository = repository,
            sessionStore = pdfStore,
            autoScrollEngine = autoScrollEngine
        )

        vm.onIntent(ReaderIntent.OpenDocument(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.viewState.value
        assertEquals("uuid-manual-1", state.activeDocumentId)
        assertEquals(4, state.currentPageIndex)
        assertEquals(0.35f, state.scrollProgressRatio, 0.001f)
        assertEquals(1.6f, state.zoomScale, 0.001f)
        assertEquals(160.0f, state.currentSpeedPxPerSec, 0.001f)
        assertEquals(160.0f, state.documentSavedSpeedPxPerSec ?: 0f, 0.001f)

        vm.onIntent(ReaderIntent.PageChanged(6))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 1) {
            pdfStore.updateReadingProgress(
                documentId = "uuid-manual-1",
                currentPageIndex = 6,
                normalizedScrollRatio = any(),
                zoomScale = 1.6f,
                preferredSpeedPxPerSec = 160.0f,
                nowMillis = any()
            )
        }
    }

    @Test
    fun openDocument_doesNotReconcileCustomSpeedImplicitlyByFileName() = runTest {
        val newUriString = "content://new_device/doc_42.pdf"
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns newUriString
        every { uri.lastPathSegment } returns "livro-estudo.pdf"
        val doc = PdfDocument(
            uri = uri,
            fileName = "livro-estudo.pdf",
            pageCount = 12,
            fileSizeBytes = 2048,
            pageAspectRatios = List(12) { 0.75f }
        )
        coEvery { repository.openDocument(uri) } returns Result.success(doc)
        coEvery { sessionStore.getSession(newUriString) } returns flowOf(null)
        coEvery { sessionStore.getCustomDocumentSpeed(newUriString) } returns flowOf(null)
        coEvery { sessionStore.findCustomSpeedByFileName("livro-estudo.pdf") } returns 185.0f

        viewModel.onIntent(ReaderIntent.OpenDocument(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.viewState.value
        assertEquals(90.0f, state.currentSpeedPxPerSec, 0.001f)
        assertNull(state.documentSavedSpeedPxPerSec)
        coVerify(exactly = 0) { sessionStore.findCustomSpeedByFileName(any()) }
        coVerify(exactly = 0) { sessionStore.saveCustomDocumentSpeed(newUriString, any()) }
    }

    @Test
    fun openDocument_fileNotFoundOnRegisteredDocument_marksUnavailableAndExposesRelinkId() = runTest {
        val pdfStore = mockk<PdfDocumentStore>(relaxed = true)
        val uriString = "content://docs/moved.pdf"
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns uriString
        val existing = PdfDocumentRecord.createSanitized(
            id = "uuid-moved-1",
            documentUri = uriString,
            fileName = "moved.pdf",
            pageCount = 8,
            customDefaultSpeedPxPerSec = 170.0f,
            lastAccessedAt = 1000L
        )
        coEvery { repository.openDocument(uri) } returns Result.failure(FileNotFoundException("Missing"))
        coEvery { pdfStore.getByUri(uriString) } returns existing

        val vm = ReaderViewModel(
            repository = repository,
            sessionStore = pdfStore,
            autoScrollEngine = autoScrollEngine
        )

        vm.onIntent(ReaderIntent.OpenDocument(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.viewState.value.error is ReaderErrorState.FileNotFound)
        assertEquals("uuid-moved-1", vm.viewState.value.unavailableDocumentId)
        coVerify(exactly = 1) { pdfStore.setDocumentAvailability(uriString, false) }

        vm.sideEffects.test {
            vm.onIntent(ReaderIntent.RequestRelinkCurrentDocument)
            testDispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is ReaderSideEffect.LaunchRelinkPdfPicker)
            assertEquals("uuid-moved-1", (effect as ReaderSideEffect.LaunchRelinkPdfPicker).documentId)
        }
    }

    @Test
    fun toggleAutoScroll_updatesIsAutoScrolling() = runTest {
        assertFalse(viewModel.viewState.value.isAutoScrolling)
        viewModel.onIntent(ReaderIntent.ToggleAutoScroll)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.viewState.value.isAutoScrolling)

        viewModel.onIntent(ReaderIntent.ToggleAutoScroll)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.viewState.value.isAutoScrolling)
    }

    @Test
    fun scrollProgressChanged_updatesProgressRatio() = runTest {
        viewModel.onIntent(ReaderIntent.ScrollProgressChanged(0.5f))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0.5f, viewModel.viewState.value.scrollProgressRatio)
    }

    @Test
    fun setSpeed_updatesVelocityAndPersists() = runTest {
        viewModel.onIntent(ReaderIntent.SetSpeed(150f))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(150f, viewModel.viewState.value.currentSpeedPxPerSec)
        coVerify { sessionStore.setGlobalDefaultSpeed(150f) }
    }

    @Test
    fun selectPreset_updatesSpeedAndPreset() = runTest {
        viewModel.onIntent(ReaderIntent.SelectPreset(SpeedPreset.FAST))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SpeedPreset.FAST.speedPxPerSec, viewModel.viewState.value.currentSpeedPxPerSec)
        assertEquals(SpeedPreset.FAST, viewModel.viewState.value.activePreset)
    }

    @Test
    fun endOfDocumentReached_haltsAutoScrollAndSendsSideEffect() = runTest {
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://docs/book.pdf"
        val doc = PdfDocument(
            uri = uri,
            fileName = "book.pdf",
            pageCount = 5,
            fileSizeBytes = 2048,
            pageAspectRatios = List(5) { 0.75f }
        )
        coEvery { repository.openDocument(uri) } returns Result.success(doc)
        viewModel.onIntent(ReaderIntent.OpenDocument(uri))
        viewModel.onIntent(ReaderIntent.ResumeAutoScroll)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.viewState.value.isAutoScrolling)

        viewModel.sideEffects.test {
            viewModel.onIntent(ReaderIntent.EndOfDocumentReached)
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.viewState.value.isAutoScrolling)
            val effect = awaitItem()
            assertTrue(effect is ReaderSideEffect.ReachedEndOfDocument)
        }
    }

    @Test
    fun orientationChange_restoresNormalizedScrollPosition() = runTest {
        val uriString = "content://docs/sample.pdf"
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns uriString
        val savedSession = ReadingSession(
            documentUri = uriString,
            currentPageIndex = 3,
            normalizedScrollRatio = 0.6f,
            zoomScale = 1.5f,
            preferredSpeedPxPerSec = 120f
        )
        val doc = PdfDocument(
            uri = uri,
            fileName = "sample.pdf",
            pageCount = 10,
            fileSizeBytes = 1024,
            pageAspectRatios = List(10) { 0.75f }
        )
        coEvery { repository.openDocument(uri) } returns Result.success(doc)
        coEvery { sessionStore.getSession(uriString) } returns flowOf(savedSession)

        viewModel.onIntent(ReaderIntent.OpenDocument(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.viewState.value
        assertEquals(3, state.currentPageIndex)
        assertEquals(0.6f, state.scrollProgressRatio, 0.001f)
        assertEquals(1.5f, state.zoomScale, 0.001f)
        assertEquals(120f, state.currentSpeedPxPerSec, 0.001f)
    }

    @Test
    fun openDocument_recordsRecentDocumentAccess() = runTest {
        val recentsRepo = mockk<RecentsRepository>(relaxed = true)
        val vmWithRecents = ReaderViewModel(
            repository = repository,
            sessionStore = sessionStore,
            recentsRepository = recentsRepo,
            autoScrollEngine = autoScrollEngine
        )
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://test/doc.pdf"
        every { uri.lastPathSegment } returns "doc.pdf"
        val doc = PdfDocument(
            uri = uri,
            fileName = "doc.pdf",
            pageCount = 3,
            fileSizeBytes = 1000,
            pageAspectRatios = List(3) { 0.75f }
        )
        coEvery { repository.openDocument(uri) } returns Result.success(doc)
        vmWithRecents.onIntent(ReaderIntent.OpenDocument(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { recentsRepo.recordDocumentAccess(match { it.uri == "content://test/doc.pdf" && it.pageCount == 3 }) }
    }

    @Test
    fun openDocument_withCustomSavedSpeed_initializesAtCustomSpeed() = runTest {
        val uriString = "content://test/custom_doc.pdf"
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns uriString
        every { uri.lastPathSegment } returns "custom_doc.pdf"
        val doc = PdfDocument(
            uri = uri,
            fileName = "custom_doc.pdf",
            pageCount = 4,
            fileSizeBytes = 1000,
            pageAspectRatios = List(4) { 0.75f }
        )
        coEvery { repository.openDocument(uri) } returns Result.success(doc)
        coEvery { sessionStore.getCustomDocumentSpeed(uriString) } returns flowOf(175.0f)

        viewModel.onIntent(ReaderIntent.OpenDocument(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.viewState.value
        assertEquals(175.0f, state.currentSpeedPxPerSec, 0.001f)
        assertEquals(175.0f, state.documentSavedSpeedPxPerSec ?: 0f, 0.001f)
    }

    @Test
    fun saveCurrentSpeedAsDocumentDefault_persistsSpeedAndUpdatesState() = runTest {
        val uriString = "content://test/doc.pdf"
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns uriString
        every { uri.lastPathSegment } returns "doc.pdf"
        val doc = PdfDocument(
            uri = uri,
            fileName = "doc.pdf",
            pageCount = 3,
            fileSizeBytes = 1000,
            pageAspectRatios = List(3) { 0.75f }
        )
        coEvery { repository.openDocument(uri) } returns Result.success(doc)
        viewModel.onIntent(ReaderIntent.OpenDocument(uri))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onIntent(ReaderIntent.SetSpeed(145.0f))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sideEffects.test {
            viewModel.onIntent(ReaderIntent.SaveCurrentSpeedAsDocumentDefault)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { sessionStore.saveCustomDocumentSpeed(uriString, 145.0f) }
            assertEquals(145.0f, viewModel.viewState.value.documentSavedSpeedPxPerSec ?: 0f, 0.001f)
            val effect = awaitItem()
            assertTrue(effect is ReaderSideEffect.SpeedSavedForDocument)
            assertEquals(145.0f, (effect as ReaderSideEffect.SpeedSavedForDocument).speedPxPerSec, 0.001f)
        }
    }

    @Test
    fun resetDocumentSpeedToDefault_clearsCustomSpeedAndRevertsToGlobal() = runTest {
        val uriString = "content://test/doc.pdf"
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns uriString
        every { uri.lastPathSegment } returns "doc.pdf"
        val doc = PdfDocument(
            uri = uri,
            fileName = "doc.pdf",
            pageCount = 3,
            fileSizeBytes = 1000,
            pageAspectRatios = List(3) { 0.75f }
        )
        coEvery { repository.openDocument(uri) } returns Result.success(doc)
        coEvery { sessionStore.getCustomDocumentSpeed(uriString) } returns flowOf(180.0f)
        coEvery { sessionStore.getGlobalDefaultSpeed() } returns flowOf(90.0f)

        viewModel.onIntent(ReaderIntent.OpenDocument(uri))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(180.0f, viewModel.viewState.value.documentSavedSpeedPxPerSec ?: 0f, 0.001f)

        viewModel.sideEffects.test {
            viewModel.onIntent(ReaderIntent.ResetDocumentSpeedToDefault)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { sessionStore.clearCustomDocumentSpeed(uriString) }
            assertNull(viewModel.viewState.value.documentSavedSpeedPxPerSec)
            assertEquals(90.0f, viewModel.viewState.value.currentSpeedPxPerSec, 0.001f)
            val effect = awaitItem()
            assertTrue(effect is ReaderSideEffect.SpeedResetToDefault)
        }
    }

    @Test
    fun openDocument_securityException_mapsGracefullyToFileNotFoundError() = runTest {
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://docs/sec.pdf"
        coEvery { repository.openDocument(uri) } returns Result.failure(SecurityException("Permission Denial"))

        viewModel.onIntent(ReaderIntent.OpenDocument(uri))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.viewState.value.error is ReaderErrorState.FileNotFound)
    }

    @Test
    fun dismissError_clearsErrorStateAfterSecurityException() = runTest {
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://docs/sec.pdf"
        coEvery { repository.openDocument(uri) } returns Result.failure(SecurityException("Permission Denial"))

        viewModel.onIntent(ReaderIntent.OpenDocument(uri))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.viewState.value.error is ReaderErrorState.FileNotFound)

        viewModel.onIntent(ReaderIntent.DismissError)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ReaderErrorState.None, viewModel.viewState.value.error)
    }

    @Test
    fun repository_exposesInjectedInstance() {
        assertSame(repository, viewModel.repository)
    }
}
