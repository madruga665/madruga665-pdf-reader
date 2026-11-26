package com.madruga665.pdfreader.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object NeobrutalismMetrics {
    val BorderWidth = 2.5.dp
    val BorderWidthCompact = 2.dp
    val ShadowOffset = 4.dp
    val ShadowOffsetCompact = 2.dp
    val PressedShadowOffset = 1.dp
    val CornerRadiusSmall = 8.dp
    val CornerRadiusMedium = 12.dp
    val CornerRadiusLarge = 16.dp
    val GridSize = 24.dp
    val GridStrokeWidth = 1.dp
    val ExpandedReaderRailWidth = 276.dp
}

fun Modifier.neobrutalistShadow(
    shadowColor: Color,
    borderColor: Color,
    borderWidth: Dp = NeobrutalismMetrics.BorderWidth,
    shadowOffset: Dp = NeobrutalismMetrics.ShadowOffset,
    shape: Shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusMedium)
): Modifier = this
    .graphicsLayer { shadowElevation = 0f }
    .drawBehind {
        if (shadowOffset > 0.dp) {
            val shadowPx = shadowOffset.toPx()
            val outline = shape.createOutline(size, layoutDirection, this)
            drawContext.canvas.save()
            drawContext.canvas.translate(shadowPx, shadowPx)
            drawOutline(outline = outline, color = shadowColor)
            drawContext.canvas.restore()
        }
    }
    .then(
        if (borderWidth > 0.dp) {
            Modifier.border(borderWidth, borderColor, shape)
        } else {
            Modifier
        }
    )

fun Modifier.neobrutalistGridBackground(
    gridColor: Color,
    gridSize: Dp = NeobrutalismMetrics.GridSize,
    strokeWidth: Dp = NeobrutalismMetrics.GridStrokeWidth
): Modifier = this.drawBehind {
    val stepPx = gridSize.toPx()
    if (stepPx <= 0f) return@drawBehind
    val strokePx = strokeWidth.toPx()

    var x = 0f
    while (x <= size.width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = strokePx
        )
        x += stepPx
    }

    var y = 0f
    while (y <= size.height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokePx
        )
        y += stepPx
    }
}
