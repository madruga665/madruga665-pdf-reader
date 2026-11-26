package com.madruga665.pdfreader.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madruga665.pdfreader.core.backup.BackupParseResult
import com.madruga665.pdfreader.core.backup.BackupRepository
import com.madruga665.pdfreader.core.backup.RestoreMode
import com.madruga665.pdfreader.core.home.FavoriteFoldersRepository
import com.madruga665.pdfreader.core.home.RecentsRepository
import com.madruga665.pdfreader.core.model.FavoriteFolder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val recentsRepository: RecentsRepository,
    private val favoriteFoldersRepository: FavoriteFoldersRepository,
    private val backupRepository: BackupRepository? = null
) : ViewModel() {

    private val _viewState = MutableStateFlow(HomeViewState(isLoading = true))
    val viewState: StateFlow<HomeViewState> = _viewState.asStateFlow()

    private val _sideEffects = Channel<HomeSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    init {
        viewModelScope.launch {
            recentsRepository.getRecentDocuments().collect { recents ->
                _viewState.update { it.copy(recentDocuments = recents, isLoading = false) }
            }
        }

        viewModelScope.launch {
            favoriteFoldersRepository.getFavoriteFolders().collect { folders ->
                _viewState.update { it.copy(favoriteFolders = folders) }
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Refresh -> refresh()
            is HomeIntent.OpenDocument -> openDocument(intent.uri)
            is HomeIntent.RemoveRecent -> removeRecent(intent.uri)
            is HomeIntent.ClearAllRecents -> clearAllRecents()
            is HomeIntent.AddFolder -> addFolder(intent.treeUri)
            is HomeIntent.RemoveFolder -> removeFolder(intent.treeUri)
            is HomeIntent.SelectFolder -> selectFolder(intent.folder)
            is HomeIntent.OpenPdfFromFolder -> openDocument(intent.uri)
            is HomeIntent.DismissError -> _viewState.update { it.copy(errorMessage = null) }
            is HomeIntent.OpenBackupDialog -> _viewState.update { it.copy(isBackupDialogVisible = true) }
            is HomeIntent.DismissBackupDialog -> _viewState.update { it.copy(isBackupDialogVisible = false) }
            is HomeIntent.ExportBackupToUri -> exportBackup(intent.uri)
            is HomeIntent.InspectBackupUri -> inspectBackup(intent.uri)
            is HomeIntent.SelectRestoreMode -> selectRestoreMode(intent.mode)
            is HomeIntent.ConfirmRestore -> confirmRestore()
            is HomeIntent.DismissRestorePreview -> _viewState.update { it.copy(pendingRestorePreview = null) }
        }
    }

    private fun openDocument(uri: Uri) {
        viewModelScope.launch {
            _sideEffects.send(HomeSideEffect.NavigateToReader(uri))
        }
    }

    private fun removeRecent(uri: String) {
        viewModelScope.launch {
            recentsRepository.removeRecentDocument(uri)
        }
    }

    private fun clearAllRecents() {
        viewModelScope.launch {
            recentsRepository.clearAllRecents()
        }
    }

    private fun addFolder(treeUri: Uri) {
        viewModelScope.launch {
            val result = favoriteFoldersRepository.addFavoriteFolder(treeUri)
            result.onFailure { error ->
                _viewState.update { it.copy(errorMessage = "Could not pin folder: ${error.localizedMessage}") }
            }
        }
    }

    private fun removeFolder(treeUri: String) {
        viewModelScope.launch {
            favoriteFoldersRepository.removeFavoriteFolder(treeUri)
            if (_viewState.value.activeFolder?.treeUri == treeUri) {
                _viewState.update { it.copy(activeFolder = null, activeFolderPdfs = emptyList()) }
            }
        }
    }

    private fun selectFolder(folder: FavoriteFolder?) {
        if (folder == null) {
            _viewState.update { it.copy(activeFolder = null, activeFolderPdfs = emptyList()) }
            return
        }

        viewModelScope.launch {
            _viewState.update { it.copy(activeFolder = folder, isLoading = true) }
            val uri = Uri.parse(folder.treeUri)
            val result = favoriteFoldersRepository.scanFolderPdfs(uri)
            result.fold(
                onSuccess = { pdfs ->
                    _viewState.update { it.copy(activeFolderPdfs = pdfs, isLoading = false) }
                },
                onFailure = { error ->
                    _viewState.update {
                        it.copy(
                            activeFolder = null,
                            activeFolderPdfs = emptyList(),
                            isLoading = false,
                            errorMessage = "Não foi possível acessar a pasta: ${error.localizedMessage ?: "permissão expirada"}"
                        )
                    }
                }
            )
        }
    }

    private fun refresh() {
        _viewState.value.activeFolder?.let { selectFolder(it) }
    }

    private fun exportBackup(uri: Uri) {
        val repo = backupRepository ?: return
        viewModelScope.launch {
            _viewState.update { it.copy(isBackupDialogVisible = false, isBackupInProgress = true) }
            val result = repo.exportBackup(uri.toString())
            result.fold(
                onSuccess = { exportResult ->
                    val s = exportResult.summary
                    _viewState.update { it.copy(isBackupInProgress = false) }
                    _sideEffects.send(
                        HomeSideEffect.ShowToast(
                            message = "Backup exportado: ${s.sessionsCount} sessões (${s.customSpeedsCount} velocidades), " +
                                "${s.recentsCount} recentes, ${s.favoriteFoldersCount} pastas",
                            isLong = true
                        )
                    )
                },
                onFailure = { error ->
                    _viewState.update {
                        it.copy(
                            isBackupInProgress = false,
                            errorMessage = "Falha ao exportar backup: ${error.localizedMessage ?: "erro de E/S"}"
                        )
                    }
                }
            )
        }
    }

    private fun inspectBackup(uri: Uri) {
        val repo = backupRepository ?: return
        viewModelScope.launch {
            _viewState.update { it.copy(isBackupDialogVisible = false, isBackupInProgress = true) }
            when (val parsed = repo.previewBackup(uri.toString())) {
                is BackupParseResult.Success -> {
                    _viewState.update {
                        it.copy(
                            isBackupInProgress = false,
                            pendingRestorePreview = PendingRestorePreview(
                                payload = parsed.payload,
                                summary = parsed.summary,
                                selectedMode = RestoreMode.MERGE
                            )
                        )
                    }
                }
                is BackupParseResult.Failure -> {
                    _viewState.update {
                        it.copy(
                            isBackupInProgress = false,
                            errorMessage = parsed.message
                        )
                    }
                }
            }
        }
    }

    private fun selectRestoreMode(mode: RestoreMode) {
        _viewState.update { state ->
            val current = state.pendingRestorePreview ?: return@update state
            state.copy(pendingRestorePreview = current.copy(selectedMode = mode))
        }
    }

    private fun confirmRestore() {
        val repo = backupRepository ?: return
        val preview = _viewState.value.pendingRestorePreview ?: return
        viewModelScope.launch {
            _viewState.update { it.copy(pendingRestorePreview = null, isBackupInProgress = true) }
            val result = repo.restoreBackup(preview.payload, preview.selectedMode)
            result.fold(
                onSuccess = { restoreResult ->
                    val s = restoreResult.summary
                    val modeLabel = when (restoreResult.mode) {
                        RestoreMode.MERGE -> "Mesclar"
                        RestoreMode.REPLACE -> "Substituir"
                    }
                    _viewState.update { it.copy(isBackupInProgress = false) }
                    onIntent(HomeIntent.Refresh)
                    _sideEffects.send(
                        HomeSideEffect.ShowToast(
                            message = "Backup restaurado ($modeLabel): ${s.sessionsCount} sessões (${s.customSpeedsCount} velocidades), " +
                                "${s.recentsCount} recentes, ${s.favoriteFoldersCount} pastas",
                            isLong = true
                        )
                    )
                },
                onFailure = { error ->
                    _viewState.update {
                        it.copy(
                            isBackupInProgress = false,
                            errorMessage = "Falha ao restaurar backup: ${error.localizedMessage ?: "erro inesperado"}"
                        )
                    }
                }
            )
        }
    }
}
