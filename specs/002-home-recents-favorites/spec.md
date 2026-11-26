# Feature Specification: Home Screen Recents and Favorite Folders

**Feature Branch**: `002-home-recents-favorites`

**Created**: 2026-11-06

**Status**: Draft

**Input**: User description: "sessão de recents e pastas favoritas na tela inicial"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Quick Resume from Recent Documents (Priority: P1)

As a frequent reader, I want to see a list of my recently opened PDF documents on the home screen so that I can resume reading right where I left off with a single tap.

**Why this priority**: Readers frequently open the same document across multiple reading sessions; having an instant resume list eliminates the friction of navigating device file pickers every time.

**Independent Test**: Open a PDF document, read past page 1, return to the home screen, and verify that the document appears in the Recents section displaying its title, last read page, and progress ratio. Tapping it must reopen the document at the exact saved position.

**Acceptance Scenarios**:

1. **Given** a user has previously read one or more documents, **When** they launch the app or navigate to the home screen, **Then** a "Recent Documents" section is displayed, ordered chronologically by most recently accessed.
2. **Given** a document in the Recents list, **When** the user taps on it, **Then** the reader opens immediately, restoring the last read page, scroll offset, and reading speed.
3. **Given** a recent document card, **When** the user taps an item menu or long-presses and selects "Remove from Recents", **Then** the item is removed from the recent list without deleting the file from storage.
4. **Given** the user has not opened any documents yet, **When** they view the home screen, **Then** an informative empty state is shown with a prominent button to open a PDF.

---

### User Story 2 - Pinning and Browsing Favorite Folders (Priority: P2)

As a user organizing books, articles, or music sheets in specific folders, I want to pin my favorite folders to the home screen so that I can quickly view and open files stored in those locations.

**Why this priority**: Power users store their PDFs in dedicated directories (e.g., "Downloads/Books", "Study/Math"); pinning favorite folders allows direct folder exploration within the app without repeatedly launching the system folder browser.

**Independent Test**: Tap "Add Favorite Folder", choose a folder with PDF files via the system picker, verify the folder appears in the "Favorite Folders" section on the home screen, and tap it to browse and open PDFs located inside it.

**Acceptance Scenarios**:

1. **Given** a user is on the home screen, **When** they tap "Add Favorite Folder" and pick a directory from storage, **Then** the folder is saved with its display name and appears in the "Favorite Folders" section.
2. **Given** a pinned favorite folder on the home screen, **When** the user taps on it, **Then** they see a list of all PDF files present inside that folder with their titles and file sizes.
3. **Given** the list of PDFs within a favorite folder, **When** the user taps a file, **Then** it opens in the reader and automatically gets added to the Recent Documents section.
4. **Given** a pinned folder, **When** the user selects the option to unpin or remove the folder, **Then** the folder is removed from the favorites section while leaving the directory and its files intact on the device.

---

### User Story 3 - Responsive Home Dashboard across Phones and Tablets (Priority: P2)

As a user reading on smartphones or large tablets, I want the home screen to organize recents and favorite folders responsively so that navigation is ergonomic and takes full advantage of my device screen size.

**Why this priority**: Tablet screens have ample horizontal space that can display recents and folders side-by-side or in rich grids, whereas phone screens need clean, compact vertical lists or horizontal carousels.

**Independent Test**: View the home screen on phone screens and tablet screens in both portrait and landscape modes, ensuring content density, touch targets, and section hierarchy look well-proportioned and comfortable to use.

**Acceptance Scenarios**:

1. **Given** a smartphone screen, **When** viewing the home screen, **Then** Recents and Favorite Folders are stacked vertically with scrollable rows or compact cards optimized for single-hand scrolling.
2. **Given** a tablet or large foldable screen, **When** viewing the home screen, **Then** Recents and Favorite Folders adapt into an expansive multi-column grid or dual-pane dashboard layout.
3. **Given** an active home screen, **When** the device orientation changes between portrait and landscape, **Then** the dashboard smoothly reflows without losing scroll position or user selection.

---

### Edge Cases

