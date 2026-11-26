# Feature Specification: Local Backup and Restore of Settings and Usage Data

**Feature Branch**: `004-local-backup-restore`

**Created**: 2026-11-25

**Status**: Draft

**Input**: User description: "Implementar opção de Backup e Restauração local de configurações e dados de uso do PDF Reader via SAF (JSON versionado)."

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Export Local Backup of Settings and Reading Data (Priority: P1) 🎯 MVP

As a reader who has configured custom auto-scroll speeds per PDF, reading positions, recent documents, and favorite folders,
I want to export all my settings and usage data to a single portable local backup file in a folder of my choice,
So that I can safeguard my configuration before reinstalling the app, resetting my device, or migrating to another device—entirely offline and under my control.

**Why this priority**: Exporting is the foundation of data portability and protection. Without a reliable export, users cannot safeguard their accumulated reading preferences and per-document speeds.

**Independent Test**: Configure a global speed, save custom speeds for at least two PDFs, open recent documents, and add a favorite folder. Trigger "Backup e Restauração" from the home screen, choose to export, pick a destination, and confirm a valid versioned backup file is created and a summary confirmation is shown.

**Acceptance Scenarios**:

1. **Given** the user is on the Home screen (whether populated or empty),  
   **When** the user taps the "Backup e Restauração" entry point in the top bar and chooses to export a backup,  
   **Then** the system opens the device's file-creation picker with a timestamped suggested filename (`pdf-reader-backup-YYYYMMDD-HHmm.json`).

2. **Given** the user confirms a destination for the backup file,  
   **When** the export completes,  
   **Then** the file contains the backup format version, export timestamp, app version, global default speed, all per-document reading sessions and custom speeds (including document filenames for reconciliation), recent documents, and favorite folders, and the user sees a confirmation message with a summary of exported counts.

3. **Given** the user opens the file-creation picker to export a backup,  
   **When** the user cancels without choosing a destination,  
   **Then** no file is written, no error message is shown, and the app returns cleanly to its previous state.

---

### User Story 2 - Import Backup with Preview and Merge/Replace Choice (Priority: P1) 🎯 MVP

As a reader restoring data after a reinstall, device migration, or accidental reset,
I want to select a previously exported backup file, preview its contents, and choose whether to merge it with my current data or replace everything,
So that I can recover my settings safely without unexpected data loss.

**Why this priority**: Backup is only valuable if restoration is safe, transparent, and controllable. Previewing and offering Merge (default) vs Replace protects users from overwriting newer local data by mistake.

**Independent Test**: Export a backup, modify or clear local app data, then trigger import, select the backup file, verify the confirmation dialog displays accurate item counts and export metadata, choose Merge or Replace, and confirm the home screen refreshes and displays the restored items along with a summary message.

**Acceptance Scenarios**:

1. **Given** the user selects a valid backup file to import,  
   **When** the file is read and validated,  
   **Then** before modifying any local data, the system displays a confirmation dialog showing the backup summary (export date, number of document sessions/custom speeds, recent documents, favorite folders) and lets the user choose between **Mesclar** (default) and **Substituir tudo**.

2. **Given** the user confirms import in **Mesclar** (Merge) mode,  
   **When** the restore completes,  
   **Then** existing local entries are preserved, new entries from the backup are added, conflicts for the same document identifier are resolved deterministically (preferring the most recent reading timestamp and preserving custom speeds when present), the recent documents list stays within the maximum limit (30), the home screen refreshes automatically, and a summary message is displayed.

3. **Given** the user confirms import in **Substituir tudo** (Replace) mode,  
   **When** the restore completes,  
   **Then** local preferences, reading sessions, recent documents, and favorite folders are replaced by the sanitized contents of the backup, the home screen refreshes automatically, and a summary message is displayed.

4. **Given** the user cancels either the file picker or the preview confirmation dialog,  
   **When** cancellation occurs,  
   **Then** no local data is modified and the user returns cleanly to the Home screen without error.

---

### User Story 3 - Resilient Post-Restore Usage and Filename Speed Reconciliation (Priority: P2)

As a reader who restored a backup onto a new device or after reinstalling the app,
I want the app to remain crash-free when restored file or folder references no longer have persisted system permissions, and to automatically recover my custom per-PDF scroll speed when I reopen the same PDF under a new system file reference,
So that my carefully tuned per-document speeds continue to work seamlessly across reinstalls and device migrations.

**Why this priority**: After reinstalling or switching devices, operating system document identifiers change and previous file permissions are revoked. Without graceful handling and filename-based reconciliation, restored per-PDF speeds would be stranded and broken links could crash the app.

**Independent Test**: Restore a backup containing a document entry with a custom speed (`185 px/s`) and filename (`livro-estudo.pdf`) under an unreachable URI. Verify tapping the unreachable recent/folder does not crash and shows a clear message. Then open `livro-estudo.pdf` via the file picker (producing a new URI) and verify the reader automatically applies and binds `185 px/s` as the custom saved speed for the new URI.

**Acceptance Scenarios**:

