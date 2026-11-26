package com.madruga665.pdfreader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.madruga665.pdfreader.core.backup.RestoreMode
import com.madruga665.pdfreader.ui.home.PendingRestorePreview
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BackupActionsDialog(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.SettingsBackupRestore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Backup e Restauração",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Salve ou restaure localmente suas velocidades por PDF, sessões de leitura, recentes e pastas favoritas em formato JSON.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onExportClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar backup (.json)")
                }

                OutlinedButton(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importar backup (.json)")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
fun RestorePreviewDialog(
    preview: PendingRestorePreview,
    onModeSelected: (RestoreMode) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val formattedDate = remember(preview.summary.exportedAt) {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        Instant.ofEpochMilli(preview.summary.exportedAt)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirmar restauração",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Exportado em $formattedDate (app v${preview.summary.appVersion})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SummaryRow("Velocidade padrão global", "${preview.summary.globalDefaultSpeedPxPerSec.toInt()} px/s")
                    SummaryRow("Sessões de leitura", "${preview.summary.sessionsCount}")
                    SummaryRow("Velocidades personalizadas", "${preview.summary.customSpeedsCount}")
                    SummaryRow("Documentos recentes", "${preview.summary.recentsCount}")
                    SummaryRow("Pastas favoritas", "${preview.summary.favoriteFoldersCount}")
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Modo de restauração",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                RestoreModeOption(
                    title = "Mesclar (padrão)",
                    description = "Preserva dados locais e mescla conflitos pela leitura mais recente mantendo velocidades personalizadas.",
                    selected = preview.selectedMode == RestoreMode.MERGE,
                    onClick = { onModeSelected(RestoreMode.MERGE) }
                )

                RestoreModeOption(
                    title = "Substituir tudo",
                    description = "Sobrescreve todas as configurações, recentes e pastas locais pelos dados do backup.",
                    selected = preview.selectedMode == RestoreMode.REPLACE,
                    onClick = { onModeSelected(RestoreMode.REPLACE) }
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Restaurar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RestoreModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Column(modifier = Modifier.padding(start = 4.dp, top = 2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
