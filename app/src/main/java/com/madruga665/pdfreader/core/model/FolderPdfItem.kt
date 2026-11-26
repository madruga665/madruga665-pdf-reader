package com.madruga665.pdfreader.core.model

data class FolderPdfItem(
    val uri: String,
    val fileName: String,
    val fileSizeBytes: Long = 0L,
    val lastModified: Long = 0L
) {
    init {
        require(uri.isNotBlank()) { "uri cannot be blank" }
        require(fileName.isNotBlank()) { "fileName cannot be blank" }
        require(fileSizeBytes >= 0L) { "fileSizeBytes cannot be negative, was $fileSizeBytes" }
        require(lastModified >= 0L) { "lastModified cannot be negative, was $lastModified" }
    }
}
