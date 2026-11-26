package com.madruga665.pdfreader.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.core.repository.PdfDocumentRepository

@Composable
fun PdfPageItem(
    pageIndex: Int,
    aspectRatio: Float,
    repository: PdfDocumentRepository,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pageIndex) { mutableStateOf(true) }

    val safeRatio = aspectRatio.coerceIn(0.2f, 5.0f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(safeRatio),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val targetWidthPx = remember(maxWidth, density) {
            with(density) { maxWidth.roundToPx() }
        }
        val targetHeightPx = remember(targetWidthPx, safeRatio) {
            if (targetWidthPx > 0) (targetWidthPx / safeRatio).toInt() else 0
        }

        LaunchedEffect(pageIndex, targetWidthPx, targetHeightPx) {
            if (targetWidthPx > 0 && targetHeightPx > 0) {
                isLoading = true
                val result = repository.renderPageBitmap(pageIndex, targetWidthPx, targetHeightPx)
                bitmap = result.getOrNull()
                isLoading = false
            }
        }

        val currentBitmap = bitmap
        if (currentBitmap != null && !currentBitmap.isRecycled) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "PDF Page ${pageIndex + 1}",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
