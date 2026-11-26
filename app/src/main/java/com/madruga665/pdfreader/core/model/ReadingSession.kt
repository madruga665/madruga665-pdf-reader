package com.madruga665.pdfreader.core.model

data class ReadingSession(
    val documentUri: String,
    val currentPageIndex: Int,
    val normalizedScrollRatio: Float,
    val zoomScale: Float = PdfDocumentRecord.DEFAULT_ZOOM_SCALE,
    val preferredSpeedPxPerSec: Float = PdfDocumentRecord.DEFAULT_SPEED_PX_PER_SEC,
    val customDefaultSpeedPxPerSec: Float? = null,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val fileName: String = "",
    val documentId: String = ""
) {
    init {
        require(documentUri.isNotBlank()) { "documentUri cannot be blank" }
        require(currentPageIndex >= 0) { "currentPageIndex must be >= 0" }
        require(normalizedScrollRatio.isFinite() && normalizedScrollRatio in 0.0f..1.0f) {
            "normalizedScrollRatio must be between 0.0 and 1.0, was $normalizedScrollRatio"
        }
        require(zoomScale.isFinite() && zoomScale in 0.5f..4.0f) {
            "zoomScale must be between 0.5 and 4.0, was $zoomScale"
        }
        require(preferredSpeedPxPerSec.isFinite() && preferredSpeedPxPerSec in 10.0f..500.0f) {
            "preferredSpeedPxPerSec must be between 10.0 and 500.0, was $preferredSpeedPxPerSec"
        }
        if (customDefaultSpeedPxPerSec != null) {
            require(customDefaultSpeedPxPerSec.isFinite() && customDefaultSpeedPxPerSec in 10.0f..500.0f) {
                "customDefaultSpeedPxPerSec must be between 10.0 and 500.0, was $customDefaultSpeedPxPerSec"
            }
        }
    }
}
