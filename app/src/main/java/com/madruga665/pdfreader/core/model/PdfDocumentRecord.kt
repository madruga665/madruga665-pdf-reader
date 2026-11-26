package com.madruga665.pdfreader.core.model

import java.util.UUID

/**
 * Root persisted entity in `pdf_documents` (SQLite schema v1).
 * Unifies document identity (immutable UUID v4), reading session state,
 * custom per-document speed, recents membership, and file availability.
 */
data class PdfDocumentRecord(
    val id: String,
    val documentUri: String,
    val fileName: String,
    val pageCount: Int = MIN_PAGE_COUNT,
    val currentPageIndex: Int = 0,
    val normalizedScrollRatio: Float = 0.0f,
    val zoomScale: Float = DEFAULT_ZOOM_SCALE,
    val preferredSpeedPxPerSec: Float = DEFAULT_SPEED_PX_PER_SEC,
    val customDefaultSpeedPxPerSec: Float? = null,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val isInRecents: Boolean = true,
    val isAvailable: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank()) { "id cannot be blank" }
        require(documentUri.isNotBlank()) { "documentUri cannot be blank" }
        require(fileName.isNotBlank()) { "fileName cannot be blank" }
        require(pageCount >= MIN_PAGE_COUNT) { "pageCount must be >= $MIN_PAGE_COUNT, was $pageCount" }
        require(currentPageIndex in 0 until pageCount) {
            "currentPageIndex ($currentPageIndex) must be in [0, $pageCount)"
        }
        require(normalizedScrollRatio.isFinite() && normalizedScrollRatio in MIN_SCROLL_RATIO..MAX_SCROLL_RATIO) {
            "normalizedScrollRatio must be in [$MIN_SCROLL_RATIO, $MAX_SCROLL_RATIO], was $normalizedScrollRatio"
        }
        require(zoomScale.isFinite() && zoomScale in MIN_ZOOM_SCALE..MAX_ZOOM_SCALE) {
            "zoomScale must be in [$MIN_ZOOM_SCALE, $MAX_ZOOM_SCALE], was $zoomScale"
        }
        require(preferredSpeedPxPerSec.isFinite() && preferredSpeedPxPerSec in MIN_SPEED_PX_PER_SEC..MAX_SPEED_PX_PER_SEC) {
            "preferredSpeedPxPerSec must be in [$MIN_SPEED_PX_PER_SEC, $MAX_SPEED_PX_PER_SEC], was $preferredSpeedPxPerSec"
        }
        if (customDefaultSpeedPxPerSec != null) {
            require(
                customDefaultSpeedPxPerSec.isFinite() &&
                    customDefaultSpeedPxPerSec in MIN_SPEED_PX_PER_SEC..MAX_SPEED_PX_PER_SEC
            ) {
                "customDefaultSpeedPxPerSec must be in [$MIN_SPEED_PX_PER_SEC, $MAX_SPEED_PX_PER_SEC], was $customDefaultSpeedPxPerSec"
            }
        }
        require(lastAccessedAt > 0L) { "lastAccessedAt must be > 0" }
        require(createdAt > 0L) { "createdAt must be > 0" }
        require(updatedAt > 0L) { "updatedAt must be > 0" }
    }

    fun toReadingSession(): ReadingSession = ReadingSession(
        documentId = id,
        documentUri = documentUri,
        currentPageIndex = currentPageIndex,
        normalizedScrollRatio = normalizedScrollRatio,
        zoomScale = zoomScale,
        preferredSpeedPxPerSec = preferredSpeedPxPerSec,
        customDefaultSpeedPxPerSec = customDefaultSpeedPxPerSec,
        lastReadTimestamp = lastAccessedAt,
        fileName = fileName
    )

    fun toRecentDocument(): RecentDocument = RecentDocument(
        id = id,
        uri = documentUri,
        fileName = fileName,
        pageCount = pageCount,
        lastReadPageIndex = currentPageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0)),
        progressRatio = normalizedScrollRatio.coerceIn(MIN_SCROLL_RATIO, MAX_SCROLL_RATIO),
        lastAccessedTimestamp = lastAccessedAt,
        isAvailable = isAvailable
    )

    companion object {
        const val DEFAULT_FILE_NAME = "Document.pdf"
        const val MIN_PAGE_COUNT = 1
        const val MIN_SCROLL_RATIO = 0.0f
        const val MAX_SCROLL_RATIO = 1.0f
        const val MIN_ZOOM_SCALE = 0.5f
        const val MAX_ZOOM_SCALE = 4.0f
        const val DEFAULT_ZOOM_SCALE = 1.0f
        const val MIN_SPEED_PX_PER_SEC = 10.0f
        const val MAX_SPEED_PX_PER_SEC = 500.0f
        const val DEFAULT_SPEED_PX_PER_SEC = 90.0f

        fun sanitizeSpeed(speed: Float, fallback: Float = DEFAULT_SPEED_PX_PER_SEC): Float {
            if (!speed.isFinite()) return fallback.coerceIn(MIN_SPEED_PX_PER_SEC, MAX_SPEED_PX_PER_SEC)
            return speed.coerceIn(MIN_SPEED_PX_PER_SEC, MAX_SPEED_PX_PER_SEC)
        }

        fun sanitizeOptionalSpeed(speed: Float?): Float? {
            if (speed == null || !speed.isFinite()) return null
            return speed.coerceIn(MIN_SPEED_PX_PER_SEC, MAX_SPEED_PX_PER_SEC)
        }

        fun sanitizeZoom(zoom: Float): Float {
            if (!zoom.isFinite()) return DEFAULT_ZOOM_SCALE
            return zoom.coerceIn(MIN_ZOOM_SCALE, MAX_ZOOM_SCALE)
        }

        fun sanitizeScrollRatio(ratio: Float): Float {
            if (!ratio.isFinite()) return 0.0f
            return ratio.coerceIn(MIN_SCROLL_RATIO, MAX_SCROLL_RATIO)
        }

        fun sanitizePageCount(pageCount: Int): Int = pageCount.coerceAtLeast(MIN_PAGE_COUNT)

        fun sanitizePageIndex(pageIndex: Int, pageCount: Int): Int {
            val safeCount = sanitizePageCount(pageCount)
            return pageIndex.coerceIn(0, safeCount - 1)
        }

        fun sanitizeFileName(fileName: String?): String =
            fileName?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_FILE_NAME

        fun sanitizeTimestamp(timestamp: Long, fallback: Long = System.currentTimeMillis()): Long =
            when {
                timestamp > 0L -> timestamp
                fallback > 0L -> fallback
                else -> 1L
            }

        fun createSanitized(
            id: String = UUID.randomUUID().toString(),
            documentUri: String,
            fileName: String?,
            pageCount: Int = MIN_PAGE_COUNT,
            currentPageIndex: Int = 0,
            normalizedScrollRatio: Float = 0.0f,
            zoomScale: Float = DEFAULT_ZOOM_SCALE,
            preferredSpeedPxPerSec: Float = DEFAULT_SPEED_PX_PER_SEC,
            customDefaultSpeedPxPerSec: Float? = null,
            lastAccessedAt: Long = System.currentTimeMillis(),
            isInRecents: Boolean = true,
            isAvailable: Boolean = true,
            createdAt: Long = lastAccessedAt,
            updatedAt: Long = lastAccessedAt
        ): PdfDocumentRecord {
            val safePageCount = sanitizePageCount(pageCount)
            val safeLastAccessed = sanitizeTimestamp(lastAccessedAt)
            return PdfDocumentRecord(
                id = id.trim().takeIf { it.isNotEmpty() } ?: UUID.randomUUID().toString(),
                documentUri = documentUri.trim(),
                fileName = sanitizeFileName(fileName),
                pageCount = safePageCount,
                currentPageIndex = sanitizePageIndex(currentPageIndex, safePageCount),
                normalizedScrollRatio = sanitizeScrollRatio(normalizedScrollRatio),
                zoomScale = sanitizeZoom(zoomScale),
                preferredSpeedPxPerSec = sanitizeSpeed(preferredSpeedPxPerSec),
                customDefaultSpeedPxPerSec = sanitizeOptionalSpeed(customDefaultSpeedPxPerSec),
                lastAccessedAt = safeLastAccessed,
                isInRecents = isInRecents,
                isAvailable = isAvailable,
                createdAt = sanitizeTimestamp(createdAt, safeLastAccessed),
                updatedAt = sanitizeTimestamp(updatedAt, safeLastAccessed)
            )
        }
    }
}
