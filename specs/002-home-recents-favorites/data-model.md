# Data Model Specification: Home Screen Recents and Favorite Folders

**Feature**: Home Screen Recents and Favorite Folders  
**Feature Branch**: `002-home-recents-favorites`  
**Date**: 2026-11-06  
**Status**: Ready for Implementation  

## Entities & Value Objects

### 1. `RecentDocument`
Represents a recorded document entry for resuming reading.

| Field | Type | Description | Validation |
|---|---|---|---|
| `uri` | `String` | Content URI pointing to the PDF document | Non-empty, valid content scheme |
| `fileName` | `String` | Display name of the file | Non-empty |
| `pageCount` | `Int` | Total number of pages | $\ge 1$ |
| `lastReadPageIndex`| `Int` | Last visible page index when closed | $0 \le \text{pageIndex} < \text{pageCount}$ |
| `progressRatio` | `Float` | Completion ratio through document | $0.0 \le \text{progressRatio} \le 1.0$ |
| `lastAccessedTimestamp` | `Long` | Epoch millisecond of last read interaction | Non-zero |
| `isAvailable` | `Boolean` | True if document URI is still valid on disk | Default `true` |

---

### 2. `FavoriteFolder`
Represents a user-pinned directory from device storage.

| Field | Type | Description | Validation |
|---|---|---|---|
| `treeUri` | `String` | Persistent SAF Tree URI granted by user | Non-empty, valid tree scheme |
| `displayName` | `String` | User-visible folder name | Non-empty |
| `pathSummary` | `String` | Readable path descriptor or storage hint | String |
| `pinnedTimestamp` | `Long` | Epoch millisecond when folder was pinned | Non-zero |
| `cachedFileCount` | `Int` | Number of detected PDF files in folder | $\ge 0$ |

---

### 3. `FolderPdfItem`
Represents a PDF file discovered inside a favorite folder.

| Field | Type | Description | Validation |
|---|---|---|---|
| `uri` | `String` | Content URI of the document | Non-empty |
| `fileName` | `String` | Display filename | Non-empty |
| `fileSizeBytes` | `Long` | File size in bytes | $\ge 0$ |
| `lastModified` | `Long` | Last modified epoch millisecond | $\ge 0$ |

---

### 4. `HomeViewState`
Aggregated state driving the Home dashboard UI.

| Field | Type | Description |
|---|---|---|
| `recentDocuments` | `List<RecentDocument>` | Ordered chronologically (newest first, max 30) |
| `favoriteFolders` | `List<FavoriteFolder>` | Ordered by pinned date or name |
| `activeFolder` | `FavoriteFolder?` | Currently inspected folder, or null if on main dashboard |
| `activeFolderPdfs` | `List<FolderPdfItem>` | PDFs inside active folder |
| `isLoading` | `Boolean` | True during initial storage load or folder scan |
| `errorMessage` | `String?` | Non-blocking user message if folder/file inaccessible |

## State Machine & Transitions

```text
               ┌────────────────────────┐
               │    HOME DASHBOARD      │
               │ (Recents + Fav Folders)│
               └───────────┬────────────┘
                           │
         ┌─────────────────┼──────────────────┐
         │ Tap Recent      │ Tap Fav Folder   │ Add Folder / Open PDF
         ▼                 ▼                  ▼
  ┌──────────────┐  ┌──────────────┐   ┌──────────────┐
  │ READER SCREEN│  │ FOLDER VIEW  │   │ SYSTEM SAF   │
  │ (Resume Read)│  │ (PDFs List)  │   │ PICKER DIALOG│
  └──────┬───────┘  └──────┬───────┘   └──────────────┘
         │ Back            │ Tap PDF / Back
         └─────────────────┘
```

## Validation Rules & Invariants

1. **Max Recents Cap**: The recent documents list is capped at 30 entries. When item 31 is recorded, the oldest item by `lastAccessedTimestamp` is trimmed.
2. **De-duplication**: Opening an existing recent document updates its `lastReadPageIndex`, `progressRatio`, and `lastAccessedTimestamp`, moving it to index 0 of the recents list without duplicating entries.
3. **Graceful Stale Handling**: If opening a recent item fails with `FileNotFoundException` or security error, `isAvailable` is marked `false` and a non-blocking dialog asks the user if they wish to remove the item from recents.
4. **Persistent Permission**: Any folder URI added to `favoriteFolders` must have its persistable URI permissions taken (`takePersistableUriPermission`) before insertion into storage.
