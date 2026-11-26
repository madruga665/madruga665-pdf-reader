package com.madruga665.pdfreader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.core.model.SpeedPreset
import com.madruga665.pdfreader.ui.theme.NeobrutalismMetrics
import com.madruga665.pdfreader.ui.theme.NeobrutalismTheme

@Composable
fun FloatingAutoScrollOverlay(
    isVisible: Boolean,
    isAutoScrolling: Boolean,
    currentSpeedPxPerSec: Float,
    activePreset: SpeedPreset,
    onToggleAutoScroll: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPresetSelect: (SpeedPreset) -> Unit,
    savedSpeedPxPerSec: Float? = null,
    onSaveCurrentSpeedAsDefault: (() -> Unit)? = null,
    onResetSpeedToDefault: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = NeobrutalismTheme.colors

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        NeobrutalistCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            containerColor = colors.surface,
            borderColor = colors.border,
            shadowColor = colors.shadow,
            borderWidth = NeobrutalismMetrics.BorderWidth,
            shadowOffset = NeobrutalismMetrics.ShadowOffset,
            shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusLarge)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeobrutalistIconButton(
                        onClick = onToggleAutoScroll,
                        containerColor = if (isAutoScrolling) colors.accentOrange else colors.accentYellow,
                        contentColor = colors.onAccent,
                        size = 56.dp,
                        borderWidth = NeobrutalismMetrics.BorderWidth,
                        shadowOffset = NeobrutalismMetrics.ShadowOffset,
                        shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusMedium)
                    ) {
                        Icon(
                            imageVector = if (isAutoScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isAutoScrolling) "Pause Auto-Scroll" else "Start Auto-Scroll",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                SpeedSelectorControl(
                    currentSpeedPxPerSec = currentSpeedPxPerSec,
                    activePreset = activePreset,
                    onSpeedChange = onSpeedChange,
                    onPresetSelect = onPresetSelect,
                    savedSpeedPxPerSec = savedSpeedPxPerSec,
                    onSaveCurrentSpeedAsDefault = onSaveCurrentSpeedAsDefault,
                    onResetSpeedToDefault = onResetSpeedToDefault
                )
            }
        }
    }
}
