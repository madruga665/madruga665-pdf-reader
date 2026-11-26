# UI Contracts: MVI Architecture & Jetpack Compose

**Feature**: PDF Reader with Auto-Scroll and Speed Selector  
**Package**: `com.madruga665.pdfreader.ui`  

## 1. MVI Intent (User Actions)

```kotlin
package com.madruga665.pdfreader.ui.reader

import android.net.Uri
import com.madruga665.pdfreader.core.model.SpeedPreset

sealed interface ReaderIntent {
    data class OpenDocument(val uri: Uri) : ReaderIntent
    data object ToggleAutoScroll : ReaderIntent
    data object PauseAutoScroll : ReaderIntent
    data object ResumeAutoScroll : ReaderIntent
    data class SetSpeed(val speedPxPerSec: Float) : ReaderIntent
    data class SelectPreset(val preset: SpeedPreset) : ReaderIntent
    data class ManualScrollOffsetChanged(val offsetFraction: Float) : ReaderIntent
    data class PageChanged(val pageIndex: Int) : ReaderIntent
    data class SetZoomScale(val scale: Float) : ReaderIntent
    data object ToggleOverlayVisibility : ReaderIntent
    data object RetryLoadDocument : ReaderIntent
}
```

---

## 2. Reader ViewState (Immutable UI State)

```kotlin
package com.madruga665.pdfreader.ui.reader

import com.madruga665.pdfreader.core.model.PdfDocument
import com.madruga665.pdfreader.core.model.ReaderErrorState
import com.madruga665.pdfreader.core.model.ScrollConfiguration
import com.madruga665.pdfreader.core.model.SpeedPreset

data class ReaderViewState(
    val isLoading: Boolean = false,
    val document: PdfDocument? = null,
    val currentPageIndex: Int = 0,
    val totalPages: Int = 0,
    val scrollProgressRatio: Float = 0.0f,
    val zoomScale: Float = 1.0f,
    val isAutoScrolling: Boolean = false,
    val currentSpeedPxPerSec: Float = 80.0f,
    val activePreset: SpeedPreset = SpeedPreset.MEDIUM,
    val isOverlayControlsVisible: Boolean = true,
    val error: ReaderErrorState = ReaderErrorState.None
)
```

---

## 3. Reader Side Effects (One-Off Events)

```kotlin
package com.madruga665.pdfreader.ui.reader

sealed interface ReaderSideEffect {
    data class ShowToast(val message: String) : ReaderSideEffect
    data object ReachedEndOfDocument : ReaderSideEffect
    data class RequestDocumentPassword(val hint: String) : ReaderSideEffect
}
```

---

## 4. Compose UI Component Hierarchy

```text
ReaderScreen(viewModel)
├── AdaptiveScaffold (WindowSizeClass-driven)
│   ├── TopAppBar (Document title, open action, page counter)
│   ├── MainContentArea
│   │   ├── PdfDocumentViewport (LazyColumn / custom virtualized canvas)
│   │   │   └── PdfPageItem (Canvas with cached Bitmap)
│   │   └── FloatingAutoScrollOverlay (Bottom pill on Phone / Side rail on Tablet)
│   │       ├── PlayPauseToggle
│   │       ├── SpeedSlider & ValueLabel
│   │       └── PresetSelectorChips (Slow, Medium, Fast)
│   └── ErrorDialog (corrupted, password, not found)
```
