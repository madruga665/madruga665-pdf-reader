# Core Engine API Contract: PDF Auto-Scroll Reader

**Feature**: PDF Reader with Auto-Scroll and Speed Selector  
**Package**: `com.madruga665.pdfreader.core.engine`  

## 1. AutoScrollEngine Interface

Pure Kotlin contract managing delta-time frame calculation and velocity state without platform view dependencies.

```kotlin
package com.madruga665.pdfreader.core.engine

import kotlinx.coroutines.flow.StateFlow

interface AutoScrollEngine {
    val scrollConfig: StateFlow<ScrollConfiguration>
    
    /**
     * Starts or resumes automatic scrolling at current velocity.
     */
    fun start()
    
    /**
     * Immediately pauses scrolling. Guarantees no further offset accumulation.
     */
    fun pause()
    
    /**
     * Toggles play/pause state.
     */
    fun toggle()
    
    /**
     * Sets target velocity in pixels per second. Clamped to [10f, 500f].
     */
    fun setVelocity(pixelsPerSec: Float)
    
    /**
     * Sets speed using a predefined preset.
     */
    fun setPreset(preset: SpeedPreset)
    
    /**
     * Called by frame clock (e.g. withFrameNanos or Choreographer) on every frame.
     * @param deltaNanos Time elapsed since previous frame in nanoseconds.
     * @return Pixels to advance in this frame.
     */
    fun onFrame(deltaNanos: Long): Float
}
```

---

## 2. PdfDocumentRepository Interface

Contract for opening, parsing, and caching PDF pages.

```kotlin
package com.madruga665.pdfreader.core.repository

import android.graphics.Bitmap
import android.net.Uri
import com.madruga665.pdfreader.core.model.PdfDocument
import kotlinx.coroutines.flow.Flow

interface PdfDocumentRepository {
    /**
     * Loads metadata and validates the document at the given content Uri.
     */
    suspend fun openDocument(uri: Uri): Result<PdfDocument>
    
    /**
     * Asynchronously renders a single page into a hardware bitmap.
     * Implementations MUST use an LRU memory cache and pool buffers.
     */
    suspend fun renderPageBitmap(
        pageIndex: Int,
        targetWidthPx: Int,
        targetHeightPx: Int
    ): Result<Bitmap>
    
    /**
     * Releases active native resources (e.g. ParcelFileDescriptor, PdfRenderer).
     */
    fun closeCurrentDocument()
}
```

---

## 3. ReadingSessionStore Interface

Contract for saving and retrieving reading progress and speed preferences.

```kotlin
package com.madruga665.pdfreader.core.storage

import com.madruga665.pdfreader.core.model.ReadingSession
import kotlinx.coroutines.flow.Flow

interface ReadingSessionStore {
    /**
     * Emits the saved session for the given document URI, or null if first time.
     */
    fun getSession(documentUri: String): Flow<ReadingSession?>
    
    /**
     * Saves or updates reading progress and speed settings.
     */
    suspend fun saveSession(session: ReadingSession)
    
    /**
     * Global default speed preference across documents.
     */
    fun getGlobalDefaultSpeed(): Flow<Float>
    suspend fun setGlobalDefaultSpeed(speedPxPerSec: Float)
}
```
