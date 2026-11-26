package com.madruga665.pdfreader.core.documents

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

data class InspectedPdfMetadata(
    val uri: String,
    val fileName: String,
    val pageCount: Int
)

interface DocumentAvailabilityChecker {
    suspend fun isReadable(uriString: String): Boolean
    suspend fun inspectPdf(uriString: String): Result<InspectedPdfMetadata>
    suspend fun takePersistableReadPermission(uriString: String)
}

class AndroidDocumentAvailabilityChecker(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DocumentAvailabilityChecker {

    override suspend fun isReadable(uriString: String): Boolean = withContext(ioDispatcher) {
        if (uriString.isBlank()) return@withContext false
        try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (_: SecurityException) {
            false
        } catch (_: FileNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun inspectPdf(uriString: String): Result<InspectedPdfMetadata> =
        withContext(ioDispatcher) {
            runCatching {
                require(uriString.isNotBlank()) { "URI do arquivo inválida." }
                val uri = Uri.parse(uriString)
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: throw FileNotFoundException("Não foi possível abrir o arquivo selecionado.")

                val pageCount = pfd.use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        renderer.pageCount
                    }
                }
                if (pageCount < 1) {
                    throw IllegalArgumentException("O arquivo selecionado não possui páginas PDF válidas.")
                }

                val displayName = resolveDisplayName(uri)
                InspectedPdfMetadata(
                    uri = uriString,
                    fileName = displayName,
                    pageCount = pageCount
                )
            }
        }

    override suspend fun takePersistableReadPermission(uriString: String) =
        withContext(ioDispatcher) {
            try {
                val uri = Uri.parse(uriString)
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Some providers do not support persistable permissions; safe to ignore.
            }
        }

    private fun resolveDisplayName(uri: Uri): String {
        val queried = try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
        val fallback = uri.lastPathSegment?.substringAfterLast('/')
        return PdfDocumentRecord.sanitizeFileName(queried ?: fallback)
    }
}
