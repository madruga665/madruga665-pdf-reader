package com.madruga665.pdfreader.ui.home

import android.net.Uri
import com.madruga665.pdfreader.core.backup.RestoreMode
import com.madruga665.pdfreader.core.model.FavoriteFolder

sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data class OpenDocument(val uri: Uri) : HomeIntent
    data class RemoveRecent(val uri: String) : HomeIntent
    data object ClearAllRecents : HomeIntent
    data class AddFolder(val treeUri: Uri) : HomeIntent
    data class RemoveFolder(val treeUri: String) : HomeIntent
    data class SelectFolder(val folder: FavoriteFolder?) : HomeIntent
    data class OpenPdfFromFolder(val uri: Uri) : HomeIntent
    data object DismissError : HomeIntent

    data object OpenBackupDialog : HomeIntent
    data object DismissBackupDialog : HomeIntent
    data class ExportBackupToUri(val uri: Uri) : HomeIntent
    data class InspectBackupUri(val uri: Uri) : HomeIntent
    data class SelectRestoreMode(val mode: RestoreMode) : HomeIntent
    data object ConfirmRestore : HomeIntent
    data object DismissRestorePreview : HomeIntent
}
