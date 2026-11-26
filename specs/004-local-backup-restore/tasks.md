---

description: "Actionable, dependency-ordered task list for Local Backup & Restore"
---

# Tasks: Local Backup and Restore of Settings and Usage Data

**Input**: Design documents from `/specs/004-local-backup-restore/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/backup-json-schema-v1.md`, `quickstart.md`

**Tests**: Required (TDD is mandatory per Constitution Principle III & `AGENTS.md`). Write unit tests first and verify them against implementation.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`, `[US4]`)
- Exact file paths included for every task

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish the pure-Kotlin `core/backup/` package and models shared across all user stories.

- [X] T001 [P] Create pure-Kotlin backup domain models (`BackupPayload`, `BackupReadingSession`, `RestoreMode`, `BackupSummary`, `BackupParseResult`, `BackupErrorReason`, `BackupExportResult`, `BackupRestoreResult`) in `app/src/main/java/com/madruga665/pdfreader/core/backup/BackupModels.kt`
- [X] T002 [P] Create pure-Kotlin `BackupFileGateway` interface and `BackupRepository` interface in `app/src/main/java/com/madruga665/pdfreader/core/backup/BackupFileGateway.kt` and `app/src/main/java/com/madruga665/pdfreader/core/backup/BackupRepository.kt`
- [X] T003 [P] Add optional `val fileName: String = ""` field to `ReadingSession` in `app/src/main/java/com/madruga665/pdfreader/core/model/ReadingSession.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Extend existing DataStore repositories with atomic snapshot/replace and filename persistence capabilities required by Export, Import, and Reconciliation.

**⚠️ CRITICAL**: Complete before User Stories 1–4.

### Tests (TDD)

- [X] T004 [P] Add unit tests for `fileName` persistence, `getAllSessionsSnapshot()`, `replaceAllData()`, and `findCustomSpeedByFileName()` in `app/src/test/java/com/madruga665/pdfreader/core/storage/DataStoreReadingSessionStoreTest.kt`
- [X] T005 [P] Add unit tests for `replaceAllRecents()` (including `MAX_RECENTS = 30` cap) in `app/src/test/java/com/madruga665/pdfreader/core/home/DataStoreRecentsRepositoryTest.kt`
- [X] T006 [P] Add unit tests for `replaceAllFavoriteFolders()` in `app/src/test/java/com/madruga665/pdfreader/core/home/AndroidFavoriteFoldersRepositoryTest.kt`

### Implementation

- [X] T007 [P] Update `ReadingSessionStore` and `DataStoreReadingSessionStore` (`filename_$uri`, `getAllSessionsSnapshot()`, `replaceAllData()`, `saveDocumentFileName()`, `findCustomSpeedByFileName()`) in `app/src/main/java/com/madruga665/pdfreader/core/storage/ReadingSessionStore.kt` and `app/src/main/java/com/madruga665/pdfreader/core/storage/DataStoreReadingSessionStore.kt`
- [X] T008 [P] Add `replaceAllRecents(documents: List<RecentDocument>)` to `RecentsRepository` and `DataStoreRecentsRepository` in `app/src/main/java/com/madruga665/pdfreader/core/home/RecentsRepository.kt` and `app/src/main/java/com/madruga665/pdfreader/core/home/DataStoreRecentsRepository.kt`
- [X] T009 [P] Add `replaceAllFavoriteFolders(folders: List<FavoriteFolder>)` to `FavoriteFoldersRepository` and `AndroidFavoriteFoldersRepository` in `app/src/main/java/com/madruga665/pdfreader/core/home/FavoriteFoldersRepository.kt` and `app/src/main/java/com/madruga665/pdfreader/core/home/AndroidFavoriteFoldersRepository.kt`

**Checkpoint**: All three DataStores support full snapshot & atomic replacement, and `pdf_reader_prefs` persists `fileName` per session.

---

## Phase 3: User Story 1 & User Story 4 — Export Local Backup + Validation & Sanitization (Priority: P1/P2) 🎯 MVP

