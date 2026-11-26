# Implementation Plan: Banco de Dados Local SQLite, Revinculação de PDF e Backup `.db`

**Branch**: `008-local-sqlite-database` | **Date**: 2026-11-26 | **Spec**: [spec.md](spec.md)  
**Harness**: `agy` | **Porte**: `completa`  
**Input**: PRD "Banco de Dados Local (madruga665-pdf-reader)" e especificação `/specs/008-local-sqlite-database/spec.md`

---

## Summary

Substituir os três armazenamentos separados em DataStore Preferences (`pdf_reader_prefs`, `recents_prefs`, `favorite_folders_prefs`) por um banco de dados local SQLite único (`pdf_reader.db`, `PRAGMA user_version = 1`) construído sobre o SQLite nativo do Android SDK (`SQLiteOpenHelper` / `SQLiteDatabase`) encapsulado atrás da abstração testável `SqliteDriver`. Essa escolha elimina riscos de incompatibilidade entre Room/KSP e Kotlin `2.4.0`, mantém zero dependências novas de runtime e permite testar o schema real, constraints, migração única do DataStore, revinculação e backup `.db` na JVM via `org.xerial:sqlite-jdbc` (`testImplementation`).

Além da migração para SQLite, o plano entrega:
1. Migração automática, única e idempotente dos dados já existentes no DataStore Preferences na primeira abertura.
2. Detecção não-destrutiva de PDFs indisponíveis (inclusive via `ON_RESUME` e erro de abertura no leitor) e fluxo completo de **"Restaurar PDF"** com preservação do `id` UUID v4, ajuste seguro de página, tratamento de cancelamento/erro e resolução explícita de conflito de URI (`RelinkConflictDialog`).
3. Correção do bug pré-existente em `PdfDocumentViewport.kt` que reinicializava a lista na página `0` e sobrescrevia a página/zoom restaurados da sessão.
4. Backup e restauração local operando diretamente com arquivos SQLite `.db` (exportação compatível com API 26+ sem `VACUUM INTO`, validação estrita em staging read-only, prévia com contagens, modos `Mesclar` e `Substituir` em transação atômica, e remoção integral de menções a JSON na UI).

---

## Technical Context

**Language/Version**: Kotlin `2.4.0` (JVM target `17` via DSL `compilerOptions`; Gradle Daemon em JDK `21`)  
**Primary Dependencies**: Jetpack Compose (BOM `2024.11.00`, Material 3, Adaptive `WindowWidthSizeClass`), AndroidX `documentfile:1.0.1`, AndroidX `datastore-preferences:1.1.1` (mantido apenas para leitura única de migração legada), Coroutines `1.9.0`, `android.database.sqlite.SQLiteOpenHelper` (nativo Android SDK)  
**Testing**: JUnit `4.13.2`, `kotlinx-coroutines-test`, Turbine `1.2.0`, MockK `1.13.13`, **`org.xerial:sqlite-jdbc:3.47.1.0` (`testImplementation` apenas)** para testes reais de DDL/SQL/arquivos `.db` na JVM via `./gradlew testDebugUnitTest`  
**Target Platform**: Android (`minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`, `versionName = "1.0.0"`)  
**Project Type**: Native Android app (single module `:app`, Clean Architecture + MVI)  
**Git Base Note**: No momento da criação desta spec, a branch local `main` está em `f2242fe` (1 commit atrás de `origin/main` `443b2ef docs: add README (#2)`). O implementador `agy` deve atualizar/partir de `origin/main` antes de criar a branch `008-local-sqlite-database`.  
**Performance Goals**: Todas as operações de SQLite, verificação de disponibilidade SAF, migração, exportação e importação ocorrem em `Dispatchers.IO` sob `Mutex`, preservando rolagem a 60fps no leitor (`RNF-01`).  
**Constraints**: 100% offline/local (`RNF-03`); compatível com API 26 (`minSdk = 26` — sem `VACUUM INTO`); transacional e atômico na restauração (`RNF-02`, `RN-07`); nenhum uso de texto "JSON" na UI de backup (`RF-13`).

---

## Constitution Check

*GATE: Verificado contra `.specify/memory/constitution.md`.*

