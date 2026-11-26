package com.madruga665.pdfreader.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.madruga665.pdfreader.core.model.ReaderErrorState

@Composable
fun ErrorDialog(
    errorState: ReaderErrorState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    if (errorState is ReaderErrorState.None) return

    val (title, description) = when (errorState) {
        is ReaderErrorState.FileNotFound -> "File Not Found" to errorState.message
        is ReaderErrorState.PasswordRequired -> "Password Required" to "This document is password protected."
        is ReaderErrorState.CorruptedDocument -> "Corrupted Document" to "Unable to parse PDF: ${errorState.causeMessage}"
        is ReaderErrorState.OutOfMemory -> "Memory Limit" to errorState.recoverySuggestion
        is ReaderErrorState.None -> return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = description) },
        confirmButton = {
            Button(onClick = onRetry) {
                Text(text = "Choose Another File")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = "Dismiss")
            }
        }
    )
}
