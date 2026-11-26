package com.madruga665.pdfreader.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.madruga665.pdfreader.core.engine.DefaultAutoScrollEngine
import com.madruga665.pdfreader.core.model.PdfDocument
import com.madruga665.pdfreader.core.model.SpeedPreset
import com.madruga665.pdfreader.core.repository.PdfDocumentRepository
import com.madruga665.pdfreader.ui.components.AdaptiveReaderScaffold
import com.madruga665.pdfreader.ui.components.SpeedSelectorControl
import com.madruga665.pdfreader.ui.reader.ReaderViewState
import com.madruga665.pdfreader.ui.theme.PdfReaderTheme
import org.junit.Rule
import org.junit.Test

class ReaderScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeRepository = object : PdfDocumentRepository {
        override suspend fun openDocument(uri: Uri): Result<PdfDocument> =
            Result.failure(UnsupportedOperationException("Not used in empty-state UI tests"))

        override suspend fun renderPageBitmap(
            pageIndex: Int,
            targetWidthPx: Int,
            targetHeightPx: Int
        ): Result<Bitmap> =
            Result.failure(UnsupportedOperationException("Not used in empty-state UI tests"))

        override fun closeCurrentDocument() = Unit
    }
    private val engine = DefaultAutoScrollEngine()

    @Test
    fun emptyState_displaysPromptToOpenPdf() {
        composeTestRule.setContent {
            PdfReaderTheme {
                AdaptiveReaderScaffold(
                    viewState = ReaderViewState(),
                    windowWidthSizeClass = WindowWidthSizeClass.Compact,
                    repository = fakeRepository,
                    autoScrollEngine = engine,
                    onIntent = {},
                    onOpenFilePicker = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No PDF document opened").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Open PDF").assertIsDisplayed()
    }

    @Test
    fun openFilePicker_triggersCallback() {
        var pickerClicked = false
        composeTestRule.setContent {
            PdfReaderTheme {
                AdaptiveReaderScaffold(
                    viewState = ReaderViewState(),
                    windowWidthSizeClass = WindowWidthSizeClass.Compact,
                    repository = fakeRepository,
                    autoScrollEngine = engine,
                    onIntent = {},
                    onOpenFilePicker = { pickerClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Open PDF").performClick()
        assert(pickerClicked)
    }

    @Test
    fun emptyState_rendersInLightNeobrutalismTheme() {
        composeTestRule.setContent {
            PdfReaderTheme(darkTheme = false) {
                AdaptiveReaderScaffold(
                    viewState = ReaderViewState(),
                    windowWidthSizeClass = WindowWidthSizeClass.Compact,
                    repository = fakeRepository,
                    autoScrollEngine = engine,
                    onIntent = {},
                    onOpenFilePicker = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No PDF document opened").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Open PDF").assertIsDisplayed()
    }

    @Test
    fun emptyState_rendersInMochaDarkNeobrutalismTheme() {
        composeTestRule.setContent {
            PdfReaderTheme(darkTheme = true) {
                AdaptiveReaderScaffold(
                    viewState = ReaderViewState(),
                    windowWidthSizeClass = WindowWidthSizeClass.Compact,
                    repository = fakeRepository,
                    autoScrollEngine = engine,
                    onIntent = {},
                    onOpenFilePicker = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No PDF document opened").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Open PDF").assertIsDisplayed()
    }

    @Test
    fun speedSelectorControl_exposesSelectedSemanticsOnPresetChips() {
        composeTestRule.setContent {
            PdfReaderTheme(darkTheme = false) {
                SpeedSelectorControl(
                    currentSpeedPxPerSec = SpeedPreset.MEDIUM.speedPxPerSec,
                    activePreset = SpeedPreset.MEDIUM,
                    onSpeedChange = {},
                    onPresetSelect = {}
                )
            }
        }

        composeTestRule.onNodeWithText(SpeedPreset.SLOW.displayLabel).assertIsNotSelected()
        composeTestRule.onNodeWithText(SpeedPreset.MEDIUM.displayLabel).assertIsSelected()
        composeTestRule.onNodeWithText(SpeedPreset.FAST.displayLabel).assertIsNotSelected()
    }
}
