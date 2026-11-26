package com.madruga665.pdfreader.core.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.FolderPdfItem
import com.madruga665.pdfreader.core.storage.PdfDocumentStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class SqliteFavoriteFoldersRepository(
    private val context: Context,
    private val documentStore: PdfDocumentStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FavoriteFoldersRepository {

    override fun getFavoriteFolders(): Flow<List<FavoriteFolder>> =
        documentStore.observeFavoriteFolders()

    override suspend fun addFavoriteFolder(
        treeUri: Uri,
        displayName: String?
    ): Result<FavoriteFolder> = withContext(ioDispatcher) {
        try {
            try {
                context.contentResolver?.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            val resolvedName = displayName?.takeIf { it.isNotBlank() }
                ?: DocumentFile.fromTreeUri(context, treeUri)?.name?.takeIf { it.isNotBlank() }
                ?: treeUri.lastPathSegment?.substringAfterLast(':')?.takeIf { it.isNotBlank() }
                ?: "Folder"

            val initialFileCount = countFolderPdfs(treeUri)

            val folder = FavoriteFolder(
                treeUri = treeUri.toString(),
                displayName = resolvedName,
                pathSummary = treeUri.lastPathSegment ?: "",
                pinnedTimestamp = System.currentTimeMillis(),
                cachedFileCount = initialFileCount
            )

            documentStore.upsertFavoriteFolder(folder)
            Result.success(folder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFavoriteFolder(treeUri: String): Unit = withContext(ioDispatcher) {
        try {
            try {
                context.contentResolver?.releasePersistableUriPermission(
                    Uri.parse(treeUri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            documentStore.removeFavoriteFolder(treeUri)
        } catch (_: Exception) {
        }
    }

    override suspend fun replaceAllFavoriteFolders(folders: List<FavoriteFolder>): Unit =
        withContext(ioDispatcher) {
            documentStore.replaceFavoriteFolders(folders)
        }

    override suspend fun scanFolderPdfs(treeUri: Uri): Result<List<FolderPdfItem>> =
        withContext(ioDispatcher) {
            try {
                val docDir = DocumentFile.fromTreeUri(context, treeUri)
                if (docDir == null || !docDir.canRead()) {
                    return@withContext Result.failure(
                        FileNotFoundException("Folder is inaccessible or permission was revoked: $treeUri")
                    )
                }

                val files = docDir.listFiles()
                val pdfItems = files.filter { file ->
                    val name = file.name ?: ""
                    name.endsWith(".pdf", ignoreCase = true) || file.type == "application/pdf"
                }.map { file ->
                    FolderPdfItem(
                        uri = file.uri.toString(),
                        fileName = file.name ?: "Document.pdf",
                        fileSizeBytes = file.length().coerceAtLeast(0L),
                        lastModified = file.lastModified().coerceAtLeast(0L)
                    )
                }.sortedBy { it.fileName.lowercase() }

                documentStore.updateFavoriteFolderFileCount(treeUri.toString(), pdfItems.size)
                Result.success(pdfItems)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun countFolderPdfs(treeUri: Uri): Int = try {
        val docDir = DocumentFile.fromTreeUri(context, treeUri)
        if (docDir == null || !docDir.canRead()) {
            0
        } else {
            docDir.listFiles().count { file ->
                val name = file.name ?: ""
                name.endsWith(".pdf", ignoreCase = true) || file.type == "application/pdf"
            }
        }
    } catch (_: Exception) {
        0
    }
}
