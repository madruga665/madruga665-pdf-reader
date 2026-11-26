package com.madruga665.pdfreader.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class NeobrutalismThemeContractTest {

    @Test
    fun lightPalette_matchesExactNeobrutalismTokens() {
        val c = LightNeobrutalismColors
        assertFalse(c.isDark)
        assertEquals(Color(0xFFF8F8F8), c.background)
        assertEquals(Color(0xFFFFFFFF), c.surface)
        assertEquals(Color(0xFFEAEAEA), c.readerViewportBackground)
        assertEquals(Color(0xFF000000), c.onSurface)
        assertEquals(Color(0xFF555555), c.subtext)
        assertEquals(Color(0xFF000000), c.border)
        assertEquals(Color(0xFF000000), c.shadow)
        assertEquals(Color(0xFF000000), c.onAccent)
        assertEquals(Color(0xFFE2E2E2), c.gridLine)
        assertEquals(Color(0xFFE5E5E5), c.progressTrack)
        assertEquals(Color(0xFFFFD600), c.accentYellow)
        assertEquals(Color(0xFF7C5CFF), c.accentPurple)
        assertEquals(Color(0xFFFF6B00), c.accentOrange)
        assertEquals(Color(0xFF3399FF), c.accentBlue)
        assertEquals(Color(0xFF22C55E), c.accentGreen)
        assertEquals(Color(0xFFFF4B4B), c.accentRed)
        assertEquals(Color(0xFFC62828), c.errorText)
    }

    @Test
    fun mochaDarkPalette_matchesExactCatppuccinMochaTokens() {
        val c = MochaDarkNeobrutalismColors
        assertTrue(c.isDark)
        assertEquals(Color(0xFF1E1E2E), c.background)
        assertEquals(Color(0xFF313244), c.surface)
        assertEquals(Color(0xFF181825), c.readerViewportBackground)
        assertEquals(Color(0xFFCDD6F4), c.onSurface)
        assertEquals(Color(0xFFA6ADC8), c.subtext)
        assertEquals(Color(0xFF11111B), c.border)
        assertEquals(Color(0xFF11111B), c.shadow)
        assertEquals(Color(0xFF11111B), c.onAccent)
        assertEquals(Color(0xFF28283D), c.gridLine)
        assertEquals(Color(0xFF1E1E2E), c.progressTrack)
        assertEquals(Color(0xFFF9E2AF), c.accentYellow)
        assertEquals(Color(0xFFCBA6F7), c.accentPurple)
        assertEquals(Color(0xFFFAB387), c.accentOrange)
        assertEquals(Color(0xFF89B4FA), c.accentBlue)
        assertEquals(Color(0xFFA6E3A1), c.accentGreen)
        assertEquals(Color(0xFFF38BA8), c.accentRed)
        assertEquals(Color(0xFFF38BA8), c.errorText)
    }

    @Test
    fun allSurfacesAndTokens_areFullyOpaque() {
        listOf(LightNeobrutalismColors, MochaDarkNeobrutalismColors).forEach { palette ->
            val tokens = listOf(
                palette.background,
                palette.surface,
                palette.readerViewportBackground,
                palette.onSurface,
                palette.subtext,
                palette.border,
                palette.shadow,
                palette.onAccent,
                palette.gridLine,
                palette.progressTrack,
                palette.accentYellow,
                palette.accentPurple,
                palette.accentOrange,
                palette.accentBlue,
                palette.accentGreen,
                palette.accentRed,
                palette.errorText
            )
            tokens.forEach { color ->
                assertEquals("Expected alpha == 1f for $color", 1f, color.alpha, 0.0001f)
            }
        }
    }

    @Test
    fun gridLine_isDistinctFromBackgroundAndSurfaceInBothThemes() {
        listOf(LightNeobrutalismColors, MochaDarkNeobrutalismColors).forEach { palette ->
            assertNotEquals(palette.background, palette.gridLine)
            assertNotEquals(palette.surface, palette.gridLine)
        }
    }

    @Test
    fun metrics_respectNeobrutalismGeometryBounds() {
        assertTrue(NeobrutalismMetrics.BorderWidth in 2.dp..3.dp)
        assertTrue(NeobrutalismMetrics.BorderWidthCompact in 2.dp..3.dp)
        assertTrue(NeobrutalismMetrics.ShadowOffset in 2.dp..4.dp)
        assertTrue(NeobrutalismMetrics.ShadowOffsetCompact in 2.dp..4.dp)
        assertEquals(1.dp, NeobrutalismMetrics.PressedShadowOffset)
        assertTrue(NeobrutalismMetrics.CornerRadiusSmall in 8.dp..16.dp)
        assertTrue(NeobrutalismMetrics.CornerRadiusMedium in 8.dp..16.dp)
        assertTrue(NeobrutalismMetrics.CornerRadiusLarge in 8.dp..16.dp)
        assertEquals(24.dp, NeobrutalismMetrics.GridSize)
        assertEquals(1.dp, NeobrutalismMetrics.GridStrokeWidth)
        assertEquals(276.dp, NeobrutalismMetrics.ExpandedReaderRailWidth)
    }

    @Test
    fun folderAccent_isDeterministicAndWithinAllowedPalette() {
        val keys = listOf(
            "content://com.android.externalstorage.documents/tree/primary%3ADocuments",
            "content://com.android.externalstorage.documents/tree/primary%3ASheetMusic",
            "My Folder",
            "",
            "polygenelubricants" // hashCode == Int.MIN_VALUE
        )

        listOf(LightNeobrutalismColors, MochaDarkNeobrutalismColors).forEach { palette ->
            val allowed = setOf(
                palette.accentYellow,
                palette.accentPurple,
                palette.accentOrange,
                palette.accentBlue
            )
            keys.forEach { key ->
                val first = NeobrutalismFolderAccent.accentForKey(key, palette)
                val second = NeobrutalismFolderAccent.accentForKey(key, palette)
                assertEquals("Folder accent must be deterministic for '$key'", first, second)
                assertTrue("Folder accent must be in allowed palette", first in allowed)
            }
        }
    }

    @Test
    fun wcag22AA_contrastRatiosMeetMinimum4Point5ForAllTextAndAccentPairs() {
        listOf(
            "Light" to LightNeobrutalismColors,
            "MochaDark" to MochaDarkNeobrutalismColors
        ).forEach { (themeName, c) ->
            val pairs = listOf(
                "onSurface / surface" to (c.onSurface to c.surface),
                "onSurface / background" to (c.onSurface to c.background),
                "subtext / surface" to (c.subtext to c.surface),
                "subtext / background" to (c.subtext to c.background),
                "onAccent / accentYellow" to (c.onAccent to c.accentYellow),
                "onAccent / accentPurple" to (c.onAccent to c.accentPurple),
                "onAccent / accentOrange" to (c.onAccent to c.accentOrange),
                "onAccent / accentBlue" to (c.onAccent to c.accentBlue),
                "onAccent / accentGreen" to (c.onAccent to c.accentGreen),
                "onAccent / accentRed" to (c.onAccent to c.accentRed),
                "errorText / surface" to (c.errorText to c.surface)
            )

            pairs.forEach { (pairName, colors) ->
                val ratio = contrastRatio(colors.first, colors.second)
                assertTrue(
                    "$themeName $pairName contrast ratio %.2f is below WCAG 2.2 AA (4.5:1)".format(ratio),
                    ratio >= 4.5
                )
            }
        }
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val lum1 = relativeLuminance(foreground)
        val lum2 = relativeLuminance(background)
        val lighter = max(lum1, lum2)
        val darker = min(lum1, lum2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        val r = linearize(color.red.toDouble())
        val g = linearize(color.green.toDouble())
        val b = linearize(color.blue.toDouble())
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun linearize(channel: Double): Double {
        return if (channel <= 0.03928) {
            channel / 12.92
        } else {
            ((channel + 0.055) / 1.055).pow(2.4)
        }
    }
}
