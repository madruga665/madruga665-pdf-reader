package com.madruga665.pdfreader.core.model

data class FavoriteFolder(
    val treeUri: String,
    val displayName: String,
    val pathSummary: String = "",
    val pinnedTimestamp: Long = System.currentTimeMillis(),
    val cachedFileCount: Int = 0
) {
    init {
        require(treeUri.isNotBlank()) { "treeUri cannot be blank" }
        require(displayName.isNotBlank()) { "displayName cannot be blank" }
        require(pinnedTimestamp != 0L) { "pinnedTimestamp cannot be 0" }
        require(cachedFileCount >= 0) { "cachedFileCount cannot be negative, was $cachedFileCount" }
    }
}
