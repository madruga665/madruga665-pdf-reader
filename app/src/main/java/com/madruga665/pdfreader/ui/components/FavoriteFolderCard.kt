package com.madruga665.pdfreader.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.ui.theme.NeobrutalismFolderAccent
import com.madruga665.pdfreader.ui.theme.NeobrutalismMetrics
import com.madruga665.pdfreader.ui.theme.NeobrutalismTheme
import com.madruga665.pdfreader.ui.theme.neobrutalistShadow

@Composable
fun FavoriteFolderCard(
    folder: FavoriteFolder,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NeobrutalismTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    val folderKey = folder.treeUri.ifBlank { folder.displayName }
    val accentColor = remember(folderKey, colors) {
        NeobrutalismFolderAccent.accentForKey(folderKey, colors)
    }

    val tabShape = RoundedCornerShape(
        topStart = NeobrutalismMetrics.CornerRadiusSmall,
        topEnd = NeobrutalismMetrics.CornerRadiusSmall
    )
    val iconShape = RoundedCornerShape(10.dp)
    val cardShape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusLarge)

    Column(modifier = modifier.fillMaxWidth()) {
        // Colored binder top tab
        Box(
            modifier = Modifier
                .padding(start = 14.dp)
                .width(90.dp)
                .height(16.dp)
                .neobrutalistShadow(
                    shadowColor = colors.shadow,
                    borderColor = colors.border,
                    borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                    shadowOffset = 0.dp,
                    shape = tabShape
                )
                .background(accentColor, tabShape)
        )

        NeobrutalistCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = colors.surface,
            borderColor = colors.border,
            shadowColor = colors.shadow,
            borderWidth = NeobrutalismMetrics.BorderWidth,
            shadowOffset = NeobrutalismMetrics.ShadowOffset,
            shape = cardShape,
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.padding(
                            end = NeobrutalismMetrics.ShadowOffsetCompact,
                            bottom = NeobrutalismMetrics.ShadowOffsetCompact
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .neobrutalistShadow(
                                    shadowColor = colors.shadow,
                                    borderColor = colors.border,
                                    borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                                    shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                                    shape = iconShape
                                )
                                .background(accentColor, iconShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = colors.onAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (folder.pathSummary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = folder.pathSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.subtext,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val fileCountLabel = if (folder.cachedFileCount == 1) {
                            "1 PDF"
                        } else {
                            "${folder.cachedFileCount} PDFs"
                        }
                        NeobrutalistBadge(
                            text = fileCountLabel,
                            containerColor = colors.background,
                            contentColor = colors.onSurface,
                            borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                            shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    NeobrutalistIconButton(
                        onClick = { menuExpanded = true },
                        containerColor = colors.background,
                        contentColor = colors.onSurface,
                        size = 36.dp,
                        borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                        shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                        shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusSmall)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Folder options",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = colors.surface,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusMedium),
                        border = BorderStroke(NeobrutalismMetrics.BorderWidth, colors.border)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Unpin folder",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.errorText
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onRemoveClick()
                            }
                        )
                    }
                }
            }
        }
    }
}
