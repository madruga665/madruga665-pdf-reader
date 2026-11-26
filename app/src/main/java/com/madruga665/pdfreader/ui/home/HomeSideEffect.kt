package com.madruga665.pdfreader.ui.home

import android.net.Uri

sealed interface HomeSideEffect {
    data class NavigateToReader(val uri: Uri) : HomeSideEffect
    data class ShowToast(val message: String, val isLong: Boolean = false) : HomeSideEffect
    data class LaunchRelinkPdfPicker(val documentId: String) : HomeSideEffect
}
