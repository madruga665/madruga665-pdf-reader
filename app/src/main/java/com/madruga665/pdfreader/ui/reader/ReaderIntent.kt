package com.madruga665.pdfreader.ui.reader

import android.net.Uri
import com.madruga665.pdfreader.core.model.SpeedPreset

sealed interface ReaderIntent {
    data class OpenDocument(val uri: Uri) : ReaderIntent
    data object ToggleAutoScroll : ReaderIntent
    data object PauseAutoScroll : ReaderIntent
    data object ResumeAutoScroll : ReaderIntent
    data class SetSpeed(val speedPxPerSec: Float) : ReaderIntent
    data class SelectPreset(val preset: SpeedPreset) : ReaderIntent
    data class ScrollProgressChanged(val offsetFraction: Float) : ReaderIntent
    data class PageChanged(val pageIndex: Int) : ReaderIntent
    data class SetZoomScale(val scale: Float) : ReaderIntent
    data object ToggleOverlayVisibility : ReaderIntent
    data object RetryLoadDocument : ReaderIntent
    data object EndOfDocumentReached : ReaderIntent
    data object SaveCurrentSpeedAsDocumentDefault : ReaderIntent
    data object ResetDocumentSpeedToDefault : ReaderIntent
    data object DismissError : ReaderIntent
}
