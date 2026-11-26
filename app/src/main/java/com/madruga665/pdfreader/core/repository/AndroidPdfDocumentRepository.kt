package com.madruga665.pdfreader.core.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.collection.LruCache
import com.madruga665.pdfreader.core.model.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class AndroidPdfDocumentRepository(
    private val context: Context,
    maxCacheSizeBytes: Int = (Runtime.getRuntime().maxMemory() / 8).toInt()
) : PdfDocumentRepository {

    private val mutex = Mutex()
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var currentDocument: PdfDocument? = null

    private val bitmapCache = object : LruCache<String, Bitmap>(maxCacheSizeBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    override suspend fun openDocument(uri: Uri): Result<PdfDocument> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                closeInternal()

                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: return@withContext Result.failure(FileNotFoundException("Cannot open file descriptor for $uri"))
                
                val renderer = try {
                    PdfRenderer(pfd)
                } catch (e: Exception) {
                    pfd.close()
                    return@withContext Result.failure(e)
                }

                val pageCount = renderer.pageCount
                if (pageCount < 1) {
                    renderer.close()
                    pfd.close()
                    return@withContext Result.failure(IllegalStateException("PDF contains no pages"))
                }

                val fileName = extractFileName(uri)
                val fileSize = extractFileSize(uri)

                val aspectRatios = mutableListOf<Float>()
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    val ratio = if (page.height > 0) page.width.toFloat() / page.height.toFloat() else 1.0f
                    aspectRatios.add(ratio)
                    page.close()
                }

                val doc = PdfDocument(
                    uri = uri,
                    fileName = fileName,
                    pageCount = pageCount,
                    fileSizeBytes = fileSize,
                    pageAspectRatios = aspectRatios
                )

                fileDescriptor = pfd
                pdfRenderer = renderer
                currentDocument = doc

                Result.success(doc)
            } catch (e: Exception) {
                closeInternal()
                Result.failure(e)
            }
        }
    }

    override suspend fun renderPageBitmap(
        pageIndex: Int,
        targetWidthPx: Int,
        targetHeightPx: Int
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val renderer = pdfRenderer ?: return@withContext Result.failure(IllegalStateException("No PDF document currently opened"))
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                return@withContext Result.failure(IndexOutOfBoundsException("Page index $pageIndex out of range 0..${renderer.pageCount - 1}"))
            }

            val cacheKey = "$pageIndex-$targetWidthPx-$targetHeightPx"
            val cached = bitmapCache.get(cacheKey)
            if (cached != null && !cached.isRecycled) {
                return@withContext Result.success(cached)
            }

            try {
                val page = renderer.openPage(pageIndex)
                val width = if (targetWidthPx > 0) targetWidthPx else page.width
                val height = if (targetHeightPx > 0) targetHeightPx else page.height

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                bitmapCache.put(cacheKey, bitmap)
                Result.success(bitmap)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun closeCurrentDocument() {
        closeInternal()
    }

    private fun closeInternal() {
        bitmapCache.evictAll()
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {}
        try {
            fileDescriptor?.close()
        } catch (_: Exception) {}
        pdfRenderer = null
        fileDescriptor = null
        currentDocument = null
    }

    private fun extractFileName(uri: Uri): String {
        var name = uri.lastPathSegment ?: "Document.pdf"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex) ?: name
                }
            }
        } catch (_: Exception) {}
        return name
    }

    private fun extractFileSize(uri: Uri): Long {
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {}
        return size
    }
}
