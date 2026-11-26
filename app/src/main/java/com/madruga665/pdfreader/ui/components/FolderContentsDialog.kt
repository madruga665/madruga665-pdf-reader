package com.madruga665.pdfreader.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.core.model.FavoriteFolder
import com.madruga665.pdfreader.core.model.FolderPdfItem
import com.madruga665.pdfreader.ui.theme.NeobrutalismFolderAccent
import com.madruga665.pdfreader.ui.theme.NeobrutalismMetrics
import com.madruga665.pdfreader.ui.theme.NeobrutalismTheme
import java.util.Locale

@Composable
fun FolderContentsDialog(
    folder: FavoriteFolder,
    pdfList: List<FolderPdfItem>,
    isLoading: Boolean,
    onPdfClick: (Uri) -> Unit,
    onDismissRequest: () -> Unit,
    onUnpinFolder: () -> Unit
) {
    val colors = NeobrutalismTheme.colors
    val folderKey = folder.treeUri.ifBlank { folder.displayName }
    val accentColor = remember(folderKey, colors) {
        NeobrutalismFolderAccent.accentForKey(folderKey, colors)
    }
    val iconShape = RoundedCornerShape(10.dp)
    val listShape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusMedium)

    NeobrutalistDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            NeobrutalistIconBox(
                icon = Icons.Default.Folder,
                containerColor = accentColor,
                boxSize = 40.dp,
                iconSize = 22.dp,
                shape = iconShape
            )
        },
        title = folder.displayName,
        actions = {
            NeobrutalistButton(
                text = "Unpin",
                onClick = {
                    onUnpinFolder()
                    onDismissRequest()
                },
                containerColor = colors.accentRed,
                contentColor = colors.onAccent,
                borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            )
            NeobrutalistButton(
                text = "Close",
                onClick = onDismissRequest,
                containerColor = colors.surface,
                contentColor = colors.onSurface,
                borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (pdfList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background, listShape)
                        .border(NeobrutalismMetrics.BorderWidthCompact, colors.border, listShape)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No PDF files found in this folder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.subtext
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background, listShape)
                        .border(NeobrutalismMetrics.BorderWidthCompact, colors.border, listShape)
                ) {
                    itemsIndexed(pdfList, key = { _, item -> item.uri }) { index, pdf ->
                        FolderPdfListItem(
                            item = pdf,
                            onClick = { onPdfClick(Uri.parse(pdf.uri)) }
                        )
                        if (index < pdfList.lastIndex) {
                            HorizontalDivider(
                                thickness = 1.5.dp,
                                color = colors.border
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderPdfListItem(
    item: FolderPdfItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NeobrutalismTheme.colors
    val badgeShape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusSmall)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(colors.accentBlue, badgeShape)
                .border(NeobrutalismMetrics.BorderWidthCompact, colors.border, badgeShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.fileName,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (item.fileSizeBytes > 0L) {
                Text(
                    text = formatFileSize(item.fileSizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.subtext
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}