- **I. Smooth & Predictable Rendering (Performance First)**: PASS — Todas as consultas/gravações SQLite, verificações de descritores SAF e cópias `.db` correm em `Dispatchers.IO`; o loop `withFrameNanos` do leitor continua sem I/O na main thread.
- **II. Modular & Engine-Agnostic Core (Library-First)**: PASS — Regras puras (`DocumentRelinkPolicy`, `BackupMerger`, `BackupPayloadValidator`, `LegacyMigrationAssembler`, modelos de domínio) e toda a camada SQL (`SqliteDriver`, `SqlitePdfDocumentStore`, `SqliteBackupValidator`) permanecem desacopladas de Views/Compose e testáveis na JVM.
- **III. Test-First & Quality Gates (NON-NEGOTIABLE)**: PASS — Testes unitários JVM (incluindo banco SQLite real via `sqlite-jdbc`) são escritos antes da implementação em cada história e validados com `./gradlew testDebugUnitTest` e `./gradlew assembleDebug`.
- **IV. Responsive & Accessible Reader Experience**: PASS — Novos estados em `RecentDocumentCard`, `RelinkConflictDialog`, `ErrorDialog` e `BackupDialogs` usam tokens `NeobrutalismTheme.colors` (Claro + Catppuccin Mocha Escuro) e funcionam em `WindowWidthSizeClass.Compact` e `Expanded`.
- **V. Observability & Graceful Error Recovery**: PASS — `SecurityException`, `FileNotFoundException`, PDFs corrompidos, backups `.json`/inválidos/corrompidos e erros de E/S são capturados sem crash e sem corromper o banco local.

---

## Project Structure

### Documentation (this feature)

```text
specs/008-local-sqlite-database/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── checklists/
│   └── requirements.md
├── contracts/
│   └── sqlite-storage-and-backup-contract.md
└── tasks.md
```

### Source Code (repository root)

#### Arquivos Novos
```text
app/src/main/java/com/madruga665/pdfreader/core/
├── model/
│   └── PdfDocumentRecord.kt                          # Entidade central do banco (id UUID v4, uri, fileName, leitura, flags)
├── database/
│   ├── SqliteDriver.kt                               # Abstração portátil de banco + transações para Android e JVM tests
│   ├── PdfReaderDatabaseSchema.kt                    # DDL v1, PRAGMA user_version=1, índices e constantes de validação
│   ├── AndroidSqliteOpenHelperDriver.kt              # Implementação Android SDK (SQLiteOpenHelper, journal DELETE, Mutex)
│   └── SqliteAppDatabase.kt                          # Fachada de ciclo de vida, checkpoint e acesso ao arquivo físico .db
├── storage/
│   ├── SqlitePdfDocumentStore.kt                     # CRUD por id/uri, settings globais, transações e substituição/mesclagem
│   ├── LegacyDataStoreReader.kt                      # Interface + leitor read-only dos 3 DataStores legados
│   └── LegacyDataStoreMigrator.kt                    # Migração única, idempotente e transacional DataStore -> SQLite
├── documents/
│   ├── DocumentAvailabilityChecker.kt                # Verificação SAF + inspeção PdfRenderer (pageCount, fileName) fora da UI
│   └── DocumentRelinkPolicy.kt                       # Regras puras de revinculação (ajuste página RN-04, conflito URI RN-05)
├── home/
│   ├── SqliteRecentsRepository.kt                    # Implementação de RecentsRepository apoiada em PdfDocumentStore
│   └── SqliteFavoriteFoldersRepository.kt            # Implementação de FavoriteFoldersRepository apoiada no SQLite + SAF scan
└── backup/
    ├── SqliteBackupValidator.kt                      # Validação de header SQLite, integrity_check, user_version, schema e resumo
    └── SqliteBackupRepository.kt                     # Exportação/prévia/restauração atômica .db via staging e BackupFileGateway

app/src/test/java/com/madruga665/pdfreader/core/
├── database/
│   ├── JdbcSqliteDriver.kt                           # Driver SQLite JVM usando org.xerial:sqlite-jdbc para testes reais
│   └── PdfReaderDatabaseSchemaTest.kt                # Valida DDL, CHECK constraints, UNIQUE(document_uri) e índices
├── storage/
│   ├── SqlitePdfDocumentStoreTest.kt                 # CRUD, imutabilidade de id, RN-02 clamping, custom speed preservation
│   └── LegacyDataStoreMigratorTest.kt                # Migração dos 3 DataStores, idempotência e casos parciais/corrompidos
├── documents/
│   └── DocumentRelinkPolicyTest.kt                   # Ajuste seguro de página, conflito URI, consolidação e preservação de id
├── home/
│   ├── SqliteRecentsRepositoryTest.kt                # Recentes, disponibilidade, remoção de indisponível vs disponível
│   └── SqliteFavoriteFoldersRepositoryTest.kt        # CRUD pastas favoritas + scan SAF mockado
└── backup/
    ├── SqliteBackupValidatorTest.kt                  # Rejeição de vazio/JSON/texto/corrompido/schema inválido + contagem real
    └── SqliteBackupRepositoryTest.kt                 # Exportação .db, preview, REPLACE, MERGE e rollback atômico
```

