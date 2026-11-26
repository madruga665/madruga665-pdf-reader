package com.madruga665.pdfreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.core.engine.AutoScrollEngine
import com.madruga665.pdfreader.core.repository.PdfDocumentRepository
import com.madruga665.pdfreader.ui.reader.ReaderIntent
import com.madruga665.pdfreader.ui.reader.ReaderViewState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveReaderScaffold(
    viewState: ReaderViewState,
    windowWidthSizeClass: WindowWidthSizeClass,
    repository: PdfDocumentRepository,
    autoScrollEngine: AutoScrollEngine,
    onIntent: (ReaderIntent) -> Unit,
    onOpenFilePicker: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isExpanded = windowWidthSizeClass == WindowWidthSizeClass.Expanded

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to library"
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = viewState.document?.fileName ?: "PDF Reader",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )
                },
                actions = {
                    if (viewState.totalPages > 0) {
                        Text(
                            text = "Page ${viewState.currentPageIndex + 1} of ${viewState.totalPages}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                    IconButton(onClick = onOpenFilePicker) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Open PDF")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (viewState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (viewState.document != null) {
                if (isExpanded) {
                    // Tablet / Desktop layout with Side Rail
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight(),
                            header = {
                                IconButton(onClick = { onIntent(ReaderIntent.ToggleAutoScroll) }) {
                                    Icon(
                                        imageVector = if (viewState.isAutoScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Toggle Auto-Scroll"
                                    )
                                }
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .widthIn(max = 240.dp)
                            ) {
                                SpeedSelectorControl(
                                    currentSpeedPxPerSec = viewState.currentSpeedPxPerSec,
                                    activePreset = viewState.activePreset,
                                    onSpeedChange = { onIntent(ReaderIntent.SetSpeed(it)) },
                                    onPresetSelect = { onIntent(ReaderIntent.SelectPreset(it)) },
                                    savedSpeedPxPerSec = viewState.documentSavedSpeedPxPerSec,
                                    onSaveCurrentSpeedAsDefault = { onIntent(ReaderIntent.SaveCurrentSpeedAsDocumentDefault) },
                                    onResetSpeedToDefault = { onIntent(ReaderIntent.ResetDocumentSpeedToDefault) }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            PdfDocumentViewport(
                                document = viewState.document,
                                repository = repository,
                                autoScrollEngine = autoScrollEngine,
                                isAutoScrolling = viewState.isAutoScrolling,
                                onPageChanged = { onIntent(ReaderIntent.PageChanged(it)) },
                                onScrollProgressChanged = { onIntent(ReaderIntent.ScrollProgressChanged(it)) },
                                onTap = { onIntent(ReaderIntent.ToggleOverlayVisibility) },
                                onEndOfDocument = { onIntent(ReaderIntent.EndOfDocumentReached) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    // Phone / Compact layout
                    Box(modifier = Modifier.fillMaxSize()) {
                        PdfDocumentViewport(
                            document = viewState.document,
                            repository = repository,
                            autoScrollEngine = autoScrollEngine,
                            isAutoScrolling = viewState.isAutoScrolling,
                            onPageChanged = { onIntent(ReaderIntent.PageChanged(it)) },
                            onScrollProgressChanged = { onIntent(ReaderIntent.ScrollProgressChanged(it)) },
                            onTap = { onIntent(ReaderIntent.ToggleOverlayVisibility) },
                            onEndOfDocument = { onIntent(ReaderIntent.EndOfDocumentReached) }
                        )

                        FloatingAutoScrollOverlay(
                            isVisible = viewState.isOverlayControlsVisible,
                            isAutoScrolling = viewState.isAutoScrolling,
                            currentSpeedPxPerSec = viewState.currentSpeedPxPerSec,
                            activePreset = viewState.activePreset,
                            onToggleAutoScroll = { onIntent(ReaderIntent.ToggleAutoScroll) },
                            onSpeedChange = { onIntent(ReaderIntent.SetSpeed(it)) },
                            onPresetSelect = { onIntent(ReaderIntent.SelectPreset(it)) },
                            savedSpeedPxPerSec = viewState.documentSavedSpeedPxPerSec,
                            onSaveCurrentSpeedAsDefault = { onIntent(ReaderIntent.SaveCurrentSpeedAsDocumentDefault) },
                            onResetSpeedToDefault = { onIntent(ReaderIntent.ResetDocumentSpeedToDefault) },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No PDF document opened",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap the folder icon above to open a PDF",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            ErrorDialog(
                errorState = viewState.error,
                onDismiss = {
                    onIntent(ReaderIntent.DismissError)
                    if (viewState.document == null) {
                        onNavigateBack?.invoke()
                    }
                },
                onRetry = onOpenFilePicker
            )
        }
    }
}
