package com.madruga665.pdfreader.ui.reader

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madruga665.pdfreader.core.engine.AutoScrollEngine
import com.madruga665.pdfreader.core.engine.DefaultAutoScrollEngine
import com.madruga665.pdfreader.core.home.RecentsRepository
import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import com.madruga665.pdfreader.core.model.ReaderErrorState
import com.madruga665.pdfreader.core.model.ReadingSession
import com.madruga665.pdfreader.core.model.RecentDocument
import com.madruga665.pdfreader.core.model.SpeedPreset
import com.madruga665.pdfreader.core.repository.PdfDocumentRepository
import com.madruga665.pdfreader.core.storage.PdfDocumentStore
import com.madruga665.pdfreader.core.storage.ReadingSessionStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderViewModel(
    val repository: PdfDocumentRepository,
    private val sessionStore: ReadingSessionStore,
    private val recentsRepository: RecentsRepository? = null,
    val autoScrollEngine: AutoScrollEngine = DefaultAutoScrollEngine()
) : ViewModel() {

    private val _viewState = MutableStateFlow(ReaderViewState())
    val viewState: StateFlow<ReaderViewState> = _viewState.asStateFlow()

    private val _sideEffects = Channel<ReaderSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    private var autoHideOverlayJob: Job? = null
    private var lastLoadedUri: Uri? = null

    init {
        viewModelScope.launch {
            autoScrollEngine.scrollConfig.collect { config ->
                _viewState.update { current ->
                    current.copy(
                        isAutoScrolling = config.isScrolling,
                        currentSpeedPxPerSec = config.velocityPxPerSec,
                        activePreset = config.activePreset,
                        isOverlayControlsVisible = if (config.isScrolling) current.isOverlayControlsVisible else true
                    )
                }

                if (config.isScrolling) {
                    resetAutoHideTimer()
                } else {
                    autoHideOverlayJob?.cancel()
                    _viewState.update { it.copy(isOverlayControlsVisible = true) }
                }
            }
        }
    }

    fun onIntent(intent: ReaderIntent) {
        when (intent) {
            is ReaderIntent.OpenDocument -> openDocument(intent.uri)
            is ReaderIntent.ToggleAutoScroll -> autoScrollEngine.toggle()
            is ReaderIntent.PauseAutoScroll -> autoScrollEngine.pause()
            is ReaderIntent.ResumeAutoScroll -> autoScrollEngine.start()
            is ReaderIntent.SetSpeed -> setSpeed(intent.speedPxPerSec)
            is ReaderIntent.SelectPreset -> selectPreset(intent.preset)
            is ReaderIntent.ScrollProgressChanged -> onScrollProgressChanged(intent.offsetFraction)
            is ReaderIntent.PageChanged -> onPageChanged(intent.pageIndex)
            is ReaderIntent.SetZoomScale -> setZoomScale(intent.scale)
            is ReaderIntent.ToggleOverlayVisibility -> toggleOverlay()
            is ReaderIntent.RetryLoadDocument -> lastLoadedUri?.let { openDocument(it) }
            is ReaderIntent.EndOfDocumentReached -> onEndOfDocumentReached()
            is ReaderIntent.SaveCurrentSpeedAsDocumentDefault -> saveCurrentSpeedAsDocumentDefault()
            is ReaderIntent.ResetDocumentSpeedToDefault -> resetDocumentSpeedToDefault()
            is ReaderIntent.DismissError -> _viewState.update {
                it.copy(error = ReaderErrorState.None, unavailableDocumentId = null)
            }
            is ReaderIntent.RequestRelinkCurrentDocument -> requestRelinkCurrentDocument()
        }
    }

    private fun openDocument(uri: Uri) {
        lastLoadedUri = uri
        viewModelScope.launch {
            _viewState.update {
                it.copy(isLoading = true, error = ReaderErrorState.None, unavailableDocumentId = null)
            }
            val result = repository.openDocument(uri)
            result.fold(
                onSuccess = { doc ->
                    val uriString = uri.toString()
                    val resolvedFileName = doc.fileName.ifBlank { uri.lastPathSegment ?: PdfDocumentRecord.DEFAULT_FILE_NAME }
                    val safePageCount = doc.pageCount.coerceAtLeast(1)

                    if (sessionStore is PdfDocumentStore) {
                        val record = sessionStore.registerOrOpenDocument(
                            uri = uriString,
                            fileName = resolvedFileName,
                            pageCount = safePageCount
                        )
                        val initialSpeed = record.customDefaultSpeedPxPerSec
                            ?: record.preferredSpeedPxPerSec

                        autoScrollEngine.setVelocity(initialSpeed)

                        _viewState.update {
                            it.copy(
                                isLoading = false,
                                activeDocumentId = record.id,
                                unavailableDocumentId = null,
                                document = doc,
                                totalPages = doc.pageCount,
                                currentPageIndex = record.currentPageIndex.coerceIn(0, safePageCount - 1),
                                scrollProgressRatio = record.normalizedScrollRatio.coerceIn(0.0f, 1.0f),
                                zoomScale = record.zoomScale.coerceIn(0.5f, 4.0f),
                                currentSpeedPxPerSec = initialSpeed,
                                documentSavedSpeedPxPerSec = record.customDefaultSpeedPxPerSec,
                                error = ReaderErrorState.None
                            )
                        }
                    } else {
                        if (doc.fileName.isNotBlank()) {
                            sessionStore.saveDocumentFileName(uriString, doc.fileName)
                        }
                        val savedSession = sessionStore.getSession(uriString).firstOrNull()
                        val savedCustomSpeed = sessionStore.getCustomDocumentSpeed(uriString).firstOrNull()
                            ?: savedSession?.customDefaultSpeedPxPerSec

                        val initialSpeed = savedCustomSpeed
                            ?: savedSession?.preferredSpeedPxPerSec
                            ?: sessionStore.getGlobalDefaultSpeed().firstOrNull()
                            ?: SpeedPreset.MEDIUM.speedPxPerSec

                        autoScrollEngine.setVelocity(initialSpeed)

                        _viewState.update {
                            it.copy(
                                isLoading = false,
                                activeDocumentId = savedSession?.documentId?.takeIf { id -> id.isNotBlank() },
                                unavailableDocumentId = null,
                                document = doc,
                                totalPages = doc.pageCount,
                                currentPageIndex = (savedSession?.currentPageIndex ?: 0).coerceIn(0, safePageCount - 1),
                                scrollProgressRatio = (savedSession?.normalizedScrollRatio ?: 0.0f).coerceIn(0.0f, 1.0f),
                                zoomScale = (savedSession?.zoomScale ?: 1.0f).coerceIn(0.5f, 4.0f),
                                currentSpeedPxPerSec = initialSpeed,
                                documentSavedSpeedPxPerSec = savedCustomSpeed,
                                error = ReaderErrorState.None
                            )
                        }

                        recentsRepository?.recordDocumentAccess(
                            RecentDocument(
                                uri = uriString,
                                fileName = resolvedFileName,
                                pageCount = safePageCount,
                                lastReadPageIndex = (savedSession?.currentPageIndex ?: 0).coerceIn(0, safePageCount - 1),
                                progressRatio = (savedSession?.normalizedScrollRatio ?: 0.0f).coerceIn(0.0f, 1.0f),
                                lastAccessedTimestamp = System.currentTimeMillis()
                            )
                        )
                    }
                },
                onFailure = { error ->
                    val uriString = uri.toString()
                    val isAvailabilityError = error is java.io.FileNotFoundException || error is SecurityException
                    var unavailableDocId: String? = null

                    if (isAvailabilityError) {
                        if (sessionStore is PdfDocumentStore) {
                            sessionStore.setDocumentAvailability(uriString, false)
                            unavailableDocId = sessionStore.getByUri(uriString)?.id
                        } else {
                            recentsRepository?.updateAvailability(uriString, false)
                            unavailableDocId = recentsRepository?.getByUri(uriString)?.id?.takeIf { it.isNotBlank() }
                        }
                    }

                    val errorState = when {
                        error is java.io.FileNotFoundException ->
                            ReaderErrorState.FileNotFound(error.message ?: "Arquivo não encontrado ou movido.")
                        error is SecurityException ->
                            ReaderErrorState.FileNotFound("Permissão de acesso ao arquivo expirada ou indisponível. Selecione o PDF novamente.")
                        error.message?.contains("password", ignoreCase = true) == true ->
                            ReaderErrorState.PasswordRequired()
                        error is OutOfMemoryError ->
                            ReaderErrorState.OutOfMemory("Device ran out of memory. Try closing background applications.")
                        else ->
                            ReaderErrorState.CorruptedDocument(error.localizedMessage ?: "Unknown parsing error")
                    }
                    _viewState.update {
                        it.copy(
                            isLoading = false,
                            error = errorState,
                            unavailableDocumentId = unavailableDocId
                        )
                    }
                }
            )
        }
    }

    private fun requestRelinkCurrentDocument() {
        val docId = _viewState.value.unavailableDocumentId ?: return
        viewModelScope.launch {
            _sideEffects.send(ReaderSideEffect.LaunchRelinkPdfPicker(docId))
        }
    }

    private fun setSpeed(speedPxPerSec: Float) {
        autoScrollEngine.setVelocity(speedPxPerSec)
        persistSpeed(speedPxPerSec)
    }

    private fun selectPreset(preset: SpeedPreset) {
        autoScrollEngine.setPreset(preset)
        if (preset != SpeedPreset.CUSTOM) {
            persistSpeed(preset.speedPxPerSec)
        }
    }

    private fun persistSpeed(speedPxPerSec: Float) {
        viewModelScope.launch {
            val clampedSpeed = PdfDocumentRecord.sanitizeSpeed(speedPxPerSec)
            sessionStore.setGlobalDefaultSpeed(clampedSpeed)
            val state = _viewState.value
            val doc = state.document ?: return@launch
            val docId = state.activeDocumentId
            if (sessionStore is PdfDocumentStore && !docId.isNullOrBlank()) {
                sessionStore.updateReadingProgress(
                    documentId = docId,
                    currentPageIndex = state.currentPageIndex,
                    normalizedScrollRatio = state.scrollProgressRatio,
                    zoomScale = state.zoomScale,
                    preferredSpeedPxPerSec = clampedSpeed
                )
            } else {
                val session = ReadingSession(
                    documentId = docId.orEmpty(),
                    documentUri = doc.uri.toString(),
                    currentPageIndex = state.currentPageIndex,
                    normalizedScrollRatio = state.scrollProgressRatio,
                    zoomScale = state.zoomScale,
                    preferredSpeedPxPerSec = clampedSpeed,
                    fileName = doc.fileName
                )
                sessionStore.saveSession(session)
            }
        }
    }

    private fun onScrollProgressChanged(offsetFraction: Float) {
        val clamped = offsetFraction.coerceIn(0.0f, 1.0f)
        _viewState.update { it.copy(scrollProgressRatio = clamped) }
        saveCurrentProgress()
    }

    private fun onPageChanged(pageIndex: Int) {
        val maxIndex = (_viewState.value.totalPages - 1).coerceAtLeast(0)
        val safePage = if (_viewState.value.totalPages > 0) {
            pageIndex.coerceIn(0, maxIndex)
        } else {
            pageIndex.coerceAtLeast(0)
        }
        _viewState.update { it.copy(currentPageIndex = safePage) }
        saveCurrentProgress()
    }

    private fun onEndOfDocumentReached() {
        if (_viewState.value.isAutoScrolling) {
            autoScrollEngine.pause()
            viewModelScope.launch {
                _sideEffects.send(ReaderSideEffect.ReachedEndOfDocument)
            }
        }
    }

    private fun setZoomScale(scale: Float) {
        _viewState.update { it.copy(zoomScale = scale.coerceIn(0.5f, 4.0f)) }
        saveCurrentProgress()
    }

    private fun toggleOverlay() {
        _viewState.update { it.copy(isOverlayControlsVisible = !it.isOverlayControlsVisible) }
        if (_viewState.value.isOverlayControlsVisible && _viewState.value.isAutoScrolling) {
            resetAutoHideTimer()
        }
    }

    private fun resetAutoHideTimer() {
        autoHideOverlayJob?.cancel()
        autoHideOverlayJob = viewModelScope.launch {
            delay(2500)
            if (_viewState.value.isAutoScrolling) {
                _viewState.update { it.copy(isOverlayControlsVisible = false) }
            }
        }
    }

    private fun saveCurrentProgress() {
        val state = _viewState.value
        val doc = state.document ?: return
        val docUri = doc.uri.toString()
        val docId = state.activeDocumentId
        viewModelScope.launch {
            if (sessionStore is PdfDocumentStore && !docId.isNullOrBlank()) {
                sessionStore.updateReadingProgress(
                    documentId = docId,
                    currentPageIndex = state.currentPageIndex,
                    normalizedScrollRatio = state.scrollProgressRatio,
                    zoomScale = state.zoomScale,
                    preferredSpeedPxPerSec = state.currentSpeedPxPerSec.coerceIn(10f, 500f)
                )
            } else {
                val session = ReadingSession(
                    documentId = docId.orEmpty(),
                    documentUri = docUri,
                    currentPageIndex = state.currentPageIndex,
                    normalizedScrollRatio = state.scrollProgressRatio,
                    zoomScale = state.zoomScale,
                    preferredSpeedPxPerSec = state.currentSpeedPxPerSec.coerceIn(10f, 500f),
                    fileName = doc.fileName
                )
                sessionStore.saveSession(session)
                recentsRepository?.recordDocumentAccess(
                    RecentDocument(
                        id = docId.orEmpty(),
                        uri = docUri,
                        fileName = doc.fileName.ifBlank { doc.uri.lastPathSegment ?: PdfDocumentRecord.DEFAULT_FILE_NAME },
                        pageCount = doc.pageCount.coerceAtLeast(1),
                        lastReadPageIndex = state.currentPageIndex.coerceIn(0, (doc.pageCount - 1).coerceAtLeast(0)),
                        progressRatio = state.scrollProgressRatio.coerceIn(0.0f, 1.0f),
                        lastAccessedTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun saveCurrentSpeedAsDocumentDefault() {
        val doc = _viewState.value.document ?: return
        val docUri = doc.uri.toString()
        val speed = _viewState.value.currentSpeedPxPerSec
        viewModelScope.launch {
            sessionStore.saveCustomDocumentSpeed(docUri, speed)
            if (doc.fileName.isNotBlank()) {
                sessionStore.saveDocumentFileName(docUri, doc.fileName)
            }
            _viewState.update { it.copy(documentSavedSpeedPxPerSec = speed) }
            _sideEffects.send(ReaderSideEffect.SpeedSavedForDocument(speed))
        }
    }

    private fun resetDocumentSpeedToDefault() {
        val docUri = _viewState.value.document?.uri?.toString() ?: return
        viewModelScope.launch {
            sessionStore.clearCustomDocumentSpeed(docUri)
            val globalDefault = sessionStore.getGlobalDefaultSpeed().firstOrNull()
                ?: SpeedPreset.MEDIUM.speedPxPerSec
            autoScrollEngine.setVelocity(globalDefault)
            _viewState.update {
                it.copy(
                    documentSavedSpeedPxPerSec = null,
                    currentSpeedPxPerSec = globalDefault
                )
            }
            _sideEffects.send(ReaderSideEffect.SpeedResetToDefault)
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoScrollEngine.pause()
        repository.closeCurrentDocument()
    }
}