#### Arquivos Modificados
```text
gradle/libs.versions.toml                             # Adicionar sqliteJdbc ("3.47.1.0")
app/build.gradle.kts                                  # Adicionar testImplementation(libs.sqlite.jdbc); remover libs.json se sem uso
app/src/main/java/com/madruga665/pdfreader/
├── MainActivity.kt                                   # Instanciar SqliteAppDatabase, rodar migração inicial, revalidar no ON_RESUME, CreateDocument("application/octet-stream") + OpenDocument para .db, launcher de "Restaurar PDF"
├── core/model/
│   ├── ReadingSession.kt                             # Incluir documentId opcional/compatível e manter validações RN-02
│   └── RecentDocument.kt                             # Incluir id (UUID v4) mantendo compatibilidade de criação em testes
├── core/storage/
│   └── ReadingSessionStore.kt                        # Adicionar operações por documentId / resolução de ID estável
├── core/home/
│   └── RecentsRepository.kt                          # Suportar atualização de disponibilidade e consulta por id/uri
├── core/backup/
│   ├── BackupModels.kt                               # Atualizar BackupErrorReason (INVALID_SQLITE_FILE etc.) e payload com IDs
│   ├── BackupFileGateway.kt                          # Streaming binário/staging (copyToUri / copyFromUriToTempFile)
│   ├── AndroidSafBackupFileGateway.kt                # Cópia por stream binário segura até MAX_BACKUP_BYTES = 20 MB
│   ├── BackupMerger.kt                               # Mesclagem determinística por id e document_uri sem duplicação
│   └── BackupRepository.kt                           # Contrato compatível para exportação/prévia/restauração .db
├── ui/home/
│   ├── HomeIntent.kt                                 # Ações StartRelinkPdf, ConfirmRelinkUri, ResolveRelinkConflict, CancelRelink
│   ├── HomeViewState.kt                              # Estado pendingRelinkDocumentId, relinkConflictState
│   ├── HomeSideEffect.kt                             # LaunchRelinkPdfPicker(documentId), NavigateToReader, ShowToast
│   ├── HomeViewModel.kt                              # Revalidação de disponibilidade, revinculação, conflito de URI, mensagens sem JSON
│   └── HomeScreen.kt                                 # Conectar botão "Restaurar PDF" no RecentDocumentCard e RelinkConflictDialog
├── ui/reader/
│   ├── ReaderIntent.kt                               # Ação RelinkCurrentDocument / confirmação com nova URI
│   ├── ReaderViewState.kt                            # activeDocumentId, failedDocumentId/failedDocumentUri para restaurar do leitor
│   └── ReaderViewModel.kt                            # Cadastro/leitura por ID estável, remoção de fallback por filename, suporte a Restaurar PDF e captura de FileNotFound/SecurityException
└── ui/components/
    ├── RecentDocumentCard.kt                         # Estado indisponível (badge Neo-Brutalism + botão "Restaurar PDF")
    ├── ErrorDialog.kt                                # Botão "Restaurar PDF" quando erro vier de documento cadastrado indisponível
    ├── BackupDialogs.kt                              # Textos e botões atualizados para SQLite (.db) sem menção a JSON + RelinkConflictDialog
    └── PdfDocumentViewport.kt                        # CORREÇÃO BUG: inicializar scroll na página salva e zoom salvo sem sobrescrever com 0
```

