# Data Model: Local Backup & Restore

## Domain Models (`core/backup/BackupModels.kt`)

### `BackupPayload`
Root in-memory representation of a versioned backup snapshot.
- `schemaVersion: Int = 1` (must equal `CURRENT_SCHEMA_VERSION = 1`)
- `exportedAt: Long` (epoch milliseconds, `> 0`)
- `appVersion: String` (e.g. `"1.0.0"`)
- `globalDefaultSpeedPxPerSec: Float` (clamped `10.0f..500.0f`, default `90.0f`)
- `readingSessions: List<BackupReadingSession>` (deduplicated by `documentUri`)
- `recentDocuments: List<RecentDocument>` (deduplicated by `uri`, sorted by `lastAccessedTimestamp` desc, capped at `30`)
- `favoriteFolders: List<FavoriteFolder>` (deduplicated by `treeUri`, sorted by `pinnedTimestamp` desc)

### `BackupReadingSession`
- `documentUri: String` (non-blank)
- `fileName: String` (trimmed, may be empty if unknown)
- `currentPageIndex: Int` (`>= 0`)
- `normalizedScrollRatio: Float` (`0.0f..1.0f`)
- `zoomScale: Float` (`0.5f..4.0f`)
- `preferredSpeedPxPerSec: Float` (`10.0f..500.0f`)
- `customDefaultSpeedPxPerSec: Float?` (`null` or `10.0f..500.0f`)
- `lastReadTimestamp: Long` (`> 0`)

### `RestoreMode`
- `MERGE` (default) — Preserves local entries, merges conflicts deterministically.
- `REPLACE` — Replaces all three DataStores with the sanitized backup payload.

### `BackupSummary`
- `exportedAt: Long`
- `appVersion: String`
- `globalDefaultSpeedPxPerSec: Float`
- `sessionsCount: Int`
- `customSpeedsCount: Int` (count of sessions where `customDefaultSpeedPxPerSec != null`)
- `recentsCount: Int`
- `favoriteFoldersCount: Int`

### `BackupParseResult`
- `Success(payload: BackupPayload, summary: BackupSummary)`
- `Failure(reason: BackupErrorReason, message: String)`

### `BackupErrorReason`
- `EMPTY_FILE`
- `INVALID_JSON`
- `UNSUPPORTED_SCHEMA_VERSION`
- `INVALID_STRUCTURE`
- `IO_ERROR`

### `BackupExportResult` & `BackupRestoreResult`
- `BackupExportResult(summary: BackupSummary)`
- `BackupRestoreResult(mode: RestoreMode, summary: BackupSummary)`

## Updated Existing Models

### `ReadingSession` (`core/model/ReadingSession.kt`)
- Add `val fileName: String = ""` (optional, backward-compatible).

## Validation & Sanitization Rules

| Field | Rule |
| --- | --- |
| `schemaVersion` | Must be integer `== 1`; otherwise reject with `UNSUPPORTED_SCHEMA_VERSION` or `INVALID_STRUCTURE`. |
| `globalDefaultSpeedPxPerSec` | Clamp finite values to `10.0f..500.0f`; fallback to `90.0f` if NaN/infinite/missing. |
| `preferredSpeedPxPerSec` | Clamp finite values to `10.0f..500.0f`; fallback to `90.0f`. |
| `customDefaultSpeedPxPerSec` | `null` if absent/null/non-numeric; otherwise clamp finite value to `10.0f..500.0f`. |
| `zoomScale` | Clamp finite values to `0.5f..4.0f`; fallback to `1.0f`. |
| `normalizedScrollRatio` | Clamp finite values to `0.0f..1.0f`; fallback to `0.0f`. |
| `currentPageIndex` | `coerceAtLeast(0)`. |
| `RecentDocument` | Skip blank `uri`/`fileName`; `pageCount >= 1`; `lastReadPageIndex in 0 until pageCount`; `progressRatio in 0f..1f`; max `30` entries. |
| `FavoriteFolder` | Skip blank `treeUri`/`displayName`; `cachedFileCount >= 0`; deduplicated by `treeUri`. |
