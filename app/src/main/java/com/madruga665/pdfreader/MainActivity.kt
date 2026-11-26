package com.madruga665.pdfreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.madruga665.pdfreader.core.backup.SqliteBackupRepository
import com.madruga665.pdfreader.core.model.ReaderErrorState
import com.madruga665.pdfreader.core.repository.AndroidPdfDocumentRepository
import com.madruga665.pdfreader.core.storage.PdfReaderAppStorage
import com.madruga665.pdfreader.ui.components.AdaptiveReaderScaffold
import com.madruga665.pdfreader.ui.components.RelinkConflictDialog
import com.madruga665.pdfreader.ui.home.HomeIntent
import com.madruga665.pdfreader.ui.home.HomeScreen
import com.madruga665.pdfreader.ui.home.HomeSideEffect
import com.madruga665.pdfreader.ui.home.HomeViewModel
import com.madruga665.pdfreader.ui.navigation.AppScreen
import com.madruga665.pdfreader.ui.reader.ReaderIntent
import com.madruga665.pdfreader.ui.reader.ReaderSideEffect
import com.madruga665.pdfreader.ui.reader.ReaderViewModel
import com.madruga665.pdfreader.ui.theme.PdfReaderTheme

class MainActivity : ComponentActivity() {

    private lateinit var appStorage: PdfReaderAppStorage

    private var pendingRelinkDocumentId: String? = null
    private var hasCompletedInitialResume = false

