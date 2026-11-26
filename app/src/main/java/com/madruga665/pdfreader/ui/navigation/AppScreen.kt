package com.madruga665.pdfreader.ui.navigation

import android.net.Uri

sealed interface AppScreen {
    data object Home : AppScreen
    data class Reader(val uri: Uri) : AppScreen
}
