# Data Model & SQLite Schema: Banco de Dados Local (`008-local-sqlite-database`)

**Feature**: `008-local-sqlite-database` | **Database File**: `pdf_reader.db` | **Schema Version (`PRAGMA user_version`)**: `1`

---

## 1. DDL SQLite Oficial (`PRAGMA user_version = 1`)

```sql
CREATE TABLE IF NOT EXISTS pdf_documents (
    id TEXT PRIMARY KEY NOT NULL,
    document_uri TEXT NOT NULL UNIQUE,
    file_name TEXT NOT NULL,
    page_count INTEGER NOT NULL DEFAULT 1 CHECK (page_count >= 1),
    current_page_index INTEGER NOT NULL DEFAULT 0 CHECK (current_page_index >= 0),
    normalized_scroll_ratio REAL NOT NULL DEFAULT 0.0 CHECK (normalized_scroll_ratio >= 0.0 AND normalized_scroll_ratio <= 1.0),
    zoom_scale REAL NOT NULL DEFAULT 1.0 CHECK (zoom_scale >= 0.5 AND zoom_scale <= 4.0),
    preferred_speed_px_per_sec REAL NOT NULL DEFAULT 90.0 CHECK (preferred_speed_px_per_sec >= 10.0 AND preferred_speed_px_per_sec <= 500.0),
    custom_default_speed_px_per_sec REAL NULL CHECK (custom_default_speed_px_per_sec IS NULL OR (custom_default_speed_px_per_sec >= 10.0 AND custom_default_speed_px_per_sec <= 500.0)),
    last_accessed_at INTEGER NOT NULL,
    is_in_recents INTEGER NOT NULL DEFAULT 1 CHECK (is_in_recents IN (0, 1)),
    is_available INTEGER NOT NULL DEFAULT 1 CHECK (is_available IN (0, 1)),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pdf_documents_recents
    ON pdf_documents (is_in_recents, last_accessed_at DESC);

CREATE INDEX IF NOT EXISTS idx_pdf_documents_file_name
    ON pdf_documents (file_name COLLATE NOCASE);

CREATE TABLE IF NOT EXISTS favorite_folders (
    tree_uri TEXT PRIMARY KEY NOT NULL,
    display_name TEXT NOT NULL,
    path_summary TEXT NOT NULL DEFAULT '',
    pinned_timestamp INTEGER NOT NULL,
    cached_file_count INTEGER NOT NULL DEFAULT 0 CHECK (cached_file_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_favorite_folders_pinned
    ON favorite_folders (pinned_timestamp DESC);

CREATE TABLE IF NOT EXISTS app_settings (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT NOT NULL
);
```

### Chaves Reservadas em `app_settings`

| Chave | Valor Padrão | Significado |
|---|---|---|
| `global_default_speed` | `"90.0"` | Velocidade padrão global em px/s (`10.0..500.0`) |
| `datastore_migration_completed` | `"0"` / `"1"` | Garante migração única e idempotente (`RN-08`) |
| `datastore_migration_at` | epoch millis string | Timestamp da conclusão da migração inicial |
| `app_version` | `"1.0.0"` | Versão do app registrada no último export/gravação |
| `last_backup_exported_at` | epoch millis string | Timestamp da última exportação de backup `.db` |

---

## 2. Entidades Kotlin de Domínio e Persistência

