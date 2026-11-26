# Technical Research & Architecture Decisions: Home Screen Recents and Favorite Folders

**Feature**: Home Screen Recents and Favorite Folders  
**Feature Branch**: `002-home-recents-favorites`  
**Date**: 2026-11-06  
**Status**: Resolved  

## Research Questions & Decisions

### 1. Persistence Strategy for Recent Documents and Favorite Folders

- **Context**: Need lightweight, robust, asynchronous persistence for a list of recent documents (up to 30 items) and pinned favorite folders without adding heavy database dependencies.
- **Decision**: Extend AndroidX DataStore Preferences with a serialized JSON list (via `kotlinx.serialization.json` or simple JSON adapter) for `List<RecentDocument>` and `List<FavoriteFolder>`.
- **Rationale**:
  - The project already successfully uses AndroidX DataStore for `ReadingSessionStore`.
  - Storing a list of up to 30 recent items and 10-20 pinned folders involves trivial payload sizes (<20KB), which DataStore handles with zero disk lag, full coroutine flow reactivity, and atomic writes.
  - Adding Room would introduce unnecessary KSP annotation processing overhead, SQLite schemas, and migration files for simple user favorites.
- **Alternatives Considered**:
  - *Room SQLite Database*: Over-engineering for a list of 30 recent files; adds compile-time annotation processing weight and maintenance complexity.
  - *SharedPreferences*: Deprecated, blocking I/O on the main thread, prone to ANRs and lacking reactive Flow streams.

---

### 2. Folder Selection & File Browsing via Storage Access Framework (SAF)

- **Context**: On Android 8.0+ through Android 15 (Scoped Storage), the app needs user-granted access to whole directories chosen by the user and must be able to list PDF files inside them.
- **Decision**: Use `ActivityResultContracts.OpenDocumentTree()` with `contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)` to obtain persistent tree access. To query child documents, use `androidx.documentfile.provider.DocumentFile.fromTreeUri()` with background `Dispatchers.IO` dispatching.
- **Rationale**:
  - Standard, Google-recommended pattern for Scoped Storage compliance on Android.
  - `takePersistableUriPermission` ensures the app retains access across reboots and app updates.
  - Filtering by MIME type `application/pdf` or filename ending in `.pdf` isolates relevant documents cleanly.
- **Alternatives Considered**:
  - *MANAGE_EXTERNAL_STORAGE permission*: Violates Google Play policies for a PDF reader app and introduces major security risks.
  - *Direct File paths (`/storage/emulated/0/...`)*: Blocked on Android 11+ due to scoped storage boundaries.

---

### 3. Navigation & Screen Architecture

- **Context**: The app currently launches directly into the reader with an empty state prompt if no document is passed. With a Home Screen, the app needs two primary destinations: `Home` and `Reader`.
- **Decision**: Introduce a clean top-level screen state or navigation host:
  - `Screen.Home`: Displays Recents shelf, Favorite Folders list/grid, and a floating "Open PDF" / "Add Folder" button.
  - `Screen.Reader(uri)`: The active reading viewport with auto-scroll and speed controls.
  - Back button from Reader returns to Home, immediately refreshing the Recents section with updated progress.
- **Rationale**:
  - Keeps navigation simple and declarative without requiring heavy external navigation libraries.
  - Allows instant switching between dashboard and active reader while maintaining state.
- **Alternatives Considered**:
  - *AndroidX Navigation Compose*: Extra library dependency with route string parsing; single-activity state-driven navigation (`Screen.Home` vs `Screen.Reader(uri)`) is much lighter and easier to test.

---

### 4. Responsive Dashboard Layout across Phones and Tablets

- **Context**: Must look great on smartphones (compact portrait/landscape) and tablets (e.g. Samsung Galaxy Tab S9 FE).
- **Decision**: Utilize Material 3 `WindowWidthSizeClass`:
  - **Compact (Phones)**: Vertical column with a horizontal scrolling carousel for "Jump Back In (Recents)" and a vertical list for "Favorite Folders".
  - **Expanded / Medium (Tablets)**: Two-column / multi-column dashboard grid where Recents occupy a 2-row shelf on the left/top and Favorite Folders form an expansive grid with quick file counts.
- **Rationale**:
  - Fulfills Constitution Principle IV (Responsive & Accessible Reader Experience).
  - Maximizes screen real estate on tablets while remaining thumb-friendly on smartphones.