#### Arquivos Legados Descontinuados / Removidos após Migração
```text
app/src/main/java/com/madruga665/pdfreader/core/backup/BackupJsonCodec.kt
app/src/main/java/com/madruga665/pdfreader/core/backup/FilenameSpeedReconciler.kt
app/src/main/java/com/madruga665/pdfreader/core/storage/DataStoreReadingSessionStore.kt   # Substituído por SqlitePdfDocumentStore + LegacyDataStoreReader
app/src/main/java/com/madruga665/pdfreader/core/home/DataStoreRecentsRepository.kt         # Substituído por SqliteRecentsRepository
app/src/main/java/com/madruga665/pdfreader/core/home/AndroidFavoriteFoldersRepository.kt   # Substituído por SqliteFavoriteFoldersRepository
app/src/test/java/com/madruga665/pdfreader/core/backup/BackupJsonCodecTest.kt
app/src/test/java/com/madruga665/pdfreader/core/backup/FilenameSpeedReconcilerTest.kt
app/src/test/java/com/madruga665/pdfreader/core/storage/DataStoreReadingSessionStoreTest.kt
app/src/test/java/com/madruga665/pdfreader/core/home/DataStoreRecentsRepositoryTest.kt
app/src/test/java/com/madruga665/pdfreader/core/home/AndroidFavoriteFoldersRepositoryTest.kt
```
> **Nota**: Enquanto os testes de migração (`LegacyDataStoreMigratorTest`) precisarem ler os DataStores legados, `LegacyDataStoreReader` preserva as exatamente mesmas chaves (`pdf_reader_prefs`, `recents_prefs`, `favorite_folders_prefs`, `global_default_speed`, `recent_documents_json`, `favorite_folders_json`, prefixos `page_`, `offset_`, `zoom_`, `speed_`, `custom_default_speed_`, `time_`, `filename_`) para garantir compatibilidade 100% com instalações existentes. `AndroidSafBackupFileGatewayTest`, `BackupMergerTest`, `HomeViewModelTest` e `ReaderViewModelTest` são atualizados para os novos contratos.

---

## Armadilhas Técnicas Críticas a Evitar

1. **NUNCA usar `VACUUM INTO` na exportação**: `minSdk = 26` roda SQLite `3.19.x`; `VACUUM INTO` exige SQLite `3.27.0` (Android 11 / API 30) e lançaria `SQLiteException` em Android 8–10. Usar `setWriteAheadLoggingEnabled(false)` (`journal_mode = DELETE`), sincronização por `Mutex`, transação explícita de metadados de exportação e cópia para arquivo de staging validado com `PRAGMA integrity_check == "ok"`.
2. **NUNCA substituir o arquivo `.db` aberto em disco durante a restauração**: Substituir o arquivo do banco enquanto `SQLiteOpenHelper` mantém conexões abertas corrompe o estado interno do helper. Em vez disso, inspecionar o backup em staging `OPEN_READONLY`, ler os dados validados e aplicar `REPLACE` ou `MERGE` dentro de **uma única transação SQLite (`BEGIN IMMEDIATE ... COMMIT`)** na conexão gerenciada.
3. **Bug pré-existente em `PdfDocumentViewport.kt`**: Hoje `rememberLazyListState()` sempre inicia no item `0`, e `LaunchedEffect(firstVisibleItemIndex)` dispara `onPageChanged(0)` logo na montagem, sobrescrevendo no banco a página salva da sessão! O viewport deve receber `initialPageIndex` e `initialZoomScale`, inicializar `rememberLazyListState(initialFirstVisibleItemIndex = initialPageIndex.coerceIn(0, (document.pageCount - 1).coerceAtLeast(0)))` com chave ligada ao `document.uri` + sessão carregada, e sincronizar zoom com `ReaderIntent.SetZoomScale`.
4. **`saveCurrentProgress()` não pode apagar velocidade personalizada**: Manter `custom_default_speed_px_per_sec` preservado em atualizações de página/progresso/zoom (atualizar apenas colunas de progresso, `preferred_speed_px_per_sec` e `last_accessed_at`, nunca sobrescrevendo `custom_default_speed_px_per_sec` com `NULL`).
5. **Re-execução acidental da migração pós-restore**: Se um backup antigo ou editado não tiver `datastore_migration_completed`, uma restauração em modo `REPLACE` não pode limpar essa flag e fazer o próximo boot importar dados antigos do DataStore. Toda transação de `restoreBackup` deve assegurar `datastore_migration_completed = "1"`.
6. **Zero menções a JSON na UI (`RF-13` / `CA-13`)**: Verificar com `rg -i json app/src/main` após concluir a implementação que nenhuma string de UI, diálogo ou toast menciona "JSON" ou `.json`.
