# Implementation Plan: Local Backup and Restore of Settings and Usage Data

**Branch**: `004-local-backup-restore` | **Date**: 2026-11-25 | **Spec**: [spec.md](file:///home/madruga665/Projetos/madruga665-pdf-reader/specs/004-local-backup-restore/spec.md)

**Input**: Feature specification from `/specs/004-local-backup-restore/spec.md`

## Summary

Implement 100% local, offline Backup & Restore of PDF Reader settings and usage data via Android Storage Access Framework (SAF) using a versioned JSON format (`schemaVersion = 1`). A pure Kotlin, JVM-testable `core/backup/` module handles serialization/deserialization (`org.json`), strict validation, numeric sanitization, deterministic Merge/Replace conflict resolution, and filename-based custom speed reconciliation. The Home screen exposes a responsive Material 3 "Backup e Restauração" entry point in the TopAppBar (accessible even when empty), supporting SAF export (`CreateDocument`), SAF import (`OpenDocument`) with pre-write validation and preview dialog (Merge default vs Replace), graceful handling of revoked SAF permissions, and automatic filename-based speed reconciliation when reopening PDFs under new URIs.

## Technical Context

**Language/Version**: Kotlin 2.4.0 (JVM target 17)
**Primary Dependencies**: Jetpack Compose (Material 3, Adaptive `WindowWidthSizeClass`), AndroidX DataStore Preferences, AndroidX DocumentFile, Activity Result Contracts (SAF), `org.json` (Android built-in + `libs.json` for JVM unit tests)
**Storage**: Three existing DataStore Preferences files (`pdf_reader_prefs`, `recents_prefs`, `favorite_folders_prefs`) + local JSON files via SAF
**Testing**: JUnit 4, `kotlinx-coroutines-test`, Turbine, MockK, `org.json` on JVM (`./gradlew testDebugUnitTest`)
**Target Platform**: Android (minSdk 26, targetSdk/compileSdk 35 — smartphones, foldables, tablets)
**Project Type**: Native Android mobile application (single `:app` Gradle module with Clean Architecture + MVI)
**Performance Goals**: Zero main-thread blocking; all SAF I/O, JSON encoding/decoding, and DataStore writes run on `Dispatchers.IO`; instant UI feedback
**Constraints**: 100% offline/local; no new third-party dependencies; atomic pre-write validation (zero partial writes on invalid backups); SAF permission revocation resilience
**Scale/Scope**: Typical user data of 1–200 reading sessions, up to 30 recent documents (`MAX_RECENTS`), and 1–50 favorite folders

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Smooth & Predictable Rendering (Performance First)**: PASS — All backup file I/O, JSON parsing/encoding, and DataStore persistence run off the main thread on `Dispatchers.IO`. No impact on reader 60fps loop.
- **II. Modular & Engine-Agnostic Core (Library-First)**: PASS — `core/backup/` contains pure Kotlin models, `BackupJsonCodec`, `BackupMerger`, and `FilenameSpeedReconciler` with zero Android framework imports, testable directly on JVM. Android SAF I/O is isolated behind `BackupFileGateway`.
- **III. Test-First & Quality Gates (NON-NEGOTIABLE)**: PASS — Unit tests for codec, validation/sanitization, merge/replace, filename reconciliation, repository, `HomeViewModel`, and `ReaderViewModel` will be written before implementation (TDD) and verified via `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.
- **IV. Responsive & Accessible Reader Experience**: PASS — TopAppBar action and Material 3 dialogs adapt cleanly across `WindowWidthSizeClass.Compact` and `Expanded`, and remain accessible in `HomeEmptyState`.
- **V. Observability & Graceful Error Recovery**: PASS — Invalid/empty/incompatible files are rejected before any DataStore write; `SecurityException` on revoked SAF URIs is caught gracefully without crashes.

## Project Structure

### Documentation (this feature)

```text
specs/004-local-backup-restore/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── backup-json-schema-v1.md
└── tasks.md
```

### Source Code (repository root)

```text
app/src/main/java/com/madruga665/pdfreader/
├── MainActivity.kt                                     # Register SAF CreateDocument / OpenDocument launchers & wire BackupRepository
├── core/
│   ├── backup/                                         # NEW: Pure Kotlin backup engine + SAF gateway
│   │   ├── BackupModels.kt                             # Payload, summary, restore mode, parse/export/restore results
│   │   ├── BackupJsonCodec.kt                          # org.json encode/decode, schema validation, numeric sanitization, filename generator
│   │   ├── BackupMerger.kt                             # Deterministic MERGE / REPLACE logic
│   │   ├── FilenameSpeedReconciler.kt                  # Filename-based custom speed matching (latest timestamp)
│   │   ├── BackupFileGateway.kt                        # Pure Kotlin I/O abstraction
│   │   ├── AndroidSafBackupFileGateway.kt              # ContentResolver SAF implementation on Dispatchers.IO
│   │   ├── BackupRepository.kt                         # Export, preview, and restore contract
│   │   └── DefaultBackupRepository.kt                  # Orchestrates stores + codec + merger + gateway
│   ├── home/
│   │   ├── RecentsRepository.kt                        # Add replaceAllRecents()
│   │   ├── DataStoreRecentsRepository.kt               # Implement replaceAllRecents()
│   │   ├── FavoriteFoldersRepository.kt                # Add replaceAllFavoriteFolders()
│   │   └── AndroidFavoriteFoldersRepository.kt         # Implement replaceAllFavoriteFolders() + SecurityException safety
│   ├── model/
│   │   └── ReadingSession.kt                           # Add optional fileName: String = ""
│   └── storage/
│       ├── ReadingSessionStore.kt                      # Add getAllSessionsSnapshot(), replaceAllData(), findCustomSpeedByFileName(), optional fileName on saveCustomDocumentSpeed()
│       └── DataStoreReadingSessionStore.kt             # Persist filename_$uri + implement snapshot/replace/reconcile
└── ui/
    ├── components/
    │   └── BackupDialogs.kt                            # NEW: Material 3 BackupActionsDialog & RestorePreviewDialog
    ├── home/
    │   ├── HomeIntent.kt                               # Add backup/restore intents
    │   ├── HomeViewState.kt                            # Add backup sheet, progress, and pending restore preview state
    │   ├── HomeViewModel.kt                            # Handle export, preview, mode selection, confirm restore + Refresh
    │   └── HomeScreen.kt                               # TopAppBar "Backup e Restauração" action + dialogs + snackbar feedback
    └── reader/
        └── ReaderViewModel.kt                          # Persist fileName on sessions/custom speeds, reconcile speed by fileName, handle SecurityException

app/src/test/java/com/madruga665/pdfreader/
├── core/
│   ├── backup/
│   │   ├── BackupJsonCodecTest.kt                      # Encode/decode, validation, sanitization, schemaVersion, filename format
│   │   ├── BackupMergerTest.kt                         # MERGE & REPLACE conflict resolution, MAX_RECENTS cap
│   │   ├── FilenameSpeedReconcilerTest.kt              # Case-insensitive matching, latest timestamp tie-break, blank handling
│   │   └── DefaultBackupRepositoryTest.kt              # End-to-end export, preview (no writes on error), restore
│   ├── home/
│   │   ├── DataStoreRecentsRepositoryTest.kt           # Extend with replaceAllRecents tests
│   │   └── AndroidFavoriteFoldersRepositoryTest.kt     # Extend with replaceAllFavoriteFolders & SecurityException resilience
│   └── storage/
│       └── DataStoreReadingSessionStoreTest.kt         # Extend with fileName persistence, snapshot, replaceAllData, findCustomSpeedByFileName
└── ui/
    ├── home/HomeViewModelTest.kt                       # Extend with export, preview, cancel, merge/replace restore + Refresh
    └── reader/ReaderViewModelTest.kt                   # Extend with filename speed reconciliation & SecurityException handling
```

**Structure Decision**: Follow the existing single-module `:app` Clean Architecture + MVI layout (`core/` for headless domain/data logic, `ui/` for Compose + ViewModels), introducing `core/backup/` as a pure-Kotlin domain package with a thin Android SAF gateway.

## Complexity Tracking

No Constitution violations. Zero new third-party dependencies added.
