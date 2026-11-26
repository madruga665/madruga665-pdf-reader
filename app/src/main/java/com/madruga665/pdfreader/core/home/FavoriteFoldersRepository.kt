package com.madruga665.pdfreader.core.home

import android.net.Uri
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.FolderPdfItem
import kotlinx.coroutines.flow.Flow

interface FavoriteFoldersRepository {
    fun getFavoriteFolders(): Flow<List<FavoriteFolder>>
    suspend fun addFavoriteFolder(treeUri: Uri, displayName: String? = null): Result<FavoriteFolder>
    suspend fun removeFavoriteFolder(treeUri: String)
    suspend fun scanFolderPdfs(treeUri: Uri): Result<List<FolderPdfItem>>
    suspend fun replaceAllFavoriteFolders(folders: List<FavoriteFolder>) {}
}