**Goal**: Pure-Kotlin JSON codec (`schemaVersion = 1`) with strict validation, numeric sanitization (`speed 10..500`, `zoom 0.5..4.0`, `offset 0..1`, `pageIndex >= 0`, `MAX_RECENTS = 30`), timestamped filename generation (`pdf-reader-backup-YYYYMMDD-HHmm.json`), SAF I/O gateway, and `DefaultBackupRepository` export/preview.

**Independent Test**: Run `BackupJsonCodecTest` and `DefaultBackupRepositoryTest` on JVM; verify round-trip serialization, rejection of empty/malformed/incompatible-schema files with zero DataStore writes, and numeric clamping.

### Tests (TDD)

- [X] T010 [P] [US1] [US4] Write comprehensive JVM unit tests for `BackupJsonCodec` (encode/decode round-trip, suggested filename `pdf-reader-backup-YYYYMMDD-HHmm.json`, empty/invalid JSON rejection, unsupported `schemaVersion` rejection, numeric sanitization `10..500`, `0.5..4.0`, `0..1`, `MAX_RECENTS = 30`, deduplication) in `app/src/test/java/com/madruga665/pdfreader/core/backup/BackupJsonCodecTest.kt`

### Implementation

- [X] T011 [P] [US1] [US4] Implement pure-Kotlin `BackupJsonCodec` using `org.json` in `app/src/main/java/com/madruga665/pdfreader/core/backup/BackupJsonCodec.kt`
- [X] T012 [P] [US1] Implement `AndroidSafBackupFileGateway` executing `ContentResolver` read/write on `Dispatchers.IO` in `app/src/main/java/com/madruga665/pdfreader/core/backup/AndroidSafBackupFileGateway.kt`

---

## Phase 4: User Story 2 — Import Backup with Preview and Merge/Replace Choice (Priority: P1) 🎯 MVP

**Goal**: Deterministic `BackupMerger` (`MERGE` default vs `REPLACE`), full `DefaultBackupRepository` orchestration, Home MVI state/intents/ViewModel handling, responsive Material 3 dialogs, TopAppBar "Backup e Restauração" action (visible even when `isEmpty == true`), and SAF launchers in `MainActivity`.

**Independent Test**: Run `BackupMergerTest`, `DefaultBackupRepositoryTest`, and `HomeViewModelTest` on JVM verifying preview before writes, silent cancel, Merge conflict resolution, Replace overwrite, and `HomeIntent.Refresh` after restore.

### Tests (TDD)

- [X] T013 [P] [US2] Write JVM unit tests for `BackupMerger` (`REPLACE` mode, `MERGE` mode preserving local data, resolving same-URI conflicts by latest `lastReadTimestamp` while preserving `customDefaultSpeedPxPerSec`, capping recents at `30`, deduplicating folders) in `app/src/test/java/com/madruga665/pdfreader/core/backup/BackupMergerTest.kt`
- [X] T014 [P] [US2] Write JVM unit tests for `DefaultBackupRepository` (export enriches missing `fileName` from recents, preview rejects invalid JSON without modifying stores, restore applies Merge/Replace across all three stores) in `app/src/test/java/com/madruga665/pdfreader/core/backup/DefaultBackupRepositoryTest.kt`
- [X] T015 [P] [US2] Extend `HomeViewModelTest` with tests for export feedback, import preview dialog state, mode selection, confirm restore triggering refresh & summary feedback, error on invalid backup, and cancel without writes in `app/src/test/java/com/madruga665/pdfreader/ui/home/HomeViewModelTest.kt`

### Implementation

