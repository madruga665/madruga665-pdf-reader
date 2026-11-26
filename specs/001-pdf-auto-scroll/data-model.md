# Data Model Specification: PDF Reader with Auto-Scroll

**Feature**: PDF Reader with Auto-Scroll and Speed Selector  
**Feature Branch**: `001-pdf-auto-scroll`  
**Date**: 2026-11-04  
**Status**: Ready for Implementation  

## Entities & Value Objects

### 1. `PdfDocument`
Represents an opened PDF document descriptor.

| Field | Type | Description | Validation |
|---|---|---|---|
| `uri` | `String` / `Uri` | Android content URI pointing to the file | Non-empty, valid content scheme |
| `fileName` | `String` | Display name of the document | Non-empty |
| `pageCount` | `Int` | Total number of pages in the PDF | $\ge 1$ |
| `fileSizeBytes` | `Long` | File size in bytes | $\ge 0$ |
| `isPasswordProtected` | `Boolean` | Whether document requires decryption credentials | Default `false` |
| `pageAspectRatios` | `List<Float>` | Width-to-height ratio for each page index | Size matches `pageCount`, all $> 0.0$ |

---

### 2. `ReadingSession`
Tracks the persistent and volatile state of a user reading a document.

| Field | Type | Description | Validation |
|---|---|---|---|
| `documentUri` | `String` | Foreign key matching `PdfDocument.uri` | Non-empty |
| `currentPageIndex` | `Int` | Top-most visible page index in viewport | $0 \le \text{currentPageIndex} < \text{pageCount}$ |
| `normalizedScrollRatio`| `Float` | Normalized scroll position through entire document ($0.0 \dots 1.0$) | $0.0 \le \text{ratio} \le 1.0$ |
| `zoomScale` | `Float` | Current zoom magnification factor | $0.5 \le \text{zoomScale} \le 4.0$ (Default `1.0`) |
| `preferredSpeedPxPerSec`| `Float` | User's preferred auto-scroll velocity in pixels/second | $10.0 \le \text{speed} \le 500.0$ (Default `80.0`) |
| `lastReadTimestamp` | `Long` | Epoch millisecond of last read interaction | Non-zero |

---

### 3. `ScrollConfiguration`
Dynamic runtime state driving the auto-scroll engine.

| Field | Type | Description | Validation |
|---|---|---|---|
| `isScrolling` | `Boolean` | True if auto-scroll is actively driving the viewport | Boolean |
| `velocityPxPerSec` | `Float` | Current speed in logical pixels per second | $10.0 \le \text{velocity} \le 500.0$ |
| `activePreset` | `SpeedPreset` | Current selected speed preset | `SpeedPreset` enum |
| `isOverlayVisible` | `Boolean` | Whether navigation bars & speed controls are visible | Auto-hides during active scrolling |

```kotlin
enum class SpeedPreset(val displayLabel: String, val speedPxPerSec: Float) {
    SLOW("Slow", 40f),
    MEDIUM("Medium", 90f),
    FAST("Fast", 180f),
    CUSTOM("Custom", -1f)
}
```

---

### 4. `DeviceLayoutProfile`
Computed screen adaptation properties.

| Field | Type | Description |
|---|---|---|
| `widthSizeClass` | `WindowWidthSizeClass` | `Compact` (<600dp), `Medium` (600-840dp), `Expanded` (>840dp) |
| `isLandscape` | `Boolean` | True if orientation is landscape |
| `maxContentWidthDp` | `Int` | Max width for reading column (e.g., 720dp on tablets) |
| `controlsAnchor` | `ControlsAnchor` | `BOTTOM_BAR` (compact) vs `SIDE_RAIL` (expanded) |

---

### 5. `ReaderErrorState`
Sealed interface representing all possible failure conditions without runtime crashes.

```kotlin
sealed interface ReaderErrorState {
    data object None : ReaderErrorState
    data class FileNotFound(val message: String) : ReaderErrorState
    data class PasswordRequired(val isIncorrectAttempt: Boolean = false) : ReaderErrorState
    data class CorruptedDocument(val causeMessage: String) : ReaderErrorState
    data class OutOfMemory(val recoverySuggestion: String) : ReaderErrorState
}
```

## State Machine & Transitions

```text
               ┌───────────────┐
               │     IDLE      │ (No document selected)
               └───────┬───────┘
                       │ OpenFile(Uri)
                       ▼
               ┌───────────────┐
               │    LOADING    │ (Validating PDF & reading metadata)
               └───────┬───────┘
                       │
       ┌───────────────┴───────────────┐
       │ Success                       │ Failure
       ▼                               ▼
┌───────────────┐              ┌───────────────┐
│    READING    │◄────Retry────│     ERROR     │
│   (PAUSED)    │              └───────────────┘
└───────┬───────┘
        │ StartAutoScroll / Space Key
        ▼
┌───────────────┐
│ AUTO-SCROLLING│
└───────┬───────┘
        │ UserTap / DragGesture / Pause / EndOfDoc
        ▼
┌───────────────┐
│    READING    │
│   (PAUSED)    │
└───────────────┘
```

## Validation Rules & Invariants

1. **Velocity Clamping**: `velocityPxPerSec` must strictly clamp between `10.0f` (min readable crawl) and `500.0f` (max rapid skim). Any input outside this range is normalized.
2. **Scroll Boundaries**: Scroll position cannot advance past the end of the last page. At $1.0$ (100% progress), `isScrolling` automatically flips to `false`.
3. **Touch Interruption Invariant**: Any pointer down / drag gesture instantaneously halts the auto-scroll ticker and sets `isScrolling = false` before gesture offset propagation.
4. **Orientation Invariance**: When orientation changes, the viewport preserves the exact percentage scroll ratio (`normalizedScrollRatio`), ensuring seamless reading continuity.
