# Contratos Técnicos e MVI: Banco de Dados Local SQLite (`008-local-sqlite-database`)

**Feature**: `008-local-sqlite-database` | **Date**: 2026-11-26

---

## 1. Camada de Banco (`core/database/`)

```kotlin
interface SqliteCursor : Closeable {
    fun moveToNext(): Boolean
    fun getString(columnIndex: Int): String
    fun getStringOrNull(columnIndex: Int): String?
    fun getInt(columnIndex: Int): Int
    fun getLong(columnIndex: Int): Long
    fun getDouble(columnIndex: Int): Double
    fun getFloatOrNull(columnIndex: Int): Float?
}

interface SqliteConnection : Closeable {
    fun execSQL(sql: String, bindArgs: Array<Any?> = emptyArray())
    fun rawQuery(sql: String, selectionArgs: Array<String?> = emptyArray()): SqliteCursor
    fun <T> inTransaction(exclusive: Boolean = true, block: (SqliteConnection) -> T): T
}

interface SqliteDriver {
    fun <T> withConnection(readOnly: Boolean = false, block: (SqliteConnection) -> T): T
    fun exportDatabaseFileTo(destinationFile: File): Result<Unit>
    fun inspectExternalDatabaseFile(sourceFile: File): Result<SqliteConnection> // ou método de leitura segura
}
```
> **Nota de implementação**: Em Android (`AndroidSqliteOpenHelperDriver`), todas as operações passam pelo `Mutex` e `SQLiteOpenHelper` com `setWriteAheadLoggingEnabled(false)`. Em JVM (`JdbcSqliteDriver`), usa `DriverManager.getConnection("jdbc:sqlite:...")` suportando arquivos reais em `TemporaryFolder` e bancos em disco.

---

## 2. Repositório SQLite Central e Migração (`core/storage/`)

```kotlin
interface PdfDocumentStore : ReadingSessionStore {
    fun observeRecents(limit: Int = 30): Flow<List<PdfDocumentRecord>>
    fun observeAllDocuments(): Flow<List<PdfDocumentRecord>>
    suspend fun getById(id: String): PdfDocumentRecord?
    suspend fun getByUri(documentUri: String): PdfDocumentRecord?
    suspend fun registerOrOpenDocument(
        uri: String,
        fileName: String,
        pageCount: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): PdfDocumentRecord

    suspend fun updateReadingProgress(
        documentId: String,
        currentPageIndex: Int,
        normalizedScrollRatio: Float,
        zoomScale: Float,
        preferredSpeedPxPerSec: Float,
        nowMillis: Long = System.currentTimeMillis()
    )

    suspend fun setDocumentAvailability(documentUri: String, isAvailable: Boolean)
    suspend fun updateAvailabilityBatch(updates: Map<String, Boolean>)
    suspend fun relinkDocument(
        targetDocumentId: String,
        newUri: String,
        newFileName: String,
        newPageCount: Int,
        consolidateConflictingDocumentId: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Result<PdfDocumentRecord>

    suspend fun removeFromRecents(documentIdOrUri: String, deleteIfUnavailable: Boolean = true)
    suspend fun clearAllRecents()
    suspend fun isDataStoreMigrationCompleted(): Boolean
    suspend fun importLegacySnapshotOnce(snapshot: LegacyMigrationSnapshot)
    suspend fun getFavoriteFoldersSnapshot(): List<FavoriteFolder>
    suspend fun observeFavoriteFolders(): Flow<List<FavoriteFolder>>
    suspend fun upsertFavoriteFolder(folder: FavoriteFolder)
    suspend fun removeFavoriteFolder(treeUri: String)
    suspend fun replaceDatabaseSnapshot(
        globalDefaultSpeedPxPerSec: Float,
        documents: List<PdfDocumentRecord>,
        favoriteFolders: List<FavoriteFolder>,
        nowMillis: Long = System.currentTimeMillis()
    )
}
```

