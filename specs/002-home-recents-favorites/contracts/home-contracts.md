# Repository & UI Contracts: Home Screen Recents and Favorite Folders

**Feature**: Home Screen Recents and Favorite Folders  
**Package**: `com.madruga665.pdfreader.core.home`  

## 1. RecentsRepository Interface

Contract for saving, retrieving, and pruning recently accessed documents.

```kotlin
package com.madruga665.pdfreader.core.home

import com.madruga665.pdfreader.core.model.RecentDocument
import kotlinx.coroutines.flow.Flow

interface RecentsRepository {
    /**
     * Emits the list of recent documents ordered by most recently accessed first.
     */
    fun getRecentDocuments(): Flow<List<RecentDocument>>

    /**
     * Adds or updates a document entry in the recents list, enforcing max cap (30).
     */
    suspend fun recordDocumentAccess(document: RecentDocument)

    /**
     * Removes a single document from the recents list.
     */
    suspend fun removeRecentDocument(uri: String)

    /**
     * Clears all recent entries.
     */
    suspend fun clearAllRecents()
}
```

---

## 2. FavoriteFoldersRepository Interface

Contract for managing user-pinned directories and querying contained PDF files.

```kotlin
package com.madruga665.pdfreader.core.home

import android.net.Uri
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.FolderPdfItem
import kotlinx.coroutines.flow.Flow

interface FavoriteFoldersRepository {
    /**
     * Emits user's pinned favorite folders.
     */
    fun getFavoriteFolders(): Flow<List<FavoriteFolder>>

    /**
     * Adds and persists a new favorite folder tree URI.
     */
    suspend fun addFavoriteFolder(treeUri: Uri, displayName: String? = null): Result<FavoriteFolder>

    /**
     * Unpins a favorite folder from the list.
     */
    suspend fun removeFavoriteFolder(treeUri: String)

    /**
     * Scans and returns all direct PDF child files inside the given folder tree.
     */
    suspend fun scanFolderPdfs(treeUri: Uri): Result<List<FolderPdfItem>>
}
```

---

## 3. Home MVI Contracts (Intent & Side Effect)

```kotlin
package com.madruga665.pdfreader.ui.home

import android.net.Uri
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.RecentDocument

sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data class OpenDocument(val uri: Uri) : HomeIntent
    data class RemoveRecent(val uri: String) : HomeIntent
    data object ClearAllRecents : HomeIntent
    data class AddFolder(val treeUri: Uri) : HomeIntent
    data class RemoveFolder(val treeUri: String) : HomeIntent
    data class SelectFolder(val folder: FavoriteFolder?) : HomeIntent
    data class OpenPdfFromFolder(val uri: Uri) : HomeIntent
}

sealed interface HomeSideEffect {
    data class NavigateToReader(val uri: Uri) : HomeSideEffect
    data class ShowToast(val message: String) : HomeSideEffect
}
```
