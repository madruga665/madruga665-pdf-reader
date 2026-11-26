package com.madruga665.pdfreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.core.model.RecentDocument
import com.madruga665.pdfreader.ui.theme.NeobrutalismMetrics
import com.madruga665.pdfreader.ui.theme.NeobrutalismTheme
import com.madruga665.pdfreader.ui.theme.neobrutalistShadow
import kotlin.math.roundToInt

@Composable
fun RecentDocumentCard(
    document: RecentDocument,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NeobrutalismTheme.colors
    val iconBoxShape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusSmall)

    NeobrutalistCard(
        modifier = modifier,
        containerColor = colors.surface,
        borderColor = colors.border,
        shadowColor = colors.shadow,
        borderWidth = NeobrutalismMetrics.BorderWidth,
        shadowOffset = NeobrutalismMetrics.ShadowOffset,
        shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusLarge),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(
                            end = NeobrutalismMetrics.ShadowOffsetCompact,
                            bottom = NeobrutalismMetrics.ShadowOffsetCompact
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .neobrutalistShadow(
                                    shadowColor = colors.shadow,
                                    borderColor = colors.border,
                                    borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                                    shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                                    shape = iconBoxShape
                                )
                                .background(colors.accentRed, iconBoxShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = colors.onAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = document.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                NeobrutalistIconButton(
                    onClick = onRemoveClick,
                    containerColor = colors.background,
                    contentColor = colors.onSurface,
                    size = 32.dp,
                    borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                    shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                    shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove from recents",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val progressPercent = (document.progressRatio * 100).roundToInt()
            Text(
                text = "Page ${document.lastReadPageIndex + 1} of ${document.pageCount} • $progressPercent%",
                style = MaterialTheme.typography.bodySmall,
                color = colors.subtext,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            NeobrutalistProgressBar(
                progress = document.progressRatio,
                fillColor = colors.accentGreen,
                trackColor = colors.progressTrack,
                borderColor = colors.border
            )
        }
    }
}
