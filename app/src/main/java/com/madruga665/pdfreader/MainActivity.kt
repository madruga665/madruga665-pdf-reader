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
import com.madruga665.pdfreader.core.backup.AndroidSafBackupFileGateway
import com.madruga665.pdfreader.core.backup.BackupJsonCodec
import com.madruga665.pdfreader.core.backup.BackupRepository
import com.madruga665.pdfreader.core.backup.DefaultBackupRepository
import com.madruga665.pdfreader.core.home.AndroidFavoriteFoldersRepository
import com.madruga665.pdfreader.core.home.DataStoreRecentsRepository
import com.madruga665.pdfreader.core.home.FavoriteFoldersRepository
import com.madruga665.pdfreader.core.home.RecentsRepository
import com.madruga665.pdfreader.core.repository.AndroidPdfDocumentRepository
import com.madruga665.pdfreader.core.storage.DataStoreReadingSessionStore
import com.madruga665.pdfreader.ui.components.AdaptiveReaderScaffold
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

    private lateinit var repository: AndroidPdfDocumentRepository
    private lateinit var sessionStore: DataStoreReadingSessionStore
    private lateinit var recentsRepository: RecentsRepository
    private lateinit var favoriteFoldersRepository: FavoriteFoldersRepository
    private lateinit var backupRepository: BackupRepository

    private val readerViewModel: ReaderViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReaderViewModel(
                    repository = repository,
                    sessionStore = sessionStore,
                    recentsRepository = recentsRepository
                ) as T
            }
        }
    }

    private val homeViewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    recentsRepository = recentsRepository,
                    favoriteFoldersRepository = favoriteFoldersRepository,
                    backupRepository = backupRepository
                ) as T
            }
        }
    }

    private var currentScreenState = mutableStateOf<AppScreen>(AppScreen.Home)

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            currentScreenState.value = AppScreen.Reader(it)
            readerViewModel.onIntent(ReaderIntent.OpenDocument(it))
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
            } catch (_: Exception) {}

            homeViewModel.onIntent(HomeIntent.AddFolder(it))
        }
    }

    private val createBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
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
        repository = AndroidPdfDocumentRepository(applicationContext)
        sessionStore = DataStoreReadingSessionStore(applicationContext)
        recentsRepository = DataStoreRecentsRepository(applicationContext)
        favoriteFoldersRepository = AndroidFavoriteFoldersRepository(applicationContext)
        val appVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
        backupRepository = DefaultBackupRepository(
            sessionStore = sessionStore,
            recentsRepository = recentsRepository,
            favoriteFoldersRepository = favoriteFoldersRepository,
            fileGateway = AndroidSafBackupFileGateway(applicationContext),
            appVersion = appVersion
        )

        // Handle incoming PDF intent from other applications
        intent?.data?.let { uri ->
            currentScreenState.value = AppScreen.Reader(uri)
            readerViewModel.onIntent(ReaderIntent.OpenDocument(uri))
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
                                createBackupLauncher.launch(BackupJsonCodec.suggestedBackupFileName())
                            },
                            onImportBackupLauncher = {
                                openBackupLauncher.launch(
                                    arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")
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
                            repository = repository,
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
            }
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