### `PdfDocumentRecord` (Novo — `core/model/PdfDocumentRecord.kt`)
```kotlin
data class PdfDocumentRecord(
    val id: String,
    val documentUri: String,
    val fileName: String,
    val pageCount: Int = 1,
    val currentPageIndex: Int = 0,
    val normalizedScrollRatio: Float = 0.0f,
    val zoomScale: Float = 1.0f,
    val preferredSpeedPxPerSec: Float = 90.0f,
    val customDefaultSpeedPxPerSec: Float? = null,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val isInRecents: Boolean = true,
    val isAvailable: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### Invariantes e Clamping (`RN-02`)

| Campo | Tipo | Faixa / Regra | Comportamento ao Sanitizar |
|---|---|---|---|
| `id` | `String` | UUID v4 não vazio | Gerado uma única vez no primeiro cadastro; imutável (`RN-01`) |
| `documentUri` | `String` | Não vazio, único em `pdf_documents` | `require(documentUri.isNotBlank())`; unicidade garantida por índice `UNIQUE` (`RN-05`) |
| `fileName` | `String` | Não vazio | Fallback `"Document.pdf"` se vazio |
| `pageCount` | `Int` | `>= 1` | `.coerceAtLeast(1)` |
| `currentPageIndex` | `Int` | `[0, pageCount - 1]` | `.coerceIn(0, (pageCount - 1).coerceAtLeast(0))` |
| `normalizedScrollRatio` | `Float` | `[0.0f, 1.0f]`, finito | `0.0f` se NaN/Infinito, senão `.coerceIn(0.0f, 1.0f)` |
| `zoomScale` | `Float` | `[0.5f, 4.0f]`, finito | `1.0f` se NaN/Infinito, senão `.coerceIn(0.5f, 4.0f)` |
| `preferredSpeedPxPerSec` | `Float` | `[10.0f, 500.0f]`, finito | `90.0f` se NaN/Infinito, senão `.coerceIn(10.0f, 500.0f)` |
| `customDefaultSpeedPxPerSec` | `Float?` | `null` ou `[10.0f, 500.0f]` | `null` se `null`/NaN/Infinito, senão `.coerceIn(10.0f, 500.0f)` |
| `lastAccessedAt` / `createdAt` / `updatedAt` | `Long` | `> 0L` | `.coerceAtLeast(1L)` |

### Modelos Existentes Atualizados

- **`RecentDocument`** (`core/model/RecentDocument.kt`):
  - Adiciona `val id: String = ""` (preenchido com o UUID da tabela `pdf_documents` pelo `SqliteRecentsRepository`, mantendo default `""` para não quebrar builders legados de teste até remoção).
  - Mantém `uri`, `fileName`, `pageCount`, `lastReadPageIndex`, `progressRatio`, `lastAccessedTimestamp`, `isAvailable`.
- **`ReadingSession`** (`core/model/ReadingSession.kt`):
  - Adiciona `val documentId: String = ""` (mantendo `documentUri`, `currentPageIndex`, `normalizedScrollRatio`, `zoomScale`, `preferredSpeedPxPerSec`, `customDefaultSpeedPxPerSec`, `lastReadTimestamp`, `fileName`).
- **`BackupPayload` & `BackupSummary`** (`core/backup/BackupModels.kt`):
  - `schemaVersion = 1` (correspondente a `PRAGMA user_version = 1`).
  - `documents: List<PdfDocumentRecord>` (substitui ou consolida `readingSessions` + `recentDocuments` preservando `id`, `isInRecents`, `isAvailable` e configurações de rolagem).
  - `BackupSummary`: `exportedAt`, `appVersion`, `globalDefaultSpeedPxPerSec`, `documentsCount` (sessões/PDFs), `customSpeedsCount`, `recentsCount` (`documents.count { it.isInRecents }`), `favoriteFoldersCount`.

---

## 3. Máquina de Estados: Disponibilidade e Revinculação (`RN-03`, `RN-04`, `RN-05`)

```text
[Novo PDF Aberto] ──(insert, UUID gerado)──> [Disponível (is_available=1)]
                                                  │
        (arquivo movido/renomeado/apagado         │
         ou permissão SAF revogada)               │
                                                  ▼
                                        [Indisponível (is_available=0)]
                                          │         │             │
            (Remover "X" — RF-08) ────────┘         │             │ (Restaurar PDF -> Cancelado/Inválido)
                     │                              │             └──────────────────────────┐
                     ▼                              │                                        ▼
            [Excluído do banco]                     │ (Restaurar PDF -> PDF válido)   [Mantido Indisponível]
                                                    ▼
                                     [URI já existe em outro ID?]
                                      ├── Não ──> Atualiza URI/fileName/pageCount,
                                      │           ajusta currentPageIndex = min(saved, newCount-1),
                                      │           preserva id + velocidades, is_available=1 -> Abre Leitor
                                      └── Sim ──> [RelinkConflictDialog]
                                                    ├── Cancelar -> Mantém ambos inalterados
                                                    ├── Abrir existente -> Abre existing.uri
                                                    └── Consolidar neste registro -> Transação:
                                                        remove conflicting.id, atualiza target.id
                                                        com nova URI e is_available=1 -> Abre Leitor
```

---

## 4. Mapeamento da Migração Única DataStore → SQLite (`RF-04`, `RN-08`)

| Origem DataStore | Chave / Formato Legado | Destino SQLite | Regra de Consolidação |
|---|---|---|---|
| `pdf_reader_prefs` | `global_default_speed` (Float) | `app_settings["global_default_speed"]` | Clamped `[10.0, 500.0]`, fallback `90.0` |
| `pdf_reader_prefs` | Prefixos `page_<uri>`, `offset_<uri>`, `zoom_<uri>`, `speed_<uri>`, `custom_default_speed_<uri>`, `time_<uri>`, `filename_<uri>` | `pdf_documents` | Agrupado por `<uri>`; `id = UUID.randomUUID()`; `is_in_recents = 1` se também existir em `recents_prefs`, senão `0` |
| `recents_prefs` | `recent_documents_json` (JSONArray) | `pdf_documents` | Faz merge por `uri` com sessão existente: preserva `fileName`, `pageCount`, `lastAccessedTimestamp`, `isAvailable`; se não houver sessão em `pdf_reader_prefs`, cria linha com velocidades padrão |
| `favorite_folders_prefs` | `favorite_folders_json` (JSONArray) | `favorite_folders` | Deduplicado por `treeUri`, ordenado por `pinnedTimestamp DESC` |
| Flag de controle | *(ausente)* | `app_settings["datastore_migration_completed"] = "1"` | Gravada atomicamente na mesma transação que insere os dados migrados |

> **Nota sobre limite de recentes (`MAX_RECENTS = 30`)**: A lista exibida em `getRecentDocuments()` continua ordenada por `last_accessed_at DESC` com `LIMIT 30` onde `is_in_recents = 1`. Registros além de 30 ou removidos dos recentes apenas (`is_in_recents = 0`) ainda preservam `id` e configurações caso o usuário volte a abrir ou importe no backup, sendo também contabilizados corretamente.
