package com.madruga665.pdfreader.core.model

sealed interface ReaderErrorState {
    data object None : ReaderErrorState
    data class FileNotFound(val message: String) : ReaderErrorState
    data class PasswordRequired(val isIncorrectAttempt: Boolean = false) : ReaderErrorState
    data class CorruptedDocument(val causeMessage: String) : ReaderErrorState
    data class OutOfMemory(val recoverySuggestion: String) : ReaderErrorState
}
