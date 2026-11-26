package com.madruga665.pdfreader.core.documents

import com.madruga665.pdfreader.core.model.PdfDocumentRecord

sealed interface RelinkOutcome {
    data class Relinked(val record: PdfDocumentRecord) : RelinkOutcome

    data class UriConflict(
        val targetRecord: PdfDocumentRecord,
        val conflictingRecord: PdfDocumentRecord,
        val newMetadata: InspectedPdfMetadata
    ) : RelinkOutcome

    data class InvalidTarget(val message: String) : RelinkOutcome
}

object DocumentRelinkPolicy {

    fun evaluateRelink(
        target: PdfDocumentRecord,
        newMetadata: InspectedPdfMetadata,
        existingByNewUri: PdfDocumentRecord?,
        nowMillis: Long = System.currentTimeMillis()
    ): RelinkOutcome {
        val safeUri = newMetadata.uri.trim()
        if (safeUri.isBlank()) {
            return RelinkOutcome.InvalidTarget("Arquivo selecionado inválido.")
        }
        if (newMetadata.pageCount < PdfDocumentRecord.MIN_PAGE_COUNT) {
            return RelinkOutcome.InvalidTarget("O arquivo selecionado não é um PDF válido.")
        }

        if (existingByNewUri != null && existingByNewUri.id != target.id) {
            return RelinkOutcome.UriConflict(
                targetRecord = target,
                conflictingRecord = existingByNewUri,
                newMetadata = newMetadata.copy(uri = safeUri)
            )
        }

        val safePageCount = PdfDocumentRecord.sanitizePageCount(newMetadata.pageCount)
        val adjustedPage = adjustPageIndexForNewPageCount(target.currentPageIndex, safePageCount)
        val adjustedRatio = adjustProgressRatio(
            adjustedPageIndex = adjustedPage,
            savedPageIndex = target.currentPageIndex,
            newPageCount = safePageCount,
            savedRatio = target.normalizedScrollRatio
        )
        val safeNow = PdfDocumentRecord.sanitizeTimestamp(nowMillis)

        val updated = target.copy(
            documentUri = safeUri,
            fileName = PdfDocumentRecord.sanitizeFileName(newMetadata.fileName),
            pageCount = safePageCount,
            currentPageIndex = adjustedPage,
            normalizedScrollRatio = adjustedRatio,
            lastAccessedAt = safeNow,
            isInRecents = true,
            isAvailable = true,
            updatedAt = safeNow
        )

        return RelinkOutcome.Relinked(updated)
    }

    fun adjustPageIndexForNewPageCount(savedPageIndex: Int, newPageCount: Int): Int =
        savedPageIndex.coerceIn(0, (newPageCount.coerceAtLeast(1) - 1))

    fun adjustProgressRatio(
        adjustedPageIndex: Int,
        savedPageIndex: Int,
        newPageCount: Int,
        savedRatio: Float
    ): Float {
        if (adjustedPageIndex == savedPageIndex) {
            return PdfDocumentRecord.sanitizeScrollRatio(savedRatio)
        }
        val safeCount = newPageCount.coerceAtLeast(1)
        return if (safeCount <= 1) {
            0.0f
        } else {
            PdfDocumentRecord.sanitizeScrollRatio(adjustedPageIndex.toFloat() / safeCount.toFloat())
        }
    }
}
