package com.madruga665.pdfreader.ui.reader

sealed interface ReaderSideEffect {
    data class ShowToast(val message: String) : ReaderSideEffect
    data object ReachedEndOfDocument : ReaderSideEffect
    data class RequestDocumentPassword(val hint: String) : ReaderSideEffect
    data class SpeedSavedForDocument(val speedPxPerSec: Float) : ReaderSideEffect
    data object SpeedResetToDefault : ReaderSideEffect
    data class LaunchRelinkPdfPicker(val documentId: String) : ReaderSideEffect
}
