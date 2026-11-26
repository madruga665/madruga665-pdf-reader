package com.madruga665.pdfreader.ui.home

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.ui.components.BackupActionsDialog
import com.madruga665.pdfreader.ui.components.FavoriteFolderCard
import com.madruga665.pdfreader.ui.components.FolderContentsDialog
import com.madruga665.pdfreader.ui.components.HomeEmptyState
import com.madruga665.pdfreader.ui.components.NeobrutalistBadge
import com.madruga665.pdfreader.ui.components.NeobrutalistButton
import com.madruga665.pdfreader.ui.components.NeobrutalistIconBox
import com.madruga665.pdfreader.ui.components.NeobrutalistIconButton
import com.madruga665.pdfreader.ui.components.NeobrutalistSnackbarHost
import com.madruga665.pdfreader.ui.components.RecentDocumentCard
import com.madruga665.pdfreader.ui.components.RestorePreviewDialog
import com.madruga665.pdfreader.ui.theme.NeobrutalismMetrics
import com.madruga665.pdfreader.ui.theme.NeobrutalismTheme
import com.madruga665.pdfreader.ui.theme.neobrutalistGridBackground

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
    val colors = NeobrutalismTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    val brandIconShape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusSmall)

    LaunchedEffect(viewState.errorMessage) {
        viewState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            onIntent(HomeIntent.DismissError)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NeobrutalistIconBox(
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                containerColor = colors.accentYellow,
                                boxSize = 36.dp,
                                iconSize = 20.dp,
                                shape = brandIconShape
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "PDF Reader",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            NeobrutalistIconButton(
                                onClick = { onIntent(HomeIntent.OpenBackupDialog) },
                                containerColor = colors.surface,
                                contentColor = colors.onSurface
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsBackupRestore,
                                    contentDescription = "Backup e Restauração",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            NeobrutalistIconButton(
                                onClick = onPinFolderLauncher,
                                containerColor = colors.surface,
                                contentColor = colors.onSurface
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = "Pin folder",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            NeobrutalistIconButton(
                                onClick = onOpenDocumentLauncher,
                                containerColor = colors.accentYellow,
                                contentColor = colors.onAccent
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Open PDF",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surface,
                        titleContentColor = colors.onSurface,
                        actionIconContentColor = colors.onSurface
                    )
                )
                HorizontalDivider(
                    thickness = NeobrutalismMetrics.BorderWidth,
                    color = colors.border
                )
            }
        },
        floatingActionButton = {
            if (!viewState.isEmpty) {
                if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                    NeobrutalistIconButton(
                        onClick = onOpenDocumentLauncher,
                        containerColor = colors.accentYellow,
                        contentColor = colors.onAccent,
                        size = 56.dp,
                        borderWidth = NeobrutalismMetrics.BorderWidth,
                        shadowOffset = NeobrutalismMetrics.ShadowOffset,
                        shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusLarge)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Open PDF",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                } else {
                    NeobrutalistButton(
                        onClick = onOpenDocumentLauncher,
                        containerColor = colors.accentYellow,
                        contentColor = colors.onAccent,
                        borderWidth = NeobrutalismMetrics.BorderWidth,
                        shadowOffset = NeobrutalismMetrics.ShadowOffset,
                        shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusLarge),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open PDF",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        },
        snackbarHost = { NeobrutalistSnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.background)
                .neobrutalistGridBackground(colors.gridLine)
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
                    onIntent = onIntent
                )
            } else {
                ExpandedHomeContent(
                    viewState = viewState,
                    onIntent = onIntent
                )
            }

            if (viewState.isBackupInProgress) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = colors.accentPurple,
                        trackColor = colors.progressTrack
                    )
                    HorizontalDivider(
                        thickness = NeobrutalismMetrics.BorderWidthCompact,
                        color = colors.border
                    )
                }
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
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        if (viewState.recentDocuments.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Jump Back In",
                    count = viewState.recentDocuments.size,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp)
                )
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 20.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewState.recentDocuments, key = { it.id.ifBlank { it.uri } }) { recent ->
                        RecentDocumentCard(
                            document = recent,
                            onClick = {
                                onIntent(HomeIntent.OpenDocument(Uri.parse(recent.uri)))
                            },
                            onRemoveClick = {
                                onIntent(HomeIntent.RemoveRecent(recent.uri))
                            },
                            onRelinkClick = if (!recent.isAvailable && recent.id.isNotBlank()) {
                                { onIntent(HomeIntent.RequestRelinkPdf(recent.id)) }
                            } else {
                                null
                            },
                            modifier = Modifier.width(264.dp)
                        )
                    }
                }
            }
        }

        if (viewState.favoriteFolders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(22.dp))
                SectionHeader(
                    title = "Favorite Folders",
                    count = viewState.favoriteFolders.size,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
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
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(end = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(viewState.recentDocuments, key = { it.id.ifBlank { it.uri } }) { recent ->
                        RecentDocumentCard(
                            document = recent,
                            onClick = {
                                onIntent(HomeIntent.OpenDocument(Uri.parse(recent.uri)))
                            },
                            onRemoveClick = {
                                onIntent(HomeIntent.RemoveRecent(recent.uri))
                            },
                            onRelinkClick = if (!recent.isAvailable && recent.id.isNotBlank()) {
                                { onIntent(HomeIntent.RequestRelinkPdf(recent.id)) }
                            } else {
                                null
                            },
                            modifier = Modifier.width(284.dp)
                        )
                    }
                }
            }
        }

        if (viewState.favoriteFolders.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(
                    title = "Favorite Folders",
                    count = viewState.favoriteFolders.size,
                    modifier = Modifier.padding(bottom = 4.dp)
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
    val colors = NeobrutalismTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface
        )

        NeobrutalistBadge(
            text = "$count",
            containerColor = colors.accentBlue,
            contentColor = colors.onAccent,
            borderWidth = NeobrutalismMetrics.BorderWidthCompact,
            shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact
        )
    }
}
