package com.madruga665.pdfreader.ui.home

import com.madruga665.pdfreader.core.backup.BackupPayload
import com.madruga665.pdfreader.core.backup.BackupSummary
import com.madruga665.pdfreader.core.backup.RestoreMode
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.FolderPdfItem
import com.madruga665.pdfreader.core.model.PdfDocumentRecord
import com.madruga665.pdfreader.core.model.RecentDocument

data class PendingRestorePreview(
    val payload: BackupPayload,
    val summary: BackupSummary,
    val selectedMode: RestoreMode = RestoreMode.MERGE
)

data class RelinkConflictState(
    val targetRecord: PdfDocumentRecord,
    val conflictingRecord: PdfDocumentRecord,
    val newUri: String,
    val newFileName: String,
    val newPageCount: Int
)

data class HomeViewState(
    val recentDocuments: List<RecentDocument> = emptyList(),
    val favoriteFolders: List<FavoriteFolder> = emptyList(),
    val activeFolder: FavoriteFolder? = null,
    val activeFolderPdfs: List<FolderPdfItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isBackupDialogVisible: Boolean = false,
    val isBackupInProgress: Boolean = false,
    val pendingRestorePreview: PendingRestorePreview? = null,
    val relinkConflict: RelinkConflictState? = null
) {
    val isEmpty: Boolean get() = recentDocuments.isEmpty() && favoriteFolders.isEmpty()
}
