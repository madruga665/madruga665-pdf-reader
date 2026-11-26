package com.madruga665.pdfreader.core.repository

import android.graphics.Bitmap
import android.net.Uri
import com.madruga665.pdfreader.core.model.PdfDocument

interface PdfDocumentRepository {
    suspend fun openDocument(uri: Uri): Result<PdfDocument>
    suspend fun renderPageBitmap(
        pageIndex: Int,
        targetWidthPx: Int,
        targetHeightPx: Int
    ): Result<Bitmap>
    fun closeCurrentDocument()
}