    private val readerViewModel: ReaderViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReaderViewModel(
                    repository = AndroidPdfDocumentRepository(applicationContext),
                    sessionStore = appStorage.documentStore,
                    recentsRepository = appStorage.recentsRepository
                ) as T
            }
        }
    }

    private val homeViewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    recentsRepository = appStorage.recentsRepository,
                    favoriteFoldersRepository = appStorage.favoriteFoldersRepository,
                    backupRepository = appStorage.backupRepository,
                    documentStore = appStorage.documentStore,
                    availabilityChecker = appStorage.availabilityChecker
                ) as T
            }
        }
    }

    private val currentScreenState = mutableStateOf<AppScreen>(AppScreen.Home)

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            currentScreenState.value = AppScreen.Reader(it)
            readerViewModel.onIntent(ReaderIntent.OpenDocument(it))
        }
    }

    private val relinkPdfLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val docId = pendingRelinkDocumentId
        pendingRelinkDocumentId = null
        if (docId != null && uri != null) {
            homeViewModel.onIntent(HomeIntent.CompleteRelinkPdf(docId, uri))
        }
    }

    private val openFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            homeViewModel.onIntent(HomeIntent.AddFolder(it))
        }
    }

    private val createBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let {
            homeViewModel.onIntent(HomeIntent.ExportBackupToUri(it))
        }
    }

    private val openBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            homeViewModel.onIntent(HomeIntent.InspectBackupUri(it))
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appStorage = PdfReaderAppStorage.getInstance(applicationContext)
        pendingRelinkDocumentId = savedInstanceState
            ?.getString(KEY_PENDING_RELINK_DOC_ID)
            ?.takeIf { it.isNotBlank() }

        val restoredReaderUri = savedInstanceState
            ?.getString(KEY_ACTIVE_READER_URI)
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)

        if (restoredReaderUri != null) {
            currentScreenState.value = AppScreen.Reader(restoredReaderUri)
            val readerState = readerViewModel.viewState.value
            if (readerState.document == null && !readerState.isLoading && readerState.error is ReaderErrorState.None) {
                readerViewModel.onIntent(ReaderIntent.OpenDocument(restoredReaderUri))
            }
        } else if (savedInstanceState == null) {
            intent?.data?.let { uri ->
                currentScreenState.value = AppScreen.Reader(uri)
                readerViewModel.onIntent(ReaderIntent.OpenDocument(uri))
            }
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            var currentScreen by remember { currentScreenState }
            val homeViewState by homeViewModel.viewState.collectAsState()
            val readerViewState by readerViewModel.viewState.collectAsState()

            LaunchedEffect(Unit) {
                homeViewModel.sideEffects.collect { effect ->
                    when (effect) {
                        is HomeSideEffect.NavigateToReader -> {
                            currentScreen = AppScreen.Reader(effect.uri)
                            readerViewModel.onIntent(ReaderIntent.OpenDocument(effect.uri))
                        }

                        is HomeSideEffect.ShowToast -> {
                            val duration = if (effect.isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                            Toast.makeText(this@MainActivity, effect.message, duration).show()
                        }

                        is HomeSideEffect.LaunchRelinkPdfPicker -> {
                            pendingRelinkDocumentId = effect.documentId
                            relinkPdfLauncher.launch(arrayOf("application/pdf"))
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                readerViewModel.sideEffects.collect { effect ->
                    when (effect) {
                        is ReaderSideEffect.ShowToast -> {
                            Toast.makeText(this@MainActivity, effect.message, Toast.LENGTH_SHORT).show()
                        }

                        is ReaderSideEffect.SpeedSavedForDocument -> {
                            Toast.makeText(
                                this@MainActivity,
                                "Velocidade padrão salva: ${effect.speedPxPerSec.toInt()} px/s",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is ReaderSideEffect.SpeedResetToDefault -> {
                            Toast.makeText(
                                this@MainActivity,
                                "Velocidade restaurada para o padrão geral",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is ReaderSideEffect.LaunchRelinkPdfPicker -> {
                            pendingRelinkDocumentId = effect.documentId
                            relinkPdfLauncher.launch(arrayOf("application/pdf"))
                        }

                        else -> {}
                    }
                }
            }

            PdfReaderTheme {
                when (currentScreen) {
                    is AppScreen.Home -> {
                        HomeScreen(
                            viewState = homeViewState,
                            onIntent = homeViewModel::onIntent,
                            windowWidthSizeClass = windowSizeClass.widthSizeClass,
                            onOpenDocumentLauncher = {
                                openDocumentLauncher.launch(arrayOf("application/pdf"))
                            },
                            onPinFolderLauncher = {
                                openFolderLauncher.launch(null)
                            },
                            onExportBackupLauncher = {
                                createBackupLauncher.launch(SqliteBackupRepository.suggestedBackupFileName())
                            },
                            onImportBackupLauncher = {
                                openBackupLauncher.launch(
                                    arrayOf(
                                        "application/x-sqlite3",
                                        "application/vnd.sqlite3",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            }
                        )
                    }

                    is AppScreen.Reader -> {
                        BackHandler {
                            currentScreen = AppScreen.Home
                            homeViewModel.onIntent(HomeIntent.Refresh)
                        }

                        AdaptiveReaderScaffold(
                            viewState = readerViewState,
                            windowWidthSizeClass = windowSizeClass.widthSizeClass,
                            repository = readerViewModel.repository,
                            autoScrollEngine = readerViewModel.autoScrollEngine,
                            onIntent = readerViewModel::onIntent,
                            onOpenFilePicker = {
                                openDocumentLauncher.launch(arrayOf("application/pdf"))
                            },
                            onNavigateBack = {
                                currentScreen = AppScreen.Home
                                homeViewModel.onIntent(HomeIntent.Refresh)
                            }
                        )
                    }
                }

                homeViewState.relinkConflict?.let { conflict ->
                    RelinkConflictDialog(
                        conflict = conflict,
                        onConsolidate = {
                            homeViewModel.onIntent(HomeIntent.ConfirmConsolidateRelinkConflict)
                        },
                        onOpenExisting = {
                            homeViewModel.onIntent(HomeIntent.OpenExistingFromRelinkConflict)
                        },
                        onDismiss = {
                            homeViewModel.onIntent(HomeIntent.DismissRelinkConflict)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasCompletedInitialResume && currentScreenState.value is AppScreen.Home) {
            homeViewModel.onIntent(HomeIntent.Refresh)
        }
        hasCompletedInitialResume = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val screen = currentScreenState.value
        if (screen is AppScreen.Reader) {
            outState.putString(KEY_ACTIVE_READER_URI, screen.uri.toString())
        }
        pendingRelinkDocumentId?.takeIf { it.isNotBlank() }?.let {
            outState.putString(KEY_PENDING_RELINK_DOC_ID, it)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { uri ->
            currentScreenState.value = AppScreen.Reader(uri)
            readerViewModel.onIntent(ReaderIntent.OpenDocument(uri))
        }
    }

    companion object {
        private const val KEY_ACTIVE_READER_URI = "active_reader_uri"
        private const val KEY_PENDING_RELINK_DOC_ID = "pending_relink_document_id"
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (currentScreenState.value is AppScreen.Reader) {
            return when (keyCode) {
                KeyEvent.KEYCODE_SPACE -> {
                    readerViewModel.onIntent(ReaderIntent.ToggleAutoScroll)
                    true
                }

                KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_EQUALS -> {
                    val currentSpeed = readerViewModel.viewState.value.currentSpeedPxPerSec
                    readerViewModel.onIntent(ReaderIntent.SetSpeed(currentSpeed + 15f))
                    true
                }

                KeyEvent.KEYCODE_MINUS -> {
                    val currentSpeed = readerViewModel.viewState.value.currentSpeedPxPerSec
                    readerViewModel.onIntent(ReaderIntent.SetSpeed(currentSpeed - 15f))
                    true
                }

                else -> super.onKeyDown(keyCode, event)
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
