package com.madruga665.pdfreader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.core.model.SpeedPreset
import com.madruga665.pdfreader.ui.theme.NeobrutalismMetrics
import com.madruga665.pdfreader.ui.theme.NeobrutalismTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
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
    val colors = NeobrutalismTheme.colors
    val chipShape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusSmall)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(SpeedPreset.SLOW, SpeedPreset.MEDIUM, SpeedPreset.FAST).forEach { preset ->
                val isSelected = activePreset == preset
                NeobrutalistButton(
                    onClick = { onPresetSelect(preset) },
                    modifier = Modifier.weight(1f),
                    containerColor = if (isSelected) colors.accentPurple else colors.surface,
                    contentColor = if (isSelected) colors.onAccent else colors.onSurface,
                    borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                    shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                    shape = chipShape,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = preset.displayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Slider and +/- Step Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeobrutalistIconButton(
                onClick = { onSpeedChange((currentSpeedPxPerSec - 10f).coerceIn(10f, 500f)) },
                containerColor = colors.background,
                contentColor = colors.onSurface,
                size = 36.dp,
                borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                shape = chipShape
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease Speed",
                    modifier = Modifier.size(18.dp)
                )
            }

            Slider(
                value = currentSpeedPxPerSec,
                onValueChange = onSpeedChange,
                valueRange = 10f..500f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = colors.accentYellow,
                    activeTrackColor = colors.accentPurple,
                    inactiveTrackColor = colors.progressTrack
                )
            )

            NeobrutalistIconButton(
                onClick = { onSpeedChange((currentSpeedPxPerSec + 10f).coerceIn(10f, 500f)) },
                containerColor = colors.background,
                contentColor = colors.onSurface,
                size = 36.dp,
                borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                shape = chipShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase Speed",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        NeobrutalistBadge(
            text = "${currentSpeedPxPerSec.roundToInt()} px/s",
            containerColor = colors.background,
            contentColor = colors.onSurface,
            borderWidth = NeobrutalismMetrics.BorderWidthCompact,
            shadowOffset = 0.dp
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            thickness = NeobrutalismMetrics.BorderWidthCompact,
            color = colors.border
        )

        // Document Saved Speed Indicator and Actions (stacked/flowing to avoid cramping at 360dp & 276dp rail)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (savedSpeedPxPerSec != null) {
                NeobrutalistBadge(
                    text = "Padrão salvo: ${savedSpeedPxPerSec.roundToInt()} px/s",
                    leadingIcon = Icons.Default.Bookmark,
                    containerColor = colors.accentYellow,
                    contentColor = colors.onAccent,
                    borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                    shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact
                )
            } else {
                Text(
                    text = "Padrão geral do app",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.subtext
                )
            }

            if ((savedSpeedPxPerSec != null && onResetSpeedToDefault != null) || onSaveCurrentSpeedAsDefault != null) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (savedSpeedPxPerSec != null && onResetSpeedToDefault != null) {
                        NeobrutalistButton(
                            onClick = onResetSpeedToDefault,
                            containerColor = colors.surface,
                            contentColor = colors.errorText,
                            borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                            shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                            shape = chipShape,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Restaurar",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    if (onSaveCurrentSpeedAsDefault != null) {
                        NeobrutalistButton(
                            onClick = onSaveCurrentSpeedAsDefault,
                            containerColor = colors.accentBlue,
                            contentColor = colors.onAccent,
                            borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                            shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                            shape = chipShape,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
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
}
