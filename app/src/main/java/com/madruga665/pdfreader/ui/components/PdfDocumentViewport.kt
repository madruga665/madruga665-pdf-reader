package com.madruga665.pdfreader.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.core.engine.AutoScrollEngine
import com.madruga665.pdfreader.core.model.PdfDocument
import com.madruga665.pdfreader.core.repository.PdfDocumentRepository

@Composable
fun PdfDocumentViewport(
    document: PdfDocument,
    repository: PdfDocumentRepository,
    autoScrollEngine: AutoScrollEngine,
    isAutoScrolling: Boolean,
    onPageChanged: (Int) -> Unit,
    onScrollProgressChanged: (Float) -> Unit,
    onTap: () -> Unit,
    onEndOfDocument: () -> Unit,
    onUserTouchPause: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    // Track active page index for display in the header
    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleItemIndex) {
        onPageChanged(firstVisibleItemIndex)
        if (document.pageCount > 0) {
            onScrollProgressChanged(firstVisibleItemIndex.toFloat() / document.pageCount.toFloat())
        }
    }

    // Auto-scroll loop using frame-synced dispatchRawDelta.
    // User touch interaction does NOT pause auto-scroll; pausing only happens via explicit button action.
    LaunchedEffect(isAutoScrolling) {
        if (!isAutoScrolling) return@LaunchedEffect
        var lastFrameNanos = 0L
        while (isAutoScrolling) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    val deltaNanos = frameNanos - lastFrameNanos
                    val pixelsToScroll = autoScrollEngine.onFrame(deltaNanos)
                    // If user is actively dragging the list manually, don't fight their drag.
                    // The moment they release their finger, auto-scroll smoothly continues.
                    if (pixelsToScroll > 0 && !listState.isScrollInProgress) {
                        val consumed = listState.dispatchRawDelta(pixelsToScroll)
                        // True end of document: last page visible and cannot scroll forward
                        val isLastPageVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == document.pageCount - 1
                        if (consumed <= 0.001f && !listState.canScrollForward && isLastPageVisible) {
                            onEndOfDocument()
                        }
                    }
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Single tap toggles the controls overlay without pausing auto-scroll
                detectTapGestures(
                    onTap = {
                        onTap()
                    }
                )
            }
            .pointerInput(Unit) {
                // Two-finger pinch-to-zoom without swallowing single-finger scroll drags
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size >= 2) {
                            val zoom = event.calculateZoom()
                            if (zoom != 1.0f) {
                                zoomScale = (zoomScale * zoom).coerceIn(0.5f, 4.0f)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = zoomScale,
                    scaleY = zoomScale
                )
        ) {
            items(
                count = document.pageCount,
                key = { pageIndex -> "${document.uri}_page_$pageIndex" }
            ) { pageIndex ->
                val ratio = document.pageAspectRatios.getOrElse(pageIndex) { 0.75f }
                PdfPageItem(
                    pageIndex = pageIndex,
                    aspectRatio = ratio,
                    repository = repository
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
