# Implementation Plan: PDF Reader with Auto-Scroll and Speed Selector

**Branch**: `001-pdf-auto-scroll` | **Date**: 2026-11-04 | **Spec**: [specs/001-pdf-auto-scroll/spec.md](spec.md)

**Input**: Feature specification from `specs/001-pdf-auto-scroll/spec.md`

## Summary

Build a native Android PDF reader application in Kotlin and Jetpack Compose designed for smartphones, foldables, and tablets. The application delivers hands-free reading via a continuous, hardware-accelerated auto-scroll engine driven by delta-time frame calculations (60fps steady rendering) and an instant-response speed selector with presets (Slow, Medium, Fast) and continuous slider control. Document decoding is handled client-side using `android.graphics.pdf.PdfRenderer` with bitmap pooling and lazy page virtualization, ensuring maximum performance, low memory footprint, and complete user privacy.

## Technical Context

**Language/Version**: Kotlin 2.4.0 (Targeting JVM 17/21)

**Primary Dependencies**:
- Jetpack Compose BOM (`androidx.compose.bom:2024.09.00`+)
- Compose Foundation, Material 3, Adaptive (`androidx.compose.material3.adaptive`)
- AndroidX Lifecycle & ViewModel (`androidx.lifecycle:lifecycle-viewmodel-compose`)
- AndroidX DataStore Preferences (`androidx.datastore:datastore-preferences`)
- Kotlinx Coroutines & Flow (`org.jetbrains.kotlinx:kotlinx-coroutines-android`)
- Platform PDF Engine (`android.graphics.pdf.PdfRenderer`)

**Storage**: AndroidX DataStore Preferences for reading sessions and velocity settings; Storage Access Framework (SAF) for document URI access.

**Testing**: JUnit 5, Kotlinx-coroutines-test (`runTest`, `TestScope`), Turbine for Flow validation, Compose UI Test (`androidx.compose.ui:ui-test-junit4`), MockK.

**Target Platform**: Android 8.0+ (API Level 26+) through Android 15 (API Level 35) across smartphones, foldables, and tablets.

**Project Type**: Native Android Application (Single codebase, clean multi-package modular architecture).

**Performance Goals**:
- Steady 60fps (or native display rate) jank-free scrolling.
- Instantaneous scroll velocity adjustments (<16ms response).
- Document load & first-page render under 1.5 seconds.
- Memory usage capped via LRU bitmap pooling.

**Constraints**:
- 100% offline-capable; zero third-party remote tracking or telemetry.
- No bulky native binaries (zero external C++ `.so` blobs).

**Scale/Scope**: Fully responsive application supporting unlimited document page counts via lazy virtualization.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle / Gate | Requirement | Status | Verification & Design Proof |
|---|---|:---:|---|
| **I. Smooth & Predictable Rendering** | 60fps target; async background decoding; canvas/bitmap pooling; off-screen page virtualization. | **PASS** | `PdfRenderer` offloaded to background Coroutine I/O dispatcher; Compose `LazyColumn` virtualizes pages; bitmaps recycled in `LruCache`. |
| **II. Modular & Engine-Agnostic Core** | Headless core modules; velocity math independent of UI framework. | **PASS** | `AutoScrollEngine` is pure Kotlin with zero Android View/Compose dependencies, fully unit testable. |
| **III. Test-First & Quality Gates** | TDD mandatory; velocity calculations, boundary transitions, and touch interrupts covered by automated tests. | **PASS** | Comprehensive unit & integration test suites defined in `quickstart.md` using `kotlinx-coroutines-test` and Turbine. |
| **IV. Responsive & Accessible Reader** | Multi-modal controls (touch, keyboard, presets); thumb-friendly on phone; side-rail on tablet; auto-hiding controls. | **PASS** | Material 3 Adaptive layout (`WindowSizeClass`) with floating bottom pill on Compact and side rail on Expanded. |
| **V. Observability & Graceful Error Recovery** | Safe handling of corrupted/password files; structured errors without app crashes. | **PASS** | Sealed `ReaderErrorState` hierarchy with user recovery dialogs. |

*Re-evaluation post-Phase 1 design: All gates remain fully satisfied with zero violations.*

## Project Structure

### Documentation (this feature)

```text
specs/001-pdf-auto-scroll/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (/speckit-plan output)
├── research.md          # Architecture decisions & research (Phase 0)
├── data-model.md        # Entities, state transitions, invariants (Phase 1)
├── quickstart.md        # Verification and runnable test guide (Phase 1)
├── contracts/           # API and UI contracts (Phase 1)
│   ├── reader-engine-contract.md
│   └── ui-contracts.md
└── checklists/
    └── requirements.md  # Specification quality checklist
```

### Source Code Layout

```text
app/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   └── java/com/madruga665/pdfreader/
    │       ├── MainActivity.kt
    │       ├── core/
    │       │   ├── engine/
    │       │   │   ├── AutoScrollEngine.kt
    │       │   │   └── DefaultAutoScrollEngine.kt
    │       │   ├── model/
    │       │   │   ├── PdfDocument.kt
    │       │   │   ├── ReadingSession.kt
    │       │   │   ├── ScrollConfiguration.kt
    │       │   │   └── ReaderErrorState.kt
    │       │   ├── repository/
    │       │   │   ├── PdfDocumentRepository.kt
    │       │   │   └── AndroidPdfDocumentRepository.kt
    │       │   └── storage/
    │       │       ├── ReadingSessionStore.kt
    │       │       └── DataStoreReadingSessionStore.kt
    │       └── ui/
    │           ├── reader/
    │           │   ├── ReaderViewModel.kt
    │           │   ├── ReaderViewState.kt
    │           │   ├── ReaderIntent.kt
    │           │   └── ReaderSideEffect.kt
    │           ├── components/
    │           │   ├── PdfDocumentViewport.kt
    │           │   ├── PdfPageItem.kt
    │           │   ├── FloatingAutoScrollOverlay.kt
    │           │   ├── SpeedSelectorControl.kt
    │           │   └── ErrorDialog.kt
    │           └── theme/
    │               ├── Color.kt
    │               ├── Theme.kt
    │               └── Type.kt
    ├── test/java/com/madruga665/pdfreader/
    │   ├── core/
    │   │   └── engine/
    │   │       └── AutoScrollEngineTest.kt
    │   └── ui/
    │       └── reader/
    │           └── ReaderViewModelTest.kt
    └── androidTest/java/com/madruga665/pdfreader/
        └── ui/
            └── ReaderScreenTest.kt
```

**Structure Decision**: Standard Android single-application structure with strict clean-architecture separation:
- `core/engine`: Pure Kotlin headless business logic (auto-scroll ticker and velocity calculations).
- `core/repository`: Android platform PDF parsing, rendering, and bitmap cache.
- `core/storage`: DataStore persistence for sessions and preferences.
- `ui/reader`: Unidirectional MVI ViewModel and presentation state.
- `ui/components`: Declarative Jetpack Compose components adapting to phones and tablets.

## Complexity Tracking

*No violations detected. All design specifications strictly comply with the Constitution principles without requiring architectural exemptions.*
