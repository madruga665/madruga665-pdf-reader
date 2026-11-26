# Implementation Plan: Per-PDF Auto-Scroll Speed Persistence

**Branch**: `003-per-pdf-scroll-speed` | **Date**: 2026-11-06 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/003-per-pdf-scroll-speed/spec.md`

## Summary

Allow users to explicitly save their active auto-scroll speed (in px/s) as the default velocity for any specific PDF document. Display an intuitive indicator within the speed controls showing the configured saved speed (or general fallback), and automatically apply this configured speed whenever the document is opened in future reading sessions. Provide a quick one-tap reset back to the application's global default speed.

## Technical Context

**Language/Version**: Kotlin 2.4.0 (JVM target 17)  
**Primary Dependencies**: Jetpack Compose, Material 3, AndroidX DataStore Preferences, Coroutines/Flow  
**Storage**: AndroidX DataStore Preferences (`pdf_reader_prefs`) with key `custom_default_speed_<uri>`  
**Testing**: JUnit 4, MockK, CashApp Turbine, Kotlinx Coroutines Test  
**Target Platform**: Android (Phones, Foldables, Tablets - minSdk 26, targetSdk 35)  
**Project Type**: Android Native Application  
**Performance Goals**: Instant speed change response (< 16ms), atomic background preference write with zero main-thread block, seamless 60fps auto-scroll  
**Constraints**: Fully offline-capable, resilient to URI changes, strictly bounded speeds (10 to 500 px/s)  
**Scale/Scope**: Clean extension to existing `ReadingSessionStore`, `ReaderViewModel`, and `SpeedSelectorControl`  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I (Smooth & Predictable Rendering)**: Speed adjustments and preference writes execute asynchronously on `Dispatchers.IO` / coroutine scopes. Auto-scroll velocity updates apply without dropping frames. **PASS**
- **Principle II (Modular & Engine-Agnostic Core)**: Speed mathematical constraints (`10f..500f`) and `ReadingSession` model remain pure Kotlin modules testable in JVM unit tests. **PASS**
- **Principle III (Test-First & Quality Gates)**: Unit tests for DataStore persistence, speed precedence in `ReaderViewModel`, and intent handling will be created before implementation. **PASS**
- **Principle IV (Responsive & Accessible Reader Experience)**: Speed indicator and save/reset actions are integrated seamlessly into the existing overlay and speed selector without obscuring reading text or violating auto-hide behavior. **PASS**
- **Principle V (Observability & Graceful Error Recovery)**: Corrupted or missing preferences fallback gracefully to global default speed (`90 px/s`) without crashes. **PASS**

## Project Structure

### Documentation (this feature)

```text
specs/003-per-pdf-scroll-speed/
├── spec.md              # Feature specification
├── plan.md              # This file
├── research.md          # Technical research & architecture decisions
├── data-model.md        # Entities, view state, and DataStore schema
├── quickstart.md        # Quickstart & verification scenarios
├── contracts/           # Storage and MVI contracts
│   └── speed-contracts.md
└── tasks.md             # Tasks created by /speckit-tasks
```

### Source Code

```text
app/src/main/java/com/madruga665/pdfreader/
├── core/
│   ├── model/
│   │   └── ReadingSession.kt             # Add customDefaultSpeedPxPerSec
│   └── storage/
│       ├── ReadingSessionStore.kt        # Add custom document speed contracts
│       └── DataStoreReadingSessionStore.kt # Implement DataStore keys and methods
└── ui/
    ├── components/
    │   ├── SpeedSelectorControl.kt       # Add saved speed indicator and save/reset buttons
    │   ├── FloatingAutoScrollOverlay.kt  # Forward saved speed state and callbacks
    │   └── AdaptiveReaderScaffold.kt     # Forward saved speed parameters to overlay
    └── reader/
        ├── ReaderIntent.kt               # Add SaveCurrentSpeed and ResetSpeed intents
        ├── ReaderSideEffect.kt           # Add confirmation side effects
        ├── ReaderViewState.kt            # Add documentSavedSpeedPxPerSec
        └── ReaderViewModel.kt            # Implement resolution precedence and intent handlers

app/src/test/java/com/madruga665/pdfreader/
├── core/
│   └── storage/
│       └── DataStoreReadingSessionStoreTest.kt # Unit tests for custom speed persistence
└── ui/
    └── reader/
        └── ReaderViewModelTest.kt        # Unit tests for speed resolution and intents
```