- **File Deleted or Moved Externally**: When a user attempts to open a recent document that has been deleted, renamed, or moved outside the app, the system must not crash; it must present a clear message ("File no longer accessible") and offer to remove the stale entry from recents.
- **Folder Deleted or Inaccessible**: If a pinned favorite folder is deleted or storage permissions are revoked, the app must mark the folder as unavailable and offer to remove it or re-authorize storage access.
- **Empty Folder**: If a favorite folder contains no PDF files, navigating into it must show a clear "No PDF files found in this folder" message.
- **Large Number of Documents**: When a user accumulates dozens of recent items, the list must be capped (e.g., top 30 most recent) and paginated/virtualized so performance remains instantaneous without stutter.
- **Duplicate Opens**: Opening an already recent document must update its timestamp and move it to the top of the list rather than creating a duplicate entry.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display a "Recent Documents" section on the home screen containing previously opened PDF documents.
- **FR-002**: System MUST sort recent documents in descending order of last accessed timestamp (most recent first).
- **FR-003**: System MUST display metadata for each recent document item, including file title, last accessed date/time, total page count, and current reading progress (percentage or current page / total pages).
- **FR-004**: System MUST allow users to tap any recent document to instantly open it in the reader view, resuming at the exact last read page, scroll ratio, and speed configuration.
- **FR-005**: System MUST allow users to remove individual items from the recent documents list via an item action menu or long-press.
- **FR-006**: System MUST allow users to clear all recent documents with a confirmation prompt.
- **FR-007**: System MUST provide an "Add Favorite Folder" action allowing users to select and pin directories from device storage.
- **FR-008**: System MUST display a "Favorite Folders" section on the home screen listing all pinned directories with folder names and PDF item counts.
- **FR-009**: System MUST allow users to open a favorite folder to browse all contained PDF documents in a dedicated list or grid view.
- **FR-010**: System MUST allow users to unpin or remove folders from the favorite folders list without deleting files on disk.
- **FR-011**: System MUST automatically add any document opened from a favorite folder into the Recent Documents list.
- **FR-012**: System MUST persist the recents list and favorite folder entries across device restarts and app closures.
- **FR-013**: System MUST gracefully detect when a recent file or favorite folder is missing or inaccessible, displaying a non-blocking indicator and offering removal.
- **FR-014**: System MUST adapt the home screen layout responsively:
  - On compact/phone screens: vertical stacked flow with horizontal carousel or compact card list.
  - On expanded/tablet screens: adaptive multi-column dashboard with side-by-side recents shelf and favorite folder grid.
- **FR-015**: System MUST provide friendly, actionable empty states when no recent documents or favorite folders exist yet.

### Key Entities

- **RecentDocument**: Represents a recorded document entry. Attributes include document identifier/URI, file name, total page count, last read page index, normalized progress percentage ($0.0 \dots 1.0$), and last accessed timestamp.
- **FavoriteFolder**: Represents a user-pinned directory. Attributes include folder identifier/URI, display label, folder path descriptor, pinned timestamp, and cached PDF file count.
- **FolderDocumentItem**: Represents a PDF file located inside a favorite folder, including file URI, file name, file size, and last modified date.
- **HomeDashboardState**: Aggregates recent documents, favorite folders, active layout mode, and any storage permission warnings.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Home screen renders recent documents and favorite folders in under 300 milliseconds on app startup.
- **SC-002**: Tapping a recent document opens the reader and restores the reading position in under 1.2 seconds.
- **SC-003**: 95% of users can access their most frequent reading material with a single tap from the home screen without navigating system menus.
- **SC-004**: Adding a new favorite folder requires at most 3 user interactions (Tap "Add Folder" $\to$ Pick folder in system dialog $\to$ Confirm).
- **SC-005**: Zero application crashes when handling deleted, renamed, or inaccessible files or folders.

## Assumptions

- Pinned folders and recent documents reside in accessible local device storage or SD card storage supported by the system file framework.
- Up to 30 recent documents are stored in the active recents list to ensure fast loading and responsive UI.
- Recursive subfolder indexing inside favorite folders is limited to direct children to maintain fast folder opening performance; deep recursive searching is out of scope for initial release.
- Device permissions for persistent storage access are retained between sessions using the operating system's persistent URI grant mechanism.
