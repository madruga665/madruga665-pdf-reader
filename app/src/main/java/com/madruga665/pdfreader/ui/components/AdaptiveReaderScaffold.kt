package com.madruga665.pdfreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.core.engine.AutoScrollEngine
import com.madruga665.pdfreader.core.repository.PdfDocumentRepository
import com.madruga665.pdfreader.ui.reader.ReaderIntent
import com.madruga665.pdfreader.ui.reader.ReaderViewState
import com.madruga665.pdfreader.ui.theme.NeobrutalismMetrics
import com.madruga665.pdfreader.ui.theme.NeobrutalismTheme

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
    val colors = NeobrutalismTheme.colors
    val isExpanded = windowWidthSizeClass == WindowWidthSizeClass.Expanded

    Scaffold(
        containerColor = colors.readerViewportBackground,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    navigationIcon = {
                        if (onNavigateBack != null) {
                            NeobrutalistIconButton(
                                onClick = onNavigateBack,
                                containerColor = colors.surface,
                                contentColor = colors.onSurface,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to library",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    title = {
                        Text(
                            text = viewState.document?.fileName ?: "PDF Reader",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            if (viewState.totalPages > 0) {
                                NeobrutalistBadge(
                                    text = "Page ${viewState.currentPageIndex + 1} of ${viewState.totalPages}",
                                    containerColor = colors.accentBlue,
                                    contentColor = colors.onAccent
                                )
                            }
                            NeobrutalistIconButton(
                                onClick = onOpenFilePicker,
                                containerColor = colors.accentYellow,
                                contentColor = colors.onAccent
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileOpen,
                                    contentDescription = "Open PDF",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surface,
                        titleContentColor = colors.onSurface,
                        navigationIconContentColor = colors.onSurface,
                        actionIconContentColor = colors.onSurface
                    )
                )
                HorizontalDivider(
                    thickness = NeobrutalismMetrics.BorderWidth,
                    color = colors.border
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.readerViewportBackground)
        ) {
            if (viewState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    NeobrutalistCard(
                        modifier = Modifier
                            .padding(24.dp)
                            .widthIn(max = 140.dp),
                        containerColor = colors.surface,
                        shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusLarge)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else if (viewState.document != null) {
                if (isExpanded) {
                    // Tablet / Desktop layout with dedicated opaque Neo-Brutalism side panel
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .width(NeobrutalismMetrics.ExpandedReaderRailWidth)
                                .fillMaxHeight()
                                .background(colors.surface)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            NeobrutalistIconButton(
                                onClick = { onIntent(ReaderIntent.ToggleAutoScroll) },
                                containerColor = if (viewState.isAutoScrolling) colors.accentOrange else colors.accentYellow,
                                contentColor = colors.onAccent,
                                size = 56.dp,
                                borderWidth = NeobrutalismMetrics.BorderWidth,
                                shadowOffset = NeobrutalismMetrics.ShadowOffset,
                                shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusMedium)
                            ) {
                                Icon(
                                    imageVector = if (viewState.isAutoScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Toggle Auto-Scroll",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

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

                        // Solid vertical divider separating controls from PDF viewport
                        Box(
                            modifier = Modifier
                                .width(NeobrutalismMetrics.BorderWidth)
                                .fillMaxHeight()
                                .background(colors.border)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(colors.readerViewportBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            PdfDocumentViewport(
                                document = viewState.document,
                                repository = repository,
                                autoScrollEngine = autoScrollEngine,
                                isAutoScrolling = viewState.isAutoScrolling,
                                initialPageIndex = viewState.currentPageIndex,
                                initialZoomScale = viewState.zoomScale,
                                onPageChanged = { onIntent(ReaderIntent.PageChanged(it)) },
                                onScrollProgressChanged = { onIntent(ReaderIntent.ScrollProgressChanged(it)) },
                                onZoomScaleChanged = { onIntent(ReaderIntent.SetZoomScale(it)) },
                                onTap = { onIntent(ReaderIntent.ToggleOverlayVisibility) },
                                onEndOfDocument = { onIntent(ReaderIntent.EndOfDocumentReached) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    // Phone / Compact layout
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.readerViewportBackground)
                    ) {
                        PdfDocumentViewport(
                            document = viewState.document,
                            repository = repository,
                            autoScrollEngine = autoScrollEngine,
                            isAutoScrolling = viewState.isAutoScrolling,
                            initialPageIndex = viewState.currentPageIndex,
                            initialZoomScale = viewState.zoomScale,
                            onPageChanged = { onIntent(ReaderIntent.PageChanged(it)) },
                            onScrollProgressChanged = { onIntent(ReaderIntent.ScrollProgressChanged(it)) },
                            onZoomScaleChanged = { onIntent(ReaderIntent.SetZoomScale(it)) },
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
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .widthIn(max = 480.dp)
                                .heightIn(max = maxHeight * 0.62f)
                        )
                    }
                }
            } else {
                // Empty state in Neo-Brutalism card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NeobrutalistCard(
                        modifier = Modifier
                            .widthIn(max = 380.dp)
                            .fillMaxWidth()
                            .testTag("reader_empty_state_card"),
                        containerColor = colors.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No PDF document opened",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the folder icon above to open a PDF",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.subtext,
                                textAlign = TextAlign.Center
                            )
                        }
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
