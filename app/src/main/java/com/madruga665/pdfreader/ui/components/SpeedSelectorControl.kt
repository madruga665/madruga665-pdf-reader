package com.madruga665.pdfreader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.core.model.SpeedPreset
import kotlin.math.roundToInt

@Composable
fun SpeedSelectorControl(
    currentSpeedPxPerSec: Float,
    activePreset: SpeedPreset,
    onSpeedChange: (Float) -> Unit,
    onPresetSelect: (SpeedPreset) -> Unit,
    savedSpeedPxPerSec: Float? = null,
    onSaveCurrentSpeedAsDefault: (() -> Unit)? = null,
    onResetSpeedToDefault: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(SpeedPreset.SLOW, SpeedPreset.MEDIUM, SpeedPreset.FAST).forEach { preset ->
                FilterChip(
                    selected = activePreset == preset,
                    onClick = { onPresetSelect(preset) },
                    label = { Text(preset.displayLabel) }
                )
            }
        }

        // Slider and +/- Step Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onSpeedChange((currentSpeedPxPerSec - 10f).coerceIn(10f, 500f)) }
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease Speed")
            }

            Slider(
                value = currentSpeedPxPerSec,
                onValueChange = onSpeedChange,
                valueRange = 10f..500f,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { onSpeedChange((currentSpeedPxPerSec + 10f).coerceIn(10f, 500f)) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase Speed")
            }
        }

        Text(
            text = "${currentSpeedPxPerSec.roundToInt()} px/s",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Document Saved Speed Indicator and Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                if (savedSpeedPxPerSec != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Padrão salvo: ${savedSpeedPxPerSec.roundToInt()} px/s",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = "Padrão geral do app",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (savedSpeedPxPerSec != null && onResetSpeedToDefault != null) {
                    TextButton(
                        onClick = onResetSpeedToDefault,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Restaurar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (onSaveCurrentSpeedAsDefault != null) {
                    FilledTonalButton(
                        onClick = onSaveCurrentSpeedAsDefault,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Salvar neste PDF",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
