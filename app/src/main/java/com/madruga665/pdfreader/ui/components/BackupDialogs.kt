package com.madruga665.pdfreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.core.backup.RestoreMode
import com.madruga665.pdfreader.ui.home.PendingRestorePreview
import com.madruga665.pdfreader.ui.theme.NeobrutalismMetrics
import com.madruga665.pdfreader.ui.theme.NeobrutalismTheme
import com.madruga665.pdfreader.ui.theme.neobrutalistShadow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BackupActionsDialog(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onDismiss: () -> Unit
) {
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
                        .background(colors.accentBlue, iconShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsBackupRestore,
                        contentDescription = null,
                        tint = colors.onAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        title = "Backup e Restauração",
        actions = {
            NeobrutalistButton(
                text = "Fechar",
                onClick = onDismiss,
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Salve ou restaure localmente suas velocidades por PDF, sessões de leitura, recentes e pastas favoritas em formato JSON.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.subtext
            )

            NeobrutalistButton(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth(),
                containerColor = colors.accentYellow,
                contentColor = colors.onAccent
            ) {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Exportar backup (.json)",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            NeobrutalistButton(
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth(),
                containerColor = colors.surface,
                contentColor = colors.onSurface
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Importar backup (.json)",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun RestorePreviewDialog(
    preview: PendingRestorePreview,
    onModeSelected: (RestoreMode) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = NeobrutalismTheme.colors
    val formattedDate = remember(preview.summary.exportedAt) {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        Instant.ofEpochMilli(preview.summary.exportedAt)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }
    val summaryShape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusMedium)

    NeobrutalistDialog(
        onDismissRequest = onDismiss,
        title = "Confirmar restauração",
        actions = {
            NeobrutalistButton(
                text = "Cancelar",
                onClick = onDismiss,
                containerColor = colors.surface,
                contentColor = colors.onSurface,
                borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            )
            NeobrutalistButton(
                text = "Restaurar",
                onClick = onConfirm,
                containerColor = if (preview.selectedMode == RestoreMode.REPLACE) {
                    colors.accentRed
                } else {
                    colors.accentYellow
                },
                contentColor = colors.onAccent,
                borderWidth = NeobrutalismMetrics.BorderWidthCompact,
                shadowOffset = NeobrutalismMetrics.ShadowOffsetCompact,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Exportado em $formattedDate (app v${preview.summary.appVersion})",
                style = MaterialTheme.typography.bodySmall,
                color = colors.subtext
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background, summaryShape)
                    .border(NeobrutalismMetrics.BorderWidthCompact, colors.border, summaryShape)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SummaryRow("Velocidade padrão global", "${preview.summary.globalDefaultSpeedPxPerSec.toInt()} px/s")
                SummaryRow("Sessões de leitura", "${preview.summary.sessionsCount}")
                SummaryRow("Velocidades personalizadas", "${preview.summary.customSpeedsCount}")
                SummaryRow("Documentos recentes", "${preview.summary.recentsCount}")
                SummaryRow("Pastas favoritas", "${preview.summary.favoriteFoldersCount}")
            }

            HorizontalDivider(
                thickness = NeobrutalismMetrics.BorderWidthCompact,
                color = colors.border
            )

            Text(
                text = "Modo de restauração",
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface
            )

            RestoreModeOption(
                title = "Mesclar (padrão)",
                description = "Preserva dados locais e mescla conflitos pela leitura mais recente mantendo velocidades personalizadas.",
                selected = preview.selectedMode == RestoreMode.MERGE,
                selectedContainerColor = colors.accentYellow,
                onClick = { onModeSelected(RestoreMode.MERGE) }
            )

            RestoreModeOption(
                title = "Substituir tudo",
                description = "Sobrescreve todas as configurações, recentes e pastas locais pelos dados do backup.",
                selected = preview.selectedMode == RestoreMode.REPLACE,
                selectedContainerColor = colors.accentRed,
                onClick = { onModeSelected(RestoreMode.REPLACE) }
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    val colors = NeobrutalismTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.subtext,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurface
        )
    }
}

@Composable
private fun RestoreModeOption(
    title: String,
    description: String,
    selected: Boolean,
    selectedContainerColor: Color,
    onClick: () -> Unit
) {
    val colors = NeobrutalismTheme.colors
    val shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusMedium)
    val containerColor = if (selected) selectedContainerColor else colors.background
    val titleColor = if (selected) colors.onAccent else colors.onSurface
    val descColor = if (selected) colors.onAccent else colors.subtext

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                end = if (selected) NeobrutalismMetrics.ShadowOffsetCompact else 0.dp,
                bottom = if (selected) NeobrutalismMetrics.ShadowOffsetCompact else 0.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .neobrutalistShadow(
                    shadowColor = colors.shadow,
                    borderColor = colors.border,
                    borderWidth = if (selected) NeobrutalismMetrics.BorderWidth else NeobrutalismMetrics.BorderWidthCompact,
                    shadowOffset = if (selected) NeobrutalismMetrics.ShadowOffsetCompact else 0.dp,
                    shape = shape
                )
                .background(containerColor, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = colors.onAccent,
                    unselectedColor = colors.onSurface
                )
            )
            Column(
                modifier = Modifier
                    .padding(start = 4.dp, top = 2.dp, end = 6.dp)
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = descColor
                )
            }
        }
    }
}
