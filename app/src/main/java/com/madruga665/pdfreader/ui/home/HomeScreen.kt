package com.madruga665.pdfreader.ui.home

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.ui.components.BackupActionsDialog
import com.madruga665.pdfreader.ui.components.FavoriteFolderCard
import com.madruga665.pdfreader.ui.components.FolderContentsDialog
import com.madruga665.pdfreader.ui.components.HomeEmptyState
import com.madruga665.pdfreader.ui.components.RecentDocumentCard
import com.madruga665.pdfreader.ui.components.RestorePreviewDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewState: HomeViewState,
    onIntent: (HomeIntent) -> Unit,
    windowWidthSizeClass: WindowWidthSizeClass,
    onOpenDocumentLauncher: () -> Unit,
    onPinFolderLauncher: () -> Unit,
    onExportBackupLauncher: () -> Unit = {},
    onImportBackupLauncher: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewState.errorMessage) {
        viewState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            onIntent(HomeIntent.DismissError)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "PDF Reader",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onIntent(HomeIntent.OpenBackupDialog) }) {
                        Icon(
                            imageVector = Icons.Default.SettingsBackupRestore,
                            contentDescription = "Backup e Restauração"
                        )
                    }
                    IconButton(onClick = onPinFolderLauncher) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "Pin folder"
                        )
                    }
                    IconButton(onClick = onOpenDocumentLauncher) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Open PDF"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!viewState.isEmpty) {
                if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                    FloatingActionButton(
                        onClick = onOpenDocumentLauncher,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Open PDF"
                        )
                    }
                } else {
                    ExtendedFloatingActionButton(
                        onClick = onOpenDocumentLauncher,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )
                        },
                        text = { Text("Open PDF") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (viewState.isEmpty) {
                HomeEmptyState(
                    onOpenPdfClick = onOpenDocumentLauncher,
                    onAddFolderClick = onPinFolderLauncher,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                CompactHomeContent(
                    viewState = viewState,
                    onIntent = onIntent,
                    onPinFolderLauncher = onPinFolderLauncher
                )
            } else {
                ExpandedHomeContent(
                    viewState = viewState,
                    onIntent = onIntent,
                    onPinFolderLauncher = onPinFolderLauncher
                )
            }

            if (viewState.isBackupInProgress) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            // Dialog for showing PDFs within active pinned folder
            viewState.activeFolder?.let { folder ->
                FolderContentsDialog(
                    folder = folder,
                    pdfList = viewState.activeFolderPdfs,
                    isLoading = viewState.isLoading,
                    onPdfClick = { uri ->
                        onIntent(HomeIntent.OpenPdfFromFolder(uri))
                    },
                    onDismissRequest = {
                        onIntent(HomeIntent.SelectFolder(null))
                    },
                    onUnpinFolder = {
                        onIntent(HomeIntent.RemoveFolder(folder.treeUri))
                    }
                )
            }

            if (viewState.isBackupDialogVisible) {
                BackupActionsDialog(
                    onExportClick = {
                        onIntent(HomeIntent.DismissBackupDialog)
                        onExportBackupLauncher()
                    },
                    onImportClick = {
                        onIntent(HomeIntent.DismissBackupDialog)
                        onImportBackupLauncher()
                    },
                    onDismiss = {
                        onIntent(HomeIntent.DismissBackupDialog)
                    }
                )
            }

            viewState.pendingRestorePreview?.let { preview ->
                RestorePreviewDialog(
                    preview = preview,
                    onModeSelected = { mode ->
                        onIntent(HomeIntent.SelectRestoreMode(mode))
                    },
                    onConfirm = {
                        onIntent(HomeIntent.ConfirmRestore)
                    },
                    onDismiss = {
                        onIntent(HomeIntent.DismissRestorePreview)
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactHomeContent(
    viewState: HomeViewState,
    onIntent: (HomeIntent) -> Unit,
    onPinFolderLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        if (viewState.recentDocuments.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Jump Back In",
                    count = viewState.recentDocuments.size,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewState.recentDocuments, key = { it.uri }) { recent ->
                        RecentDocumentCard(
                            document = recent,
                            onClick = {
                                onIntent(HomeIntent.OpenDocument(Uri.parse(recent.uri)))
                            },
                            onRemoveClick = {
                                onIntent(HomeIntent.RemoveRecent(recent.uri))
                            },
                            modifier = Modifier.width(260.dp)
                        )
                    }
                }
            }
        }

        if (viewState.favoriteFolders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(
                    title = "Favorite Folders",
                    count = viewState.favoriteFolders.size,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }

            items(viewState.favoriteFolders, key = { it.treeUri }) { folder ->
                FavoriteFolderCard(
                    folder = folder,
                    onClick = {
                        onIntent(HomeIntent.SelectFolder(folder))
                    },
                    onRemoveClick = {
                        onIntent(HomeIntent.RemoveFolder(folder.treeUri))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandedHomeContent(
    viewState: HomeViewState,
    onIntent: (HomeIntent) -> Unit,
    onPinFolderLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 320.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (viewState.recentDocuments.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Jump Back In",
                    count = viewState.recentDocuments.size,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(viewState.recentDocuments, key = { it.uri }) { recent ->
                        RecentDocumentCard(
                            document = recent,
                            onClick = {
                                onIntent(HomeIntent.OpenDocument(Uri.parse(recent.uri)))
                            },
                            onRemoveClick = {
                                onIntent(HomeIntent.RemoveRecent(recent.uri))
                            },
                            modifier = Modifier.width(280.dp)
                        )
                    }
                }
            }
        }

        if (viewState.favoriteFolders.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Favorite Folders",
                    count = viewState.favoriteFolders.size,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(viewState.favoriteFolders, key = { it.treeUri }) { folder ->
                FavoriteFolderCard(
                    folder = folder,
                    onClick = {
                        onIntent(HomeIntent.SelectFolder(folder))
                    },
                    onRemoveClick = {
                        onIntent(HomeIntent.RemoveFolder(folder.treeUri))
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
