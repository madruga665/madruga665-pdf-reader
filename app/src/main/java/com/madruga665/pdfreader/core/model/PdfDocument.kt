package com.madruga665.pdfreader.core.model

import android.net.Uri

data class PdfDocument(
    val uri: Uri,
    val fileName: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val isPasswordProtected: Boolean = false,
    val pageAspectRatios: List<Float>
) {
    init {
        require(pageCount >= 1) { "pageCount must be at least 1, but was $pageCount" }
        require(fileSizeBytes >= 0) { "fileSizeBytes cannot be negative, was $fileSizeBytes" }
        require(pageAspectRatios.size == pageCount) {
            "pageAspectRatios size (${pageAspectRatios.size}) must match pageCount ($pageCount)"
        }
    }
}