1. **Given** a backup has been restored containing recent documents or favorite folders whose system access permissions are no longer valid,  
   **When** the user views the Home screen or attempts to open one of those entries,  
   **Then** the application does not crash and handles the inaccessible item gracefully with clear user feedback.

2. **Given** a user opens a PDF file whose exact document identifier has no saved custom speed locally,  
   **When** a restored entry exists with the same filename (`fileName`) and a saved custom default speed,  
   **Then** the system reconciles the speed by filename, starts the document at that custom speed, and associates that custom speed with the new document identifier for future sessions.

3. **Given** multiple restored entries share the same filename with custom speeds,  
   **When** reconciliation runs for a newly opened PDF with that filename,  
   **Then** the system deterministically selects the most recently updated entry (highest `lastReadTimestamp`).

4. **Given** a PDF already has a saved custom speed for its exact document identifier,  
   **When** the user opens that PDF,  
   **Then** the exact identifier match always takes precedence over filename reconciliation.

---

### User Story 4 - Backup Validation, Sanitization, and Atomic Failure Safety (Priority: P2)

As a reader importing a backup file that might be corrupted, manually edited, empty, or from a newer incompatible version,
I want the app to validate the file completely before touching my current settings and sanitize out-of-range numeric values,
So that a broken backup can never corrupt my local data or leave the app in an invalid state.

**Why this priority**: Data integrity is critical when writing to persistent user storage. Partial writes or out-of-bounds values could degrade reading performance or wipe user data.

**Independent Test**: Attempt to import an empty file, malformed JSON, a non-backup JSON file, and a backup with an unsupported future schema version; verify each is rejected with a clear error message and local data remains completely unchanged. Import a valid backup containing out-of-range numeric values (e.g., speed `999`, zoom `10.0`, scroll ratio `-0.5`) and verify values are clamped to safe bounds (`10..500`, `0.5..4.0`, `0..1`).

**Acceptance Scenarios**:

1. **Given** the user selects an empty file, corrupted/non-JSON file, or a file missing required backup structure,  
   **When** import validation runs,  
   **Then** the system rejects the file, displays a clear error message, and performs zero writes to local storage.

2. **Given** the user selects a backup file with an unsupported `schemaVersion` (e.g., greater than `1` or invalid),  
   **When** import validation runs,  
   **Then** the system rejects the file with an incompatibility message and leaves existing local data untouched.

3. **Given** a structurally valid backup contains numeric values outside supported limits,  
   **When** the backup is imported,  
   **Then** speeds are clamped to `10..500` px/s, zoom scales to `0.5..4.0`, normalized scroll ratios to `0.0..1.0`, negative page indices to `0`, and recent documents are capped at `30` entries.

---

### Edge Cases

- **Empty app state export**: Exporting when the user has zero recents, zero favorite folders, and only default settings produces a valid backup file with empty collections and the current global default speed.
- **Empty backup import**: Importing a valid backup with zero sessions, zero recents, and zero favorites succeeds; in Merge mode it preserves all local data, and in Replace mode it clears local collections and restores the backup's global default speed.
- **Duplicate entries inside a single backup file**: Duplicate document identifiers or folder identifiers within a backup file are deduplicated deterministically during parsing/sanitization.
- **Responsive layout (Compact vs. Expanded)**: The "Backup e Restauração" action and its dialogs remain accessible, legible, and ergonomic on both compact smartphone screens and expanded tablet/foldable screens, including when the Home screen is in its empty state.
- **Unreadable file or I/O error during export/import**: If the storage provider fails mid-read or mid-write (e.g., storage full or revoked target), the operation fails gracefully with a user-facing error message without crashing or partially overwriting local preferences.
- **Merge conflict on global default speed**: In Merge mode, if the backup carries a valid global default speed and the local global speed is still at the app default, or when merging, conflict resolution is deterministic and documented (see Assumptions).

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Home screen top bar MUST provide a "Backup e Restauração" entry point accessible in both populated and empty states, adapting cleanly to compact and expanded screen widths.
- **FR-002**: Users MUST be able to export a complete local backup file to a user-chosen location using the system document-creation flow, with a default suggested filename formatted as `pdf-reader-backup-YYYYMMDD-HHmm.json`.
- **FR-003**: The exported backup payload MUST be a versioned structured document (`schemaVersion = 1`) containing metadata (`schemaVersion`, `exportedAt`, `appVersion`) and three data domains:
  1. Reader preferences: `globalDefaultSpeedPxPerSec` and all per-document sessions/speeds (`documentUri`, `fileName`, `currentPageIndex`, `normalizedScrollRatio`, `zoomScale`, `preferredSpeedPxPerSec`, `customDefaultSpeedPxPerSec`, `lastReadTimestamp`).
  2. Recent documents list (up to 30 entries).
  3. Favorite folders list.
