package com.madruga665.pdfreader.ui.reader

import com.madruga665.pdfreader.core.model.PdfDocument
import com.madruga665.pdfreader.core.model.ReaderErrorState
import com.madruga665.pdfreader.core.model.SpeedPreset

data class ReaderViewState(
    val isLoading: Boolean = false,
    val document: PdfDocument? = null,
    val currentPageIndex: Int = 0,
    val totalPages: Int = 0,
    val scrollProgressRatio: Float = 0.0f,
    val zoomScale: Float = 1.0f,
    val isAutoScrolling: Boolean = false,
    val currentSpeedPxPerSec: Float = SpeedPreset.MEDIUM.speedPxPerSec,
    val activePreset: SpeedPreset = SpeedPreset.MEDIUM,
    val documentSavedSpeedPxPerSec: Float? = null,
    val isOverlayControlsVisible: Boolean = true,
    val error: ReaderErrorState = ReaderErrorState.None
)