- [X] T016 [P] [US2] Implement pure-Kotlin `BackupMerger` in `app/src/main/java/com/madruga665/pdfreader/core/backup/BackupMerger.kt`
- [X] T017 [US2] Implement `DefaultBackupRepository` in `app/src/main/java/com/madruga665/pdfreader/core/backup/DefaultBackupRepository.kt` (depends on T011, T016)
- [X] T018 [US2] Extend `HomeIntent`, `HomeViewState`, and `HomeViewModel` to orchestrate backup export, import preview, `RestoreMode` selection, restore confirmation, error display, and automatic `HomeIntent.Refresh` in `app/src/main/java/com/madruga665/pdfreader/ui/home/HomeIntent.kt`, `app/src/main/java/com/madruga665/pdfreader/ui/home/HomeViewState.kt`, and `app/src/main/java/com/madruga665/pdfreader/ui/home/HomeViewModel.kt`
- [X] T019 [P] [US2] Create Material 3 `BackupActionsDialog` and `RestorePreviewDialog` (responsive Compact/Expanded, Merge default vs Replace) in `app/src/main/java/com/madruga665/pdfreader/ui/components/BackupDialogs.kt`
- [X] T020 [US2] Add "Backup e Restauração" action to `HomeScreen` TopAppBar (accessible even when `isEmpty == true`) and wire dialogs/snackbar in `app/src/main/java/com/madruga665/pdfreader/ui/home/HomeScreen.kt`
- [X] T021 [US2] Wire `DefaultBackupRepository`, `ActivityResultContracts.CreateDocument("application/json")` (with suggested filename), and `ActivityResultContracts.OpenDocument()` (with flexible MIME fallback) in `app/src/main/java/com/madruga665/pdfreader/MainActivity.kt`

---

## Phase 5: User Story 3 — SAF Resilience & Filename Speed Reconciliation (Priority: P2)

**Goal**: Handle revoked SAF permissions gracefully without crashes and automatically reconcile `customDefaultSpeedPxPerSec` by `fileName` when opening a PDF under a new SAF URI without an exact URI match.

**Independent Test**: Run `FilenameSpeedReconcilerTest` and `ReaderViewModelTest` verifying exact URI precedence, filename reconciliation (case-insensitive, latest `lastReadTimestamp`), binding reconciled speed to new URI, and `SecurityException` mapped to `ReaderErrorState.FileNotFound`.

### Tests (TDD)

- [X] T022 [P] [US3] Write JVM unit tests for `FilenameSpeedReconciler` (exact trimmed case-insensitive match, highest `lastReadTimestamp` tie-break, ignoring entries without `customDefaultSpeedPxPerSec` or blank filenames) in `app/src/test/java/com/madruga665/pdfreader/core/backup/FilenameSpeedReconcilerTest.kt`
- [X] T023 [P] [US3] Extend `ReaderViewModelTest` with tests for filename-based custom speed reconciliation on new URI (applying + persisting to new URI), exact URI precedence over filename match, persisting `fileName` on session/custom speed saves, and graceful `SecurityException` handling in `app/src/test/java/com/madruga665/pdfreader/ui/reader/ReaderViewModelTest.kt`

### Implementation

- [X] T024 [P] [US3] Implement pure-Kotlin `FilenameSpeedReconciler` in `app/src/main/java/com/madruga665/pdfreader/core/backup/FilenameSpeedReconciler.kt`
- [X] T025 [US3] Update `ReaderViewModel` to persist `fileName` in reading sessions/custom speeds, reconcile `customDefaultSpeedPxPerSec` via `sessionStore.findCustomSpeedByFileName(doc.fileName)` when exact URI has no custom speed, and map `SecurityException` to `ReaderErrorState.FileNotFound` in `app/src/main/java/com/madruga665/pdfreader/ui/reader/ReaderViewModel.kt`

---

## Phase 6: Polish & Verification

**Purpose**: Run full verification gates and ensure zero regressions or compiler warnings.

- [X] T026 Run `./gradlew testDebugUnitTest` and confirm 100% of unit tests pass
- [X] T027 Run `./gradlew assembleDebug` and confirm clean APK build
- [X] T028 Review all acceptance criteria in `spec.md` and mark completed tasks in `tasks.md`