### Migração Legada (`LegacyDataStoreMigrator`)
```kotlin
data class LegacyMigrationSnapshot(
    val hasAnyLegacyData: Boolean,
    val globalDefaultSpeedPxPerSec: Float,
    val sessions: List<ReadingSession>,
    val recents: List<RecentDocument>,
    val favoriteFolders: List<FavoriteFolder>
)

interface LegacyDataStoreReader {
    suspend fun readSnapshot(): LegacyMigrationSnapshot
}

class LegacyDataStoreMigrator(
    private val legacyReader: LegacyDataStoreReader,
    private val documentStore: PdfDocumentStore,
    private val uuidGenerator: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun migrateIfNeeded(): MigrationOutcome
}
```

---

## 3. Verificação de Disponibilidade & Política de Revinculação (`core/documents/`)

```kotlin
data class InspectedPdfMetadata(
    val uri: String,
    val fileName: String,
    val pageCount: Int
)

sealed interface RelinkOutcome {
    data class Relinked(val record: PdfDocumentRecord) : RelinkOutcome
    data class UriConflict(
        val targetRecord: PdfDocumentRecord,
        val conflictingRecord: PdfDocumentRecord,
        val newMetadata: InspectedPdfMetadata
    ) : RelinkOutcome
    data class InvalidTarget(val message: String) : RelinkOutcome
}

interface DocumentAvailabilityChecker {
    suspend fun isReadable(uriString: String): Boolean
    suspend fun inspectPdf(uriString: String): Result<InspectedPdfMetadata>
    suspend fun takePersistableReadPermission(uriString: String)
}

object DocumentRelinkPolicy {
    fun evaluateRelink(
        target: PdfDocumentRecord,
        newMetadata: InspectedPdfMetadata,
        existingByNewUri: PdfDocumentRecord?,
        nowMillis: Long
    ): RelinkOutcome

    fun adjustPageIndexForNewPageCount(savedPageIndex: Int, newPageCount: Int): Int =
        savedPageIndex.coerceIn(0, (newPageCount.coerceAtLeast(1) - 1))

    fun adjustProgressRatio(adjustedPageIndex: Int, newPageCount: Int, savedRatio: Float): Float
}
```

---

## 4. Backup e Restauração SQLite `.db` (`core/backup/`)

```kotlin
interface BackupFileGateway {
    suspend fun writeFileToUri(uriString: String, sourceFile: File): Result<Unit>
    suspend fun copyUriToTempFile(uriString: String, destinationFile: File, maxBytes: Long = MAX_BACKUP_BYTES): Result<Unit>
}

sealed interface BackupParseResult {
    data class Success(val payload: BackupPayload, val summary: BackupSummary) : BackupParseResult
    data class Failure(val reason: BackupErrorReason, val message: String) : BackupParseResult
}

enum class BackupErrorReason {
    EMPTY_FILE,
    INVALID_SQLITE_FILE,
    CORRUPTED_DATABASE,
    UNSUPPORTED_SCHEMA_VERSION,
    INVALID_STRUCTURE,
    IO_ERROR
}
```
- **Nome sugerido ao exportar**: `pdf-reader-backup-YYYYMMDD-HHmm.db` (`suggestedBackupFileName()`).
- **MIME ao criar documento (`MainActivity`)**: `ActivityResultContracts.CreateDocument("application/octet-stream")`.
- **MIME ao abrir documento (`MainActivity`)**: `arrayOf("application/x-sqlite3", "application/vnd.sqlite3", "application/octet-stream", "*/*")`.
- **Limite máximo do backup**: `20 * 1024 * 1024` bytes (`20 MB`).

---

## 5. Contratos MVI Atualizados

