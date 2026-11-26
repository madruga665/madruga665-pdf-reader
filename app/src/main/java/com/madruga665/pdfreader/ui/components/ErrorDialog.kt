package com.madruga665.pdfreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.core.model.ReaderErrorState
import com.madruga665.pdfreader.ui.theme.NeobrutalismMetrics
import com.madruga665.pdfreader.ui.theme.NeobrutalismTheme
import com.madruga665.pdfreader.ui.theme.neobrutalistShadow

@Composable
fun ErrorDialog(
    errorState: ReaderErrorState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    if (errorState is ReaderErrorState.None) return

    val (title, description) = when (errorState) {
        is ReaderErrorState.FileNotFound -> "File Not Found" to errorState.message
        is ReaderErrorState.PasswordRequired -> "Password Required" to "This document is password protected."
        is ReaderErrorState.CorruptedDocument -> "Corrupted Document" to "Unable to parse PDF: ${errorState.causeMessage}"
        is ReaderErrorState.OutOfMemory -> "Memory Limit" to errorState.recoverySuggestion
        is ReaderErrorState.None -> return
    }

    val colors = NeobrutalismTheme.colors
    val iconShape = RoundedCornerShape(10.dp)

    NeobrutalistDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier.padding(
                    end = NeobrutalismMetrics.ShadowOffsetCompact,
                    bottom = NeobrutalismMetrics.ShadowOffsetCompact
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .neobrutalistShadow(
                            shadowColor = colors.shadow,
                            borderColor = colors.border,
                            borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                            shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                            shape = iconShape
                        )
                        .background(colors.accentRed, iconShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = colors.onAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        title = title,
        actions = {
            NeobrutalistButton(
                text = "Dismiss",
                onClick = onDismiss,
                containerColor = colors.surface,
                contentColor = colors.onSurface,
                borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            )
            NeobrutalistButton(
                text = "Choose Another File",
                onClick = onRetry,
                containerColor = colors.accentYellow,
                contentColor = colors.onAccent,
                borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface
        )
    }
}
