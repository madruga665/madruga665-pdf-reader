package com.madruga665.pdfreader.ui

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.madruga665.pdfreader.core.engine.DefaultAutoScrollEngine
import com.madruga665.pdfreader.core.repository.PdfDocumentRepository
import com.madruga665.pdfreader.ui.components.AdaptiveReaderScaffold
import com.madruga665.pdfreader.ui.reader.ReaderViewState
import com.madruga665.pdfreader.ui.theme.PdfReaderTheme
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class ReaderScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockRepository = mockk<PdfDocumentRepository>(relaxed = true)
    private val engine = DefaultAutoScrollEngine()

    @Test
    fun emptyState_displaysPromptToOpenPdf() {
        composeTestRule.setContent {
            PdfReaderTheme {
                AdaptiveReaderScaffold(
                    viewState = ReaderViewState(),
                    windowWidthSizeClass = WindowWidthSizeClass.Compact,
                    repository = mockRepository,
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
                    repository = mockRepository,
                    autoScrollEngine = engine,
                    onIntent = {},
                    onOpenFilePicker = { pickerClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Open PDF").performClick()
        assert(pickerClicked)
    }
}
