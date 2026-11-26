# Implementation Plan: Home Screen Recents and Favorite Folders

**Branch**: `002-home-recents-favorites` | **Date**: 2026-11-06 | **Spec**: [specs/002-home-recents-favorites/spec.md](spec.md)

**Input**: Feature specification from `specs/002-home-recents-favorites/spec.md`

## Summary

Build a responsive, modern Home Screen dashboard in Kotlin 2.4 and Jetpack Compose featuring a "Recent Documents" shelf and a "Favorite Folders" directory manager. Users can resume reading any recently opened document with a single tap (restoring exact page progress, scroll ratio, and speed preference) or pin favorite directories from device storage using the Storage Access Framework (`OpenDocumentTree`). The dashboard adapts fluidly between smartphones (vertical stream with horizontal recents carousel) and tablets/foldables (multi-column grid layout with quick folder browser).

## Technical Context

**Language/Version**: Kotlin 2.4.0 (Targeting JVM 17)

**Primary Dependencies**:
- Jetpack Compose BOM (`2024.11.00`)
- Compose Foundation, Material 3, Material Icons Extended, Adaptive (`androidx.compose.material3.adaptive`)
- AndroidX DataStore Preferences (`androidx.datastore:datastore-preferences:1.1.1`)
- AndroidX DocumentFile (`androidx.documentfile:documentfile:1.0.1`)
- AndroidX Lifecycle & ViewModel Compose (`2.8.7`)
- Kotlinx Coroutines (`1.9.0`)

**Storage**: AndroidX DataStore Preferences for serialized JSON lists of `RecentDocument` (max 30 items) and `FavoriteFolder`; Storage Access Framework (SAF) for persistent file and directory permissions (`takePersistableUriPermission`).

**Testing**: JUnit 4/5, Kotlinx-coroutines-test, Turbine, MockK.

**Target Platform**: Android 8.0+ (API Level 26+) to Android 15 (API Level 35) across smartphones and tablets.

**Project Type**: Native Android Application (Single-module, clean package separation).

**Performance Goals**:
- Home screen dashboard load in under 300ms.
- Recent document resume and first-page render in under 1.2s.
- Instantaneous (<16ms) local state updates upon removing or pinning items.

**Constraints**:
- 100% offline, zero network telemetry, complete document privacy.
- Robust handling of moved, renamed, or deleted files without crashing.

**Scale/Scope**: Up to 30 recent documents, unlimited pinned favorite folders with direct child scanning.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle / Gate | Requirement | Status | Verification & Design Proof |
|---|---|:---:|---|
| **I. Smooth & Predictable Rendering** | No main-thread blocking; asynchronous loading of recents and folder contents. | **PASS** | Directory scanning and DataStore queries run on `Dispatchers.IO`; UI uses Compose `LazyColumn` / `LazyRow` virtualization. |
| **II. Modular & Engine-Agnostic Core** | Headless repositories and business logic decoupled from presentation layer. | **PASS** | `RecentsRepository` and `FavoriteFoldersRepository` are clean interfaces testable on JVM without Android views. |
| **III. Test-First & Quality Gates** | TDD mandatory; de-duplication, max cap (30), unpinning, and error handling tested before implementation. | **PASS** | Unit tests defined for repositories and HomeViewModel in `quickstart.md`. |
| **IV. Responsive & Accessible Experience** | Ergonomic adaptation for smartphones and tablets; touch-friendly targets and clear empty states. | **PASS** | Adaptive layout using `WindowWidthSizeClass` (Compact carousel vs Expanded multi-column grid). |
| **V. Observability & Graceful Error Recovery** | Safe handling of deleted/inaccessible files and folders without crashes. | **PASS** | `isAvailable` flags, `Result<T>` sealed returns, and user removal prompts for stale bookmarks. |

*Re-evaluation post-Phase 1 design: All gates remain fully satisfied with zero violations.*

## Project Structure

### Documentation (this feature)

```text
specs/002-home-recents-favorites/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (/speckit-plan output)
├── research.md          # Architecture decisions & research (Phase 0)
├── data-model.md        # Entities, state transitions, invariants (Phase 1)
├── quickstart.md        # Verification and runnable test guide (Phase 1)
├── contracts/           # API and UI contracts (Phase 1)
│   └── home-contracts.md
└── checklists/
    └── requirements.md  # Specification quality checklist
```

### Source Code Layout

```text
app/src/
├── main/java/com/madruga665/pdfreader/
│   ├── MainActivity.kt                      # Top-level screen switcher (Home vs Reader)
│   ├── core/
│   │   ├── model/
│   │   │   ├── RecentDocument.kt            # Recent item entity
│   │   │   ├── FavoriteFolder.kt            # Pinned folder entity
│   │   │   └── FolderPdfItem.kt             # Child PDF entity
│   │   └── home/
│   │       ├── RecentsRepository.kt         # Recents interface
│   │       ├── DataStoreRecentsRepository.kt # DataStore implementation
│   │       ├── FavoriteFoldersRepository.kt # Folders interface
│   │       └── AndroidFavoriteFoldersRepository.kt # SAF DocumentFile implementation
│   └── ui/
│       ├── home/
│       │   ├── HomeViewModel.kt             # MVI ViewModel
│       │   ├── HomeViewState.kt             # State
│       │   ├── HomeIntent.kt                # Intents
│       │   ├── HomeSideEffect.kt            # Side effects
│       │   └── HomeScreen.kt                # Adaptive Home Composable
│       └── components/
│           ├── RecentDocumentCard.kt        # Card with title & progress
│           ├── FavoriteFolderCard.kt        # Folder card with count
│           ├── FolderContentsDialog.kt      # Browse PDFs in folder
│           └── HomeEmptyState.kt            # Friendly zero-state prompt
└── test/java/com/madruga665/pdfreader/
    ├── core/home/
    │   ├── DataStoreRecentsRepositoryTest.kt
    │   └── AndroidFavoriteFoldersRepositoryTest.kt
    └── ui/home/
        └── HomeViewModelTest.kt
```

**Structure Decision**: Standard Android clean architecture with single-activity screen switching. The existing `ReaderViewModel` and `AdaptiveReaderScaffold` remain intact and are launched whenever a document is chosen from the Home Screen or system intent.

## Complexity Tracking

*No violations detected. All design specifications strictly comply with the Constitution principles without requiring architectural exemptions.*
