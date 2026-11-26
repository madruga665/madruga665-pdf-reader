# Quickstart & Verification Guide: Home Screen Recents and Favorite Folders

**Feature**: Home Screen Recents and Favorite Folders  
**Target Environment**: Android Studio / JDK 21 / Android 8.0+  

## 1. Automated Unit Test Execution

Run unit tests for recents storage, favorite folder repository, and HomeViewModel:

```bash
# Run unit tests for Recents and Favorite Folders
./gradlew testDebugUnitTest --tests "com.madruga665.pdfreader.core.home.*"
./gradlew testDebugUnitTest --tests "com.madruga665.pdfreader.ui.home.*"

# Run full project test suite
./gradlew testDebugUnitTest
```

### Expected Test Outcomes
- `RecentsRepositoryTest`:
  - `recordDocument_appendsAndEnforcesCap30`: PASS
  - `recordDocument_deduplicatesAndMovesToTop`: PASS
  - `removeRecent_removesCorrectItem`: PASS
- `FavoriteFoldersRepositoryTest`:
  - `addFolder_persistsTreeUriAndCalculatesCount`: PASS
  - `removeFolder_unpinsFolder`: PASS
- `HomeViewModelTest`:
  - `openDocument_emitsNavigateToReaderSideEffect`: PASS
  - `selectFolder_loadsContainedPdfs`: PASS

---

## 2. End-to-End Manual Verification Scenarios

### Scenario A: Empty Home Dashboard
1. Launch the app without any previously opened documents.
2. **Verification**: Home screen displays an empty state message with two actions:
   - "Open PDF" button.
   - "Add Favorite Folder" button.

### Scenario B: Record Recent Document & Resume
1. Tap "Open PDF" and select any PDF document.
2. Read through the first 2 pages in auto-scroll.
3. Tap the system or app Back button to return to the Home Screen.
4. **Verification**:
   - The "Recent Documents" section is now visible.
   - A card for the document shows its title, `Page 2 of N`, and progress percentage.
5. Tap the recent document card.
   - **Verification**: The reader opens immediately, resuming at Page 2 with the saved speed.

### Scenario C: Pin Favorite Folder & Browse Files
1. On the Home Screen, tap **"Add Favorite Folder"** (`+`).
2. Select a folder on the device that contains one or more PDF files.
3. **Verification**:
   - The folder appears under "Favorite Folders" with its name and detected PDF count.
4. Tap the folder card.
   - **Verification**: The folder contents view opens, displaying all PDF files inside that directory.
5. Tap one of the PDF files.
   - **Verification**: The document opens in the reader and is now also listed in Recents.

### Scenario D: Remove Recent and Unpin Folder
1. Long-press or tap the options menu on a recent document and tap "Remove from Recents".
   - **Verification**: Item disappears from Recents immediately.
2. Long-press or tap the delete icon on a favorite folder and tap "Unpin Folder".
   - **Verification**: Folder is unpinned from the Home Screen while leaving files on disk untouched.
