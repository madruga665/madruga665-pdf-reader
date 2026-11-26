# Contracts: Per-PDF Auto-Scroll Speed Persistence

**Feature**: Per-PDF Auto-Scroll Speed Persistence  
**Package**: `com.madruga665.pdfreader.core.storage` & `com.madruga665.pdfreader.ui.reader`  

---

## 1. Storage Contract (`ReadingSessionStore`)

```kotlin
package com.madruga665.pdfreader.core.storage

import com.madruga665.pdfreader.core.model.ReadingSession
import kotlinx.coroutines.flow.Flow

interface ReadingSessionStore {
    fun getSession(documentUri: String): Flow<ReadingSession?>
    suspend fun saveSession(session: ReadingSession)

    fun getGlobalDefaultSpeed(): Flow<Float>
    suspend fun setGlobalDefaultSpeed(speedPxPerSec: Float)

    fun getCustomDocumentSpeed(documentUri: String): Flow<Float?>
    suspend fun saveCustomDocumentSpeed(documentUri: String, speedPxPerSec: Float)
    suspend fun clearCustomDocumentSpeed(documentUri: String)
}
```

---

## 2. Reader MVI Contract Updates

```kotlin
package com.madruga665.pdfreader.ui.reader

sealed interface ReaderIntent {
    // Existing intents...
    data object SaveCurrentSpeedAsDocumentDefault : ReaderIntent
    data object ResetDocumentSpeedToDefault : ReaderIntent
}

sealed interface ReaderSideEffect {
    // Existing side effects...
    data class SpeedSavedForDocument(val speedPxPerSec: Float) : ReaderSideEffect
    data object SpeedResetToDefault : ReaderSideEffect
}
```
