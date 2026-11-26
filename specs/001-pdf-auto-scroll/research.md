# Technical Research & Architecture Decisions: PDF Reader with Auto-Scroll

**Feature**: PDF Reader with Auto-Scroll and Speed Selector  
**Feature Branch**: `001-pdf-auto-scroll`  
**Date**: 2026-11-04  
**Status**: Resolved  

## Research Questions & Decisions

### 1. PDF Rendering Engine for Android Native

- **Context**: Need high-performance, crisp page rendering with memory pooling and virtualization support in Jetpack Compose, capable of rendering 60fps continuous vertical scrolling on phones and tablets.
- **Decision**: Use Android's native `android.graphics.pdf.PdfRenderer` with a background coroutine worker pool, bitmap recycling, and LRU cache for rendered pages.
- **Rationale**:
  - `android.graphics.pdf.PdfRenderer` is built into the Android platform (API 21+), adding zero megabytes to the APK footprint.
  - Highly efficient hardware-accelerated bitmap rendering into pre-allocated memory buffers.
  - Coupled with an LRU bitmap pool (`LruCache<Int, Bitmap>`) and Compose `LazyColumn` (or custom virtualized viewport), pages off-screen are immediately recycled, strictly meeting Constitution Principle I (Smooth & Predictable Rendering) and Principle V (Memory Pressure Safety).
- **Alternatives Considered**:
  - *PdfiumAndroid / AndroidPdfViewer*: Increases APK size by 15-20MB due to native `.so` binaries for multiple ABIs (armeabi-v7a, arm64-v8a, x86, x86_64) and introduces maintenance vulnerabilities.
  - *Webview + PDF.js*: Higher memory overhead, DOM translation latency, choppy frame rate during continuous auto-scrolling, violating Constitution Principle I.

---

### 2. Auto-Scroll Timing & Velocity Math Engine

- **Context**: Scrolling must maintain a continuous, jitter-free pace across variable screen refresh rates (60Hz, 90Hz, 120Hz) and respond instantly (<16ms) to speed changes without position jumps.
- **Decision**: Pure Kotlin delta-time accumulator driven by Compose's `withFrameNanos` / Android's `Choreographer`, updating scroll offsets as fractional pixels:
  $$\Delta \text{offset} = \text{speedPixelsPerSec} \times \frac{\Delta t_{\text{nanos}}}{10^9}$$
- **Rationale**:
  - Decouples travel velocity from the device frame rate. A 120Hz display advances by half the delta of a 60Hz display per frame, ensuring identical physical scroll distance over time.
  - Dynamic speed changes only modify the target multiplier without altering accumulated offsets, eliminating abrupt visual jumps.
  - Pure Kotlin logic implemented in a standalone headless module/class (`AutoScrollEngine`), enabling 100% automated unit testing with virtual time (`runTest` and `TestCoroutineScheduler`), satisfying Constitution Principles II & III.
- **Alternatives Considered**:
  - *Fixed-rate `Timer` / `delay(16)` coroutine loop*: Prone to drift, frame stuttering, and jank when GC pauses occur or refresh rates differ from 60Hz.
  - *Jetpack Compose standard `animateScrollBy` loop*: Designed for target-based animations with easing curves; chaining repeated small animations causes frame hitches and makes dynamic speed modulation clunky.

---

### 3. Responsive Layout across Phones and Tablets

- **Context**: The app must adapt dynamically across compact smartphones, foldables, and large tablets in both portrait and landscape modes.
- **Decision**: Adopt Material 3 Adaptive Layout (`androidx.compose.material3.adaptive`) with `WindowSizeClass` (Compact, Medium, Expanded):
  - **Compact (Phones Portrait)**: Full-width continuous document column; floating pill overlay controls at the bottom for thumb accessibility.
  - **Medium (Foldables / Phones Landscape)**: Adaptive margins; collapsible toolbar controls that auto-hide upon scroll initiation.
  - **Expanded (Tablets / Large Foldables Landscape)**: Centered reading column with comfortable gutters (avoiding excessively long line lengths); permanent or modal side navigation rail for page outline, speed sliders, and quick presets.
- **Rationale**:
  - Conforms to Android Modern App Architecture and Google Material 3 guidelines.
  - Reading position remains consistent during orientation changes using normalized vertical scroll ratios rather than raw absolute pixel counts.
- **Alternatives Considered**:
  - *Fixed universal layout*: Causes uncomfortably wide text lines on 11-inch tablets and cluttered toolbars on 5-inch phones.

---

### 4. Persistence & State Management Architecture

- **Context**: User preferences (default reading speed, last read document URI, page scroll progress, zoom scale) must persist safely across app launches and configuration changes.
- **Decision**: Unidirectional Data Flow (UDF / MVI) with AndroidX `DataStore Preferences` for persistent settings and Kotlin `StateFlow` in ViewModel.
- **Rationale**:
  - DataStore Preferences is transactional, fully asynchronous via Coroutines/Flow, non-blocking to the main thread, and avoids `SharedPreferences` deadlocks.
  - MVI ensures predictable state transitions: `ReaderIntent` (User Actions) $\to$ `ReaderViewModel` $\to$ `ReaderViewState` (Immutable UI state) $\to$ Jetpack Compose UI.
- **Alternatives Considered**:
  - *SQLite / Room Database*: Excessive complexity for storing document reading metadata and user preferences; DataStore provides simpler, robust key-value serialization.
