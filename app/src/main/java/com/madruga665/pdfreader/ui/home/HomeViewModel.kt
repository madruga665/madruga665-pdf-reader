package com.madruga665.pdfreader.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madruga665.pdfreader.core.backup.BackupParseResult
import com.madruga665.pdfreader.core.backup.BackupRepository
import com.madruga665.pdfreader.core.backup.RestoreMode
import com.madruga665.pdfreader.core.documents.DocumentAvailabilityChecker
import com.madruga665.pdfreader.core.documents.DocumentRelinkPolicy
import com.madruga665.pdfreader.core.documents.RelinkOutcome
import com.madruga665.pdfreader.core.home.FavoriteFoldersRepository
import com.madruga665.pdfreader.core.home.RecentsRepository
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.storage.PdfDocumentStore
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
    private val backupRepository: BackupRepository? = null,
    private val documentStore: PdfDocumentStore? = null,
    private val availabilityChecker: DocumentAvailabilityChecker? = null
) : ViewModel() {

    private val _viewState = MutableStateFlow(HomeViewState(isLoading = true))
    val viewState: StateFlow<HomeViewState> = _viewState.asStateFlow()

    private val _sideEffects = Channel<HomeSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    private var hasRunInitialAvailabilityCheck = false

    init {
        viewModelScope.launch {
            recentsRepository.getRecentDocuments().collect { recents ->
                _viewState.update { it.copy(recentDocuments = recents, isLoading = false) }
                if (!hasRunInitialAvailabilityCheck && recents.isNotEmpty() && availabilityChecker != null) {
                    hasRunInitialAvailabilityCheck = true
                    revalidateRecentsAvailability(recents)
                }
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
            is HomeIntent.RequestRelinkPdf -> requestRelinkPdf(intent.documentId)
            is HomeIntent.CompleteRelinkPdf -> completeRelinkPdf(intent.documentId, intent.newUri)
            is HomeIntent.ConfirmConsolidateRelinkConflict -> confirmConsolidateRelinkConflict()
            is HomeIntent.OpenExistingFromRelinkConflict -> openExistingFromRelinkConflict()
            is HomeIntent.DismissRelinkConflict -> _viewState.update { it.copy(relinkConflict = null) }
        }
    }

    private fun openDocument(uri: Uri) {
        viewModelScope.launch {
            val uriString = uri.toString()
            val matchingRecent = _viewState.value.recentDocuments.firstOrNull { it.uri == uriString }
            if (matchingRecent != null && !matchingRecent.isAvailable) {
                if (matchingRecent.id.isNotBlank()) {
                    _sideEffects.send(HomeSideEffect.LaunchRelinkPdfPicker(matchingRecent.id))
                } else {
                    _viewState.update {
                        it.copy(errorMessage = "Arquivo indisponível. Use Restaurar PDF para localizar o arquivo.")
                    }
                }
                return@launch
            }

            if (availabilityChecker != null && matchingRecent != null) {
                val readable = availabilityChecker.isReadable(uriString)
                if (!readable) {
                    recentsRepository.updateAvailability(uriString, false)
                    _viewState.update {
                        it.copy(errorMessage = "Arquivo indisponível. Toque em \"Restaurar PDF\" para localizar o arquivo movido ou renomeado.")
                    }
                    return@launch
                }
            }

            _sideEffects.send(HomeSideEffect.NavigateToReader(uri))
        }
    }

    private fun requestRelinkPdf(documentId: String) {
        if (documentId.isBlank()) return
        viewModelScope.launch {
            _sideEffects.send(HomeSideEffect.LaunchRelinkPdfPicker(documentId))
        }
    }

    private fun completeRelinkPdf(documentId: String, newUri: Uri) {
        val store = documentStore ?: return
        val checker = availabilityChecker ?: return
        viewModelScope.launch {
            val target = store.getById(documentId)
            if (target == null) {
                _viewState.update { it.copy(errorMessage = "Registro do PDF não encontrado no banco local.") }
                return@launch
            }

            val newUriString = newUri.toString()
            checker.takePersistableReadPermission(newUriString)

            val metadata = checker.inspectPdf(newUriString).getOrElse { error ->
                _viewState.update {
                    it.copy(
                        errorMessage = "Não foi possível restaurar o PDF: ${error.localizedMessage ?: "arquivo inválido ou corrompido"}"
                    )
                }
                return@launch
            }

            val existingByNewUri = store.getByUri(metadata.uri)
            when (
                val outcome = DocumentRelinkPolicy.evaluateRelink(
                    target = target,
                    newMetadata = metadata,
                    existingByNewUri = existingByNewUri
                )
            ) {
                is RelinkOutcome.InvalidTarget -> {
                    _viewState.update { it.copy(errorMessage = outcome.message) }
                }

                is RelinkOutcome.UriConflict -> {
                    _viewState.update {
                        it.copy(
                            relinkConflict = RelinkConflictState(
                                targetRecord = outcome.targetRecord,
                                conflictingRecord = outcome.conflictingRecord,
                                newUri = outcome.newMetadata.uri,
                                newFileName = outcome.newMetadata.fileName,
                                newPageCount = outcome.newMetadata.pageCount
                            )
                        )
                    }
                }

                is RelinkOutcome.Relinked -> {
                    val persisted = store.relinkDocument(
                        targetDocumentId = target.id,
                        newUri = metadata.uri,
                        newFileName = metadata.fileName,
                        newPageCount = metadata.pageCount,
                        consolidateConflictingDocumentId = null
                    )
                    persisted.fold(
                        onSuccess = {
                            _sideEffects.send(HomeSideEffect.NavigateToReader(newUri))
                        },
                        onFailure = { err ->
                            _viewState.update {
                                it.copy(
                                    errorMessage = "Falha ao atualizar vínculo do PDF: ${err.localizedMessage ?: "erro inesperado"}"
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    private fun confirmConsolidateRelinkConflict() {
        val store = documentStore ?: return
        val conflict = _viewState.value.relinkConflict ?: return
        viewModelScope.launch {
            _viewState.update { it.copy(relinkConflict = null) }
            val result = store.relinkDocument(
                targetDocumentId = conflict.targetRecord.id,
                newUri = conflict.newUri,
                newFileName = conflict.newFileName,
                newPageCount = conflict.newPageCount,
                consolidateConflictingDocumentId = conflict.conflictingRecord.id
            )
            result.fold(
                onSuccess = {
                    _sideEffects.send(HomeSideEffect.NavigateToReader(Uri.parse(conflict.newUri)))
                },
                onFailure = { error ->
                    _viewState.update {
                        it.copy(
                            errorMessage = "Não foi possível consolidar o PDF: ${error.localizedMessage ?: "erro inesperado"}"
                        )
                    }
                }
            )
        }
    }

    private fun openExistingFromRelinkConflict() {
        val conflict = _viewState.value.relinkConflict ?: return
        viewModelScope.launch {
            _viewState.update { it.copy(relinkConflict = null) }
            _sideEffects.send(HomeSideEffect.NavigateToReader(Uri.parse(conflict.conflictingRecord.documentUri)))
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
        if (availabilityChecker != null) {
            viewModelScope.launch {
                val recents = _viewState.value.recentDocuments
                revalidateRecentsAvailability(recents)
            }
        }
    }

    private suspend fun revalidateRecentsAvailability(recents: List<com.madruga665.pdfreader.core.model.RecentDocument>) {
        val checker = availabilityChecker ?: return
        if (recents.isEmpty()) return
        val changes = linkedMapOf<String, Boolean>()
        for (doc in recents) {
            val readable = checker.isReadable(doc.uri)
            if (readable != doc.isAvailable) {
                changes[doc.uri] = readable
            }
        }
        if (changes.isNotEmpty()) {
            recentsRepository.updateAvailabilityBatch(changes)
        }
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
                            message = "Backup SQLite exportado: ${s.sessionsCount} PDFs (${s.customSpeedsCount} velocidades), " +
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
                    // Revalidate availability of restored documents immediately (RF-12 / CA-12)
                    if (availabilityChecker != null && documentStore != null) {
                        val allDocs = documentStore.getAllDocumentsSnapshot()
                        val updates = linkedMapOf<String, Boolean>()
                        for (doc in allDocs) {
                            val readable = availabilityChecker.isReadable(doc.documentUri)
                            if (readable != doc.isAvailable) {
                                updates[doc.documentUri] = readable
                            }
                        }
                        if (updates.isNotEmpty()) {
                            documentStore.updateAvailabilityBatch(updates)
                        }
                    } else {
                        onIntent(HomeIntent.Refresh)
                    }
                    _sideEffects.send(
                        HomeSideEffect.ShowToast(
                            message = "Backup SQLite restaurado ($modeLabel): ${s.sessionsCount} PDFs (${s.customSpeedsCount} velocidades), " +
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
