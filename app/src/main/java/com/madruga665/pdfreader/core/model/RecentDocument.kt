package com.madruga665.pdfreader.core.model

data class RecentDocument(
    val uri: String,
    val fileName: String,
    val pageCount: Int,
    val lastReadPageIndex: Int,
    val progressRatio: Float,
    val lastAccessedTimestamp: Long = System.currentTimeMillis(),
    val isAvailable: Boolean = true
) {
    init {
        require(uri.isNotBlank()) { "uri cannot be blank" }
        require(fileName.isNotBlank()) { "fileName cannot be blank" }
        require(pageCount >= 1) { "pageCount must be at least 1, was $pageCount" }
        require(lastReadPageIndex in 0 until pageCount) {
            "lastReadPageIndex ($lastReadPageIndex) out of range [0, $pageCount)"
        }
        require(progressRatio in 0.0f..1.0f) {
            "progressRatio must be in [0.0, 1.0], was $progressRatio"
        }
        require(lastAccessedTimestamp != 0L) { "lastAccessedTimestamp cannot be 0" }
    }
}
