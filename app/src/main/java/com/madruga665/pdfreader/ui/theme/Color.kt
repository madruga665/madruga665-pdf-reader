package com.madruga665.pdfreader.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class NeobrutalismColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val readerViewportBackground: Color,
    val onSurface: Color,
    val subtext: Color,
    val border: Color,
    val shadow: Color,
    val onAccent: Color,
    val gridLine: Color,
    val progressTrack: Color,
    val accentYellow: Color,
    val accentPurple: Color,
    val accentOrange: Color,
    val accentBlue: Color,
    val accentGreen: Color,
    val accentRed: Color,
    val errorText: Color
)

val LightNeobrutalismColors = NeobrutalismColors(
    isDark = false,
    background = Color(0xFFF8F8F8),
    surface = Color(0xFFFFFFFF),
    readerViewportBackground = Color(0xFFEAEAEA),
    onSurface = Color(0xFF000000),
    subtext = Color(0xFF555555),
    border = Color(0xFF000000),
    shadow = Color(0xFF000000),
    onAccent = Color(0xFF000000),
    gridLine = Color(0xFFE2E2E2),
    progressTrack = Color(0xFFE5E5E5),
    accentYellow = Color(0xFFFFD600),
    accentPurple = Color(0xFF7C5CFF),
    accentOrange = Color(0xFFFF6B00),
    accentBlue = Color(0xFF3399FF),
    accentGreen = Color(0xFF22C55E),
    accentRed = Color(0xFFFF4B4B),
    errorText = Color(0xFFC62828)
)

val MochaDarkNeobrutalismColors = NeobrutalismColors(
    isDark = true,
    background = Color(0xFF1E1E2E),
    surface = Color(0xFF313244),
    readerViewportBackground = Color(0xFF181825),
    onSurface = Color(0xFFCDD6F4),
    subtext = Color(0xFFA6ADC8),
    border = Color(0xFF11111B),
    shadow = Color(0xFF11111B),
    onAccent = Color(0xFF11111B),
    gridLine = Color(0xFF28283D),
    progressTrack = Color(0xFF1E1E2E),
    accentYellow = Color(0xFFF9E2AF),
    accentPurple = Color(0xFFCBA6F7),
    accentOrange = Color(0xFFFAB387),
    accentBlue = Color(0xFF89B4FA),
    accentGreen = Color(0xFFA6E3A1),
    accentRed = Color(0xFFF38BA8),
    errorText = Color(0xFFF38BA8)
)

object NeobrutalismFolderAccent {
    fun accentForKey(key: String, colors: NeobrutalismColors): Color {
        val palette = listOf(
            colors.accentYellow,
            colors.accentPurple,
            colors.accentOrange,
            colors.accentBlue
        )
        val index = ((key.hashCode() % palette.size) + palette.size) % palette.size
        return palette[index]
    }
}
