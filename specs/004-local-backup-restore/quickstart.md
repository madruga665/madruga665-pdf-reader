# Quickstart Validation Guide: Local Backup & Restore

## Automated Verification (JVM Unit Tests & APK Build)

```bash
# Run full JVM unit test suite (including core/backup, storage, recents, folders, HomeViewModel, ReaderViewModel)
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
```

## Manual End-to-End Validation Checklist (Device / Emulator)

1. **Empty State Access**: Launch fresh install (`HomeEmptyState`). Tap the "Backup e Restauração" icon in the TopAppBar and verify both "Exportar backup" and "Importar backup" options are available.
2. **Export Backup**: Open a PDF, save a custom speed (e.g., `165 px/s`), pin a favorite folder, return to Home, tap "Backup e Restauração" → "Exportar backup", confirm the suggested filename `pdf-reader-backup-YYYYMMDD-HHmm.json`, save, and verify the summary feedback message appears. Cancel once to confirm silent cancellation.
3. **Import Preview & Merge/Replace**: Modify local data (or clear app data), tap "Backup e Restauração" → "Importar backup", select the exported `.json`, verify the preview dialog shows accurate counts and defaults to **Mesclar**, test both **Mesclar** and **Substituir tudo**, and verify the Home screen refreshes automatically.
4. **SAF Resilience & Filename Reconciliation**: After clearing app data and restoring the backup, tap a restored recent/folder to verify graceful error handling (no crash). Then open the same PDF via "Open PDF" (generating a new SAF URI) and verify the custom speed (`165 px/s`) is automatically restored and bound to the new URI.
5. **Invalid Backup Rejection**: Attempt to import an empty file, invalid JSON, and a JSON with `"schemaVersion": 99`; verify each shows a clear error and leaves existing data unchanged.
