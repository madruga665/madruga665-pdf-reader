# Data Model: Per-PDF Auto-Scroll Speed Persistence

**Feature**: Per-PDF Auto-Scroll Speed Persistence  
**Feature Branch**: `003-per-pdf-scroll-speed`  
**Date**: 2026-11-06  

---

## Entities & Contracts

### 1. `ReadingSession` (Updated Domain Entity)

Represents reading session state and document-level preferences for a specific PDF.

```kotlin
package com.madruga665.pdfreader.core.model

data class ReadingSession(
    val documentUri: String,
    val currentPageIndex: Int,
    val normalizedScrollRatio: Float,
    val zoomScale: Float = 1.0f,
    val preferredSpeedPxPerSec: Float = 90.0f,
    val customDefaultSpeedPxPerSec: Float? = null,
    val lastReadTimestamp: Long = System.currentTimeMillis()
) {
    init {
        require(documentUri.isNotBlank()) { "documentUri cannot be blank" }
        require(currentPageIndex >= 0) { "currentPageIndex must be >= 0" }
        require(normalizedScrollRatio in 0.0f..1.0f) {
            "normalizedScrollRatio must be between 0.0 and 1.0, was $normalizedScrollRatio"
        }
        require(zoomScale in 0.5f..4.0f) {
            "zoomScale must be between 0.5 and 4.0, was $zoomScale"
        }
        require(preferredSpeedPxPerSec in 10.0f..500.0f) {
            "preferredSpeedPxPerSec must be between 10.0 and 500.0, was $preferredSpeedPxPerSec"
        }
        if (customDefaultSpeedPxPerSec != null) {
            require(customDefaultSpeedPxPerSec in 10.0f..500.0f) {
                "customDefaultSpeedPxPerSec must be between 10.0 and 500.0, was $customDefaultSpeedPxPerSec"
            }
        }
    }
}
```

---

### 2. `ReaderViewState` (Updated Presentation State)

Exposes the document's saved custom speed to UI components.

```kotlin
package com.madruga665.pdfreader.ui.reader

data class ReaderViewState(
    val isLoading: Boolean = false,
    val document: PdfDocument? = null,
    val currentPageIndex: Int = 0,
    val totalPages: Int = 0,
    val scrollProgressRatio: Float = 0.0f,
    val zoomScale: Float = 1.0f,
    val isAutoScrolling: Boolean = false,
    val currentSpeedPxPerSec: Float = SpeedPreset.MEDIUM.speedPxPerSec,
    val activePreset: SpeedPreset = SpeedPreset.MEDIUM,
    val documentSavedSpeedPxPerSec: Float? = null,
    val isOverlayControlsVisible: Boolean = true,
    val error: ReaderErrorState = ReaderErrorState.None
)
```

---

### 3. Preferences Storage Schema (AndroidX DataStore)

Key mappings in `pdf_reader_prefs`:

| Key Name | Type | Description |
|:---|:---|:---|
| `global_default_speed` | `Float` | Fallback velocity for documents without overrides (default: 90.0f) |
| `custom_default_speed_<uri>` | `Float` | Explicitly configured default velocity for `<uri>` |
| `speed_<uri>` | `Float` | Last transient velocity used during the previous reading session |
| `page_<uri>` | `Int` | Last read page index |
| `offset_<uri>` | `Float` | Fractional vertical scroll offset |
| `zoom_<uri>` | `Float` | Viewport zoom scale |
| `time_<uri>` | `Long` | Epoch timestamp of last access |