- **FR-004**: Export and import file I/O and serialization/deserialization MUST execute off the user interface thread so the interface remains responsive.
- **FR-005**: After a successful export, the system MUST display a user-facing confirmation containing a summary of exported counts; cancelling the file picker MUST end silently without error.
- **FR-006**: Users MUST be able to select a backup file for import via the system document picker, supporting JSON files and flexible MIME fallback with internal content validation.
- **FR-007**: Before modifying any stored local data, the system MUST parse, validate, and sanitize the selected backup file completely and present a confirmation preview dialog displaying backup metadata/item counts and allowing selection between **Mesclar** (default) and **Substituir tudo**.
- **FR-008**: In **Mesclar** (Merge) mode, the system MUST preserve existing local data, add missing entries from the backup, resolve conflicts for identical document identifiers deterministically (by most recent `lastReadTimestamp`, preserving custom default speed when available), deduplicate favorite folders by URI, and enforce the maximum of 30 recent documents ordered by recency.
- **FR-009**: In **Substituir tudo** (Replace) mode, the system MUST replace stored preferences, reading sessions, recent documents, and favorite folders with the validated and sanitized backup contents.
- **FR-010**: Upon completing an import, the system MUST refresh the Home screen state automatically and display a summary message with restored counts.
- **FR-011**: The system MUST reject empty files, invalid JSON, structurally malformed backups, or incompatible `schemaVersion` values with a clear error message and without performing any partial writes to local storage.
- **FR-012**: During import sanitization, the system MUST clamp out-of-range numeric values to valid bounds: scroll speeds within `10..500` px/s, zoom scale within `0.5..4.0`, normalized scroll ratio within `0.0..1.0`, and page index `>= 0`.
- **FR-013**: Restored document or folder references that lack persisted system access permissions MUST NOT cause application crashes and MUST be handled gracefully when accessed.
- **FR-014**: When opening a PDF document that has no saved `customDefaultSpeedPxPerSec` for its exact `documentUri`, the system MUST attempt to reconcile a saved custom speed by matching `fileName` against stored/restored session entries (preferring the most recent `lastReadTimestamp`) and, if found, apply and persist that custom speed for the new `documentUri`.
- **FR-015**: All backup and restore operations MUST run 100% locally and offline without transmitting user data to external services.

### Key Entities *(include if feature involves data)*

- **BackupPayload**: Versioned root snapshot containing `schemaVersion`, `exportedAt` (epoch millis), `appVersion`, `globalDefaultSpeedPxPerSec`, `readingSessions`, `recentDocuments`, and `favoriteFolders`.
- **BackupReadingSession**: Portable representation of a document's reading state and speed configuration (`documentUri`, `fileName`, `currentPageIndex`, `normalizedScrollRatio`, `zoomScale`, `preferredSpeedPxPerSec`, `customDefaultSpeedPxPerSec`, `lastReadTimestamp`).
- **BackupRecentDocument**: Portable representation of a recently opened document (`uri`, `displayName`, `lastOpenedTimestamp`, `lastPageIndex`, `totalPages`).
- **BackupFavoriteFolder**: Portable representation of a bookmarked folder (`treeUri`, `displayName`, `addedTimestamp`).
- **RestoreMode**: User-selected restoration strategy: `MERGE` (default) or `REPLACE`.
- **BackupSummary / RestoreResult**: Aggregated counts (sessions, custom speeds, recents, favorite folders, export timestamp) used for confirmation preview and post-operation feedback.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can export a full backup or import and confirm a backup in under 30 seconds with at most 3 taps from the Home screen.
- **SC-002**: 100% of valid exported backup files round-trip through export → replace import without losing global speed, per-document custom speeds, reading sessions, recent documents (up to 30), or favorite folders.
- **SC-003**: 100% of invalid, empty, corrupted, or incompatible-version files are rejected before any local data is altered, leaving pre-existing user preferences intact.
- **SC-004**: 100% of restored per-PDF custom speeds with matching filenames are automatically recovered and bound when the user reopens the corresponding PDF under a new system file reference.
- **SC-005**: Zero application crashes occur when browsing or tapping restored recent documents or favorite folders whose system permissions are no longer valid.
- **SC-006**: UI remains responsive (no main-thread blocking) during export and import of typical backup payloads (up to 100+ document sessions, 30 recents, and 50 favorite folders).

---

## Assumptions

- **Global speed in Merge mode**: When merging, if the local global default speed differs from the backup's global default speed, the local global default speed is preserved unless it has not been customized (equals the app's built-in default `90 px/s`) while the backup specifies a different value; in Replace mode, the backup's global default speed always takes effect.
- **Filename reconciliation matching**: Filename matching for `customDefaultSpeedPxPerSec` reconciliation uses exact trimmed case-insensitive comparison on `fileName` (ignoring blank filenames) and picks the matching entry with the highest `lastReadTimestamp` that has a non-null `customDefaultSpeedPxPerSec`.
- **Existing sessions without `fileName`**: Existing reading sessions stored before this feature may not yet have `fileName` persisted in session storage; during export, the app enriches each session's `fileName` from its stored value or from matching recent documents when available (falling back to empty string if unknown). Going forward, opening/saving a session also persists `fileName` when known.
- **Offline-only scope**: Cloud sync, automatic scheduled backups, and password encryption of backup files are out of scope for v1.
