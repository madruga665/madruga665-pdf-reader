package com.madruga665.pdfreader.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.madruga665.pdfreader.core.engine.DefaultAutoScrollEngine
import com.madruga665.pdfreader.core.model.PdfDocument
import com.madruga665.pdfreader.core.model.SpeedPreset
import com.madruga665.pdfreader.core.repository.PdfDocumentRepository
import com.madruga665.pdfreader.ui.components.AdaptiveReaderScaffold
import com.madruga665.pdfreader.ui.components.HomeEmptyState
import com.madruga665.pdfreader.ui.components.NeobrutalistButton
import com.madruga665.pdfreader.ui.components.NeobrutalistDialog
import com.madruga665.pdfreader.ui.components.SpeedSelectorControl
import com.madruga665.pdfreader.ui.reader.ReaderViewState
import com.madruga665.pdfreader.ui.theme.PdfReaderTheme
import org.junit.Assert.assertTrue
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

    @Test
    fun neobrutalistDialog_respectsMaxWidth480Dp() {
        composeTestRule.setContent {
            PdfReaderTheme(darkTheme = false) {
                NeobrutalistDialog(
                    onDismissRequest = {},
                    title = "Dialog Width Check",
                    modifier = Modifier.testTag("test_dialog_card"),
                    actions = {
                        NeobrutalistButton(text = "OK", onClick = {})
                    }
                ) {
                    Text("Body content")
                }
            }
        }

        val width = composeTestRule.onNodeWithTag("test_dialog_card").getUnclippedBoundsInRoot().width
        assertTrue("Expected dialog width <= 480.5.dp, was $width", width <= 480.5.dp)
    }

    @Test
    fun homeEmptyState_respectsMaxWidth440DpOnWideViewport() {
        composeTestRule.setContent {
            PdfReaderTheme(darkTheme = false) {
                Box(modifier = Modifier.requiredSize(1000.dp, 800.dp)) {
                    HomeEmptyState(
                        onOpenPdfClick = {},
                        onAddFolderClick = {}
                    )
                }
            }
        }

        val width = composeTestRule.onNodeWithTag("home_empty_state_card").getUnclippedBoundsInRoot().width
        assertTrue("Expected home empty card width in 439.dp..440.5.dp, was $width", width in 439.dp..440.5.dp)
    }

    @Test
    fun readerEmptyState_respectsMaxWidth380DpOnWideViewport() {
        composeTestRule.setContent {
            PdfReaderTheme(darkTheme = false) {
                Box(modifier = Modifier.requiredSize(1000.dp, 800.dp)) {
                    AdaptiveReaderScaffold(
                        viewState = ReaderViewState(),
                        windowWidthSizeClass = WindowWidthSizeClass.Expanded,
                        repository = fakeRepository,
                        autoScrollEngine = engine,
                        onIntent = {},
                        onOpenFilePicker = {}
                    )
                }
            }
        }

        val width = composeTestRule.onNodeWithTag("reader_empty_state_card").getUnclippedBoundsInRoot().width
        assertTrue("Expected reader empty card width in 379.dp..380.5.dp, was $width", width in 379.dp..380.5.dp)
    }
}
