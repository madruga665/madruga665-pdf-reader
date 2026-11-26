# Contract: Backup JSON Schema v1

**Media Type**: `application/json` (UTF-8)
**Suggested Filename**: `pdf-reader-backup-YYYYMMDD-HHmm.json`
**Schema Version**: `1`

## JSON Structure

```json
{
  "schemaVersion": 1,
  "exportedAt": 1764100000000,
  "appVersion": "1.0.0",
  "preferences": {
    "globalDefaultSpeedPxPerSec": 120.0,
    "readingSessions": [
      {
        "documentUri": "content://com.android.externalstorage.documents/document/primary%3ABooks%2Fsample.pdf",
        "fileName": "sample.pdf",
        "currentPageIndex": 14,
        "normalizedScrollRatio": 0.42,
        "zoomScale": 1.25,
        "preferredSpeedPxPerSec": 150.0,
        "customDefaultSpeedPxPerSec": 150.0,
        "lastReadTimestamp": 1764099000000
      }
    ]
  },
  "recentDocuments": [
    {
      "uri": "content://com.android.externalstorage.documents/document/primary%3ABooks%2Fsample.pdf",
      "fileName": "sample.pdf",
      "pageCount": 120,
      "lastReadPageIndex": 14,
      "progressRatio": 0.42,
      "lastAccessedTimestamp": 1764099000000,
      "isAvailable": true
    }
  ],
  "favoriteFolders": [
    {
      "treeUri": "content://com.android.externalstorage.documents/tree/primary%3ABooks",
      "displayName": "Books",
      "pathSummary": "primary:Books",
      "pinnedTimestamp": 1764098000000,
      "cachedFileCount": 12
    }
  ]
}
```

## Contract Invariants

1. `schemaVersion` is mandatory and MUST equal `1` for this version of the application.
2. `preferences` MUST be a JSON object; `preferences.readingSessions`, `recentDocuments`, and `favoriteFolders` MUST be JSON arrays (may be empty).
3. `customDefaultSpeedPxPerSec` is omitted or `null` when a document does not have a saved custom default speed.
4. Any file failing structural or version validation MUST be rejected before any DataStore mutation occurs.
