package com.madruga665.pdfreader.core.documents

import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentRelinkPolicyTest {

    private fun sampleUnavailableTarget(
        id: String = "uuid-target-1",
        uri: String = "content://docs/old.pdf",
        pageCount: Int = 10,
        savedPageIndex: Int = 5,
        customSpeed: Float? = 160.0f
    ): PdfDocumentRecord = PdfDocumentRecord.createSanitized(
        id = id,
        documentUri = uri,
        fileName = "old.pdf",
        pageCount = pageCount,
        currentPageIndex = savedPageIndex,
        normalizedScrollRatio = 0.5f,
        zoomScale = 1.75f,
        preferredSpeedPxPerSec = 160.0f,
        customDefaultSpeedPxPerSec = customSpeed,
        lastAccessedAt = 1000L,
        isInRecents = true,
        isAvailable = false
    )

    @Test
    fun `evaluateRelink preserves id and custom speed and clamps saved page when new PDF has fewer pages`() {
        val target = sampleUnavailableTarget(pageCount = 10, savedPageIndex = 5, customSpeed = 160.0f)
        val newMetadata = InspectedPdfMetadata(
            uri = "content://docs/renamed.pdf",
            fileName = "renamed.pdf",
            pageCount = 3
        )

        val outcome = DocumentRelinkPolicy.evaluateRelink(
            target = target,
            newMetadata = newMetadata,
            existingByNewUri = null,
            nowMillis = 5000L
        )

        assertTrue(outcome is RelinkOutcome.Relinked)
        val relinked = (outcome as RelinkOutcome.Relinked).record
        assertEquals(target.id, relinked.id)
        assertEquals("content://docs/renamed.pdf", relinked.documentUri)
        assertEquals("renamed.pdf", relinked.fileName)
        assertEquals(3, relinked.pageCount)
        assertEquals(2, relinked.currentPageIndex) // min(5, 3 - 1) per RN-04
        assertEquals(160.0f, relinked.customDefaultSpeedPxPerSec ?: 0f, 0.001f)
        assertEquals(1.75f, relinked.zoomScale, 0.001f)
        assertTrue(relinked.isAvailable)
        assertTrue(relinked.isInRecents)
    }

    @Test
    fun `evaluateRelink detects URI conflict when newUri belongs to another document id`() {
        val target = sampleUnavailableTarget(id = "uuid-a", uri = "content://docs/a.pdf")
        val existingOther = PdfDocumentRecord.createSanitized(
            id = "uuid-b",
            documentUri = "content://docs/b.pdf",
            fileName = "b.pdf",
            pageCount = 8,
            lastAccessedAt = 2000L
        )
        val newMetadata = InspectedPdfMetadata(
            uri = "content://docs/b.pdf",
            fileName = "b.pdf",
            pageCount = 8
        )

        val outcome = DocumentRelinkPolicy.evaluateRelink(
            target = target,
            newMetadata = newMetadata,
            existingByNewUri = existingOther,
            nowMillis = 5000L
        )

        assertTrue(outcome is RelinkOutcome.UriConflict)
        val conflict = outcome as RelinkOutcome.UriConflict
        assertEquals("uuid-a", conflict.targetRecord.id)
        assertEquals("uuid-b", conflict.conflictingRecord.id)
    }

    @Test
    fun `evaluateRelink allows relinking to same URI owned by same document id`() {
        val target = sampleUnavailableTarget(id = "uuid-a", uri = "content://docs/a.pdf", pageCount = 10, savedPageIndex = 4)
        val newMetadata = InspectedPdfMetadata(
            uri = "content://docs/a.pdf",
            fileName = "a.pdf",
            pageCount = 10
        )

        val outcome = DocumentRelinkPolicy.evaluateRelink(
            target = target,
            newMetadata = newMetadata,
            existingByNewUri = target,
            nowMillis = 5000L
        )

        assertTrue(outcome is RelinkOutcome.Relinked)
        val relinked = (outcome as RelinkOutcome.Relinked).record
        assertEquals(4, relinked.currentPageIndex)
        assertEquals(0.5f, relinked.normalizedScrollRatio, 0.001f)
        assertTrue(relinked.isAvailable)
    }

    @Test
    fun `evaluateRelink returns InvalidTarget for blank URI or zero pages`() {
        val target = sampleUnavailableTarget()

        val blankUriOutcome = DocumentRelinkPolicy.evaluateRelink(
            target = target,
            newMetadata = InspectedPdfMetadata(uri = "   ", fileName = "x.pdf", pageCount = 3),
            existingByNewUri = null,
            nowMillis = 5000L
        )
        assertTrue(blankUriOutcome is RelinkOutcome.InvalidTarget)

        val zeroPageOutcome = DocumentRelinkPolicy.evaluateRelink(
            target = target,
            newMetadata = InspectedPdfMetadata(uri = "content://docs/empty.pdf", fileName = "empty.pdf", pageCount = 0),
            existingByNewUri = null,
            nowMillis = 5000L
        )
        assertTrue(zeroPageOutcome is RelinkOutcome.InvalidTarget)
    }

    @Test
    fun `adjustPageIndexForNewPageCount clamps safely across edge cases`() {
        assertEquals(0, DocumentRelinkPolicy.adjustPageIndexForNewPageCount(savedPageIndex = 5, newPageCount = 1))
        assertEquals(2, DocumentRelinkPolicy.adjustPageIndexForNewPageCount(savedPageIndex = 5, newPageCount = 3))
        assertEquals(5, DocumentRelinkPolicy.adjustPageIndexForNewPageCount(savedPageIndex = 5, newPageCount = 10))
        assertEquals(0, DocumentRelinkPolicy.adjustPageIndexForNewPageCount(savedPageIndex = -2, newPageCount = 10))
    }
}
