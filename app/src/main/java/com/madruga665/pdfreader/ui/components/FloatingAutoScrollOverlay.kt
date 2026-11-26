package com.madruga665.pdfreader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.core.model.SpeedPreset

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
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = onToggleAutoScroll,
                        containerColor = if (isAutoScrolling) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isAutoScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isAutoScrolling) "Pause Auto-Scroll" else "Start Auto-Scroll"
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
