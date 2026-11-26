# Research & Architectural Decisions: Local Backup & Restore

## 1. JSON Serialization Strategy

- **Decision**: Reuse `org.json` (`JSONObject` / `JSONArray`), already used in `DataStoreRecentsRepository` and `AndroidFavoriteFoldersRepository`, and already configured in `app/build.gradle.kts` (`testImplementation(libs.json)`).
- **Rationale**: Zero extra dependencies, zero Gradle plugin overhead (no `kotlinx-serialization` plugin needed), fully testable on JVM, and deterministic control over validation and numeric clamping during parsing.
- **Alternatives considered**: `kotlinx.serialization` or Moshi/Gson — rejected because adding a serialization plugin/library violates the lean-dependency requirement when `org.json` is already present and well-tested in this repo.

## 2. SAF Document Creation & Selection Contracts

- **Decision**:
  - Export: `ActivityResultContracts.CreateDocument("application/json")` launched with `pdf-reader-backup-YYYYMMDD-HHmm.json`.
  - Import: `ActivityResultContracts.OpenDocument()` launched with `arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")` (flexible MIME filter because some Android file managers/cloud providers tag `.json` files as `application/octet-stream` or `text/plain`), followed by strict internal JSON & schema validation.
- **Rationale**: Ensures smooth user experience across OEM file pickers (Samsung My Files, Files by Google, Drive local cache) while enforcing strict structural validation internally before any DataStore mutation.
- **Alternatives considered**: Restricting `OpenDocument` strictly to `arrayOf("application/json")` — rejected because Android providers frequently misclassify `.json` backups as `text/plain` or `application/octet-stream`, causing files to appear greyed out.

## 3. Persisting `fileName` for Reading Sessions & Reconciliation

- **Decision**: Add `val fileName: String = ""` to `ReadingSession` and store `stringPreferencesKey("filename_$uri")` in `DataStoreReadingSessionStore` (`pdf_reader_prefs`).
- **Rationale**:
  - Backward-compatible default (`""`) preserves all existing callers and unit tests.
  - Enables `BackupReadingSession` to carry `fileName` even for documents that have fallen out of the 30-item `recents_prefs` cap.
  - During export, any legacy session with an empty `fileName` is opportunistically enriched from `recents_prefs` if a matching URI exists.
  - When `ReaderViewModel.openDocument(uri)` finds no `customDefaultSpeedPxPerSec` for the exact `uri.toString()`, it queries `sessionStore.findCustomSpeedByFileName(doc.fileName)` and, if found, applies that speed immediately and persists it via `sessionStore.saveCustomDocumentSpeed(uri.toString(), reconciledSpeed, doc.fileName)` so subsequent opens hit the exact URI directly.
- **Alternatives considered**: Storing a separate filename-to-speed map in a fourth DataStore — rejected to avoid fragmenting state across another store when `pdf_reader_prefs` already holds per-document session keys.

## 4. Handling Revoked SAF Permissions After Reinstall / Migration

- **Decision**:
  - Catch `SecurityException` in `AndroidPdfDocumentRepository.openDocument` (already caught as `Exception` -> `Result.failure`) and map `SecurityException` in `ReaderViewModel.openDocument` to `ReaderErrorState.FileNotFound` with a clear message instructing the user to reopen the file via the file picker.
  - Keep `AndroidFavoriteFoldersRepository.scanFolderPdfs` catching `SecurityException` and returning a clear failure message shown in `HomeScreen` Snackbar without crashing.
- **Rationale**: Restored SAF URIs cannot regain persisted URI permissions programmatically on Android; failing gracefully without crashes while allowing filename-based speed reconciliation when the user re-picks the file delivers the best possible Android SAF UX.