### Home (`ui/home/`)
- **`HomeIntent`**:
  - Existentes preservados (`Refresh`, `OpenDocument`, `RemoveRecent`, `ClearAllRecents`, `AddFolder`, `RemoveFolder`, `SelectFolder`, `OpenPdfFromFolder`, `DismissError`, `OpenBackupDialog`, `DismissBackupDialog`, `ExportBackupToUri`, `InspectBackupUri`, `SelectRestoreMode`, `ConfirmRestore`, `DismissRestorePreview`).
  - Novos intents:
    - `data class RequestRelinkPdf(val documentId: String) : HomeIntent`
    - `data class CompleteRelinkPdf(val documentId: String, val newUri: Uri) : HomeIntent`
    - `data object ConfirmConsolidateRelinkConflict : HomeIntent`
    - `data object OpenExistingFromRelinkConflict : HomeIntent`
    - `data object DismissRelinkConflict : HomeIntent`
- **`HomeSideEffect`**:
  - `data class NavigateToReader(val uri: Uri) : HomeSideEffect`
  - `data class ShowToast(val message: String, val isLong: Boolean = false) : HomeSideEffect`
  - `data class LaunchRelinkPdfPicker(val documentId: String) : HomeSideEffect`
- **`HomeViewState`**:
  - Adiciona `val relinkConflict: RelinkConflictState? = null` (`targetRecord`, `conflictingRecord`, `newFileName`, `newUri`, `newPageCount`).
- **Regra no `HomeViewModel.openDocument(uri)`**:
  - Se o documento existir nos recentes com `isAvailable == false` (ou o checker reportar ilegível), marca `isAvailable = false` e dispara o fluxo de indisponibilidade/opção **"Restaurar PDF"** em vez de abrir cegamente ou causar crash.

### Reader (`ui/reader/`)
- **`ReaderViewState`**:
  - Adiciona `val activeDocumentId: String? = null` e `val unavailableDocumentId: String? = null`.
- **`ReaderIntent`**:
  - Adiciona `data object RequestRelinkCurrentDocument : ReaderIntent` e `data class CompleteRelinkCurrentDocument(val newUri: Uri) : ReaderIntent`.
- **`ReaderSideEffect`**:
  - Adiciona `data class LaunchRelinkPdfPicker(val documentId: String) : ReaderSideEffect`.

---

## 6. Contratos de UI / Textos Neo-Brutalism (Sem Menção a JSON — `RF-13`, `RNF-06`, `CA-13`)

| Componente | Contrato Visual e de Texto |
|---|---|
| `BackupActionsDialog` | Descrição: `"Salve ou restaure localmente o banco de dados SQLite com suas velocidades por PDF, sessões de leitura, recentes e pastas favoritas."` <br/> Botão 1: `"Exportar backup (.db)"` (`accentYellow`) <br/> Botão 2: `"Importar backup (.db)"` (`surface`) |
| `RestorePreviewDialog` | Título: `"Confirmar restauração"` <br/> Resumo: velocidade global, PDFs/sessões, velocidades personalizadas, recentes, pastas favoritas <br/> Modos: `"Mesclar (padrão)"` e `"Substituir tudo"` (sem mencionar JSON) |
| `RecentDocumentCard` | Quando `document.isAvailable == true`: card atual inalterado. <br/> Quando `document.isAvailable == false`: badge Neo-Brutalism `"Arquivo indisponível"` (`accentRed`/`onAccent`), card permanece visível com nome/progresso salvos, botão `"Restaurar PDF"` (`accentYellow`/`onAccent`, altura mínima acessível) e botão `X` para remoção explícita (`RF-08`). |
| `ErrorDialog` | Quando `errorState is ReaderErrorState.FileNotFound` e existir `unavailableDocumentId != null`: exibe ação primária `"Restaurar PDF"` (`accentYellow`) ao lado de `"Fechar"` e `"Escolher outro arquivo"`. |
| `RelinkConflictDialog` | Título: `"Arquivo já cadastrado"` <br/> Explica que a URI selecionada pertence a outro registro cadastrado (`conflictingRecord.fileName`) e oferece: `"Cancelar"`, `"Abrir existente"` e `"Consolidar neste registro"` (preservando `id` e configurações do documento restaurado). |
