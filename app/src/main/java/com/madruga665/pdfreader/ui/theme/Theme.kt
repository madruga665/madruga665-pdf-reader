package com.madruga665.pdfreader.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalNeobrutalismColors = staticCompositionLocalOf { LightNeobrutalismColors }

object NeobrutalismTheme {
    val colors: NeobrutalismColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNeobrutalismColors.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography
}

private fun NeobrutalismColors.toMaterialColorScheme(): ColorScheme = if (isDark) {
    darkColorScheme(
        primary = accentYellow,
        onPrimary = onAccent,
        primaryContainer = accentYellow,
        onPrimaryContainer = onAccent,
        secondary = accentPurple,
        onSecondary = onAccent,
        secondaryContainer = accentBlue,
        onSecondaryContainer = onAccent,
        tertiary = accentOrange,
        onTertiary = onAccent,
        tertiaryContainer = accentGreen,
        onTertiaryContainer = onAccent,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = progressTrack,
        onSurfaceVariant = subtext,
        outline = border,
        outlineVariant = border,
        error = errorText,
        onError = onAccent,
        errorContainer = accentRed,
        onErrorContainer = onAccent
    )
} else {
    lightColorScheme(
        primary = accentYellow,
        onPrimary = onAccent,
        primaryContainer = accentYellow,
        onPrimaryContainer = onAccent,
        secondary = accentPurple,
        onSecondary = onAccent,
        secondaryContainer = accentBlue,
        onSecondaryContainer = onAccent,
        tertiary = accentOrange,
        onTertiary = onAccent,
        tertiaryContainer = accentGreen,
        onTertiaryContainer = onAccent,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = progressTrack,
        onSurfaceVariant = subtext,
        outline = border,
        outlineVariant = border,
        error = errorText,
        onError = onAccent,
        errorContainer = accentRed,
        onErrorContainer = onAccent
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun PdfReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val neoColors = if (darkTheme) MochaDarkNeobrutalismColors else LightNeobrutalismColors
    val colorScheme = neoColors.toMaterialColorScheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalNeobrutalismColors provides neoColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NeobrutalismTypography,
            content = content
        )
    }
}
