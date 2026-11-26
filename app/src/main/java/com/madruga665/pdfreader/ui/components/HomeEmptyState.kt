package com.madruga665.pdfreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.ui.theme.NeobrutalismMetrics
import com.madruga665.pdfreader.ui.theme.NeobrutalismTheme
import com.madruga665.pdfreader.ui.theme.neobrutalistShadow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeEmptyState(
    onOpenPdfClick: () -> Unit,
    onAddFolderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NeobrutalismTheme.colors
    val iconShape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        NeobrutalistCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.padding(end = 3.dp, bottom = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .neobrutalistShadow(
                                shadowColor = colors.shadow,
                                borderColor = colors.border,
                                borderWidth = NeobrutalismMetrics.BorderWidth,
                                shadowOffset = 3.dp,
                                shape = iconShape
                            )
                            .background(colors.accentYellow, iconShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = colors.onAccent,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Your Library is Empty",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Open any PDF document to start reading, or pin favorite folders to quickly access your collections.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.subtext,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NeobrutalistButton(
                        onClick = onOpenPdfClick,
                        containerColor = colors.accentYellow,
                        contentColor = colors.onAccent
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open PDF",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    NeobrutalistButton(
                        onClick = onAddFolderClick,
                        containerColor = colors.surface,
                        contentColor = colors.onSurface
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pin Folder",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
