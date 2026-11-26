# Tasks: Banco de Dados Local SQLite, Revinculação de PDF e Backup `.db`

**Input**: Design documents from `/specs/008-local-sqlite-database/`  
**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/sqlite-storage-and-backup-contract.md`  
**Methodology**: TDD (escrever e verificar falha do teste antes da implementação em cada tarefa); respeitar `RNF-01`..`RNF-06`.

---

## Phase 1: Setup & Branching

- [x] T001 Sincronizar repositório com `origin/main` e criar a branch de trabalho `008-local-sqlite-database` sem alterar a `main`
- [x] T002 Adicionar `org.xerial:sqlite-jdbc:3.47.1.0` em `gradle/libs.versions.toml` e `testImplementation(libs.sqlite.jdbc)` em `app/build.gradle.kts`
- [x] T003 [P] Atualizar modelos `PdfDocumentRecord.kt`, `ReadingSession.kt` e `RecentDocument.kt` em `app/src/main/java/com/madruga665/pdfreader/core/model/` com `id` estável e constantes/validações `RN-02`

---

## Phase 2: Foundational — Camada SQLite Nativa + Driver de Testes JVM

**Purpose**: Infraestrutura SQLite compartilhada por todas as histórias (`PRAGMA user_version = 1`, `journal_mode = DELETE`, `Mutex`, `JdbcSqliteDriver` para testes JVM reais).

- [x] T004 [P] Criar interfaces `SqliteDriver`, `SqliteConnection`, `SqliteCursor` em `app/src/main/java/com/madruga665/pdfreader/core/database/SqliteDriver.kt` e constantes DDL em `PdfReaderDatabaseSchema.kt`
- [x] T005 [P] Implementar `JdbcSqliteDriver.kt` em `app/src/test/java/com/madruga665/pdfreader/core/database/JdbcSqliteDriver.kt` e escrever `PdfReaderDatabaseSchemaTest.kt` validando DDL, `CHECK`, `UNIQUE(document_uri)` e `PRAGMA user_version = 1`
- [x] T006 Implementar `AndroidSqliteOpenHelperDriver.kt` e `SqliteAppDatabase.kt` em `app/src/main/java/com/madruga665/pdfreader/core/database/` (com `setWriteAheadLoggingEnabled(false)`, `Mutex`, checkpoint defensivo e geração segura de cópia para staging)

**Checkpoint**: `./gradlew testDebugUnitTest --tests "*.PdfReaderDatabaseSchemaTest"` verde com SQLite real na JVM.

---

## Phase 3: User Story 1 — Cadastro com ID Único e Persistência Unificada no SQLite (Priority: P1) 🎯 MVP

**Goal**: Substituir `DataStoreReadingSessionStore`, `DataStoreRecentsRepository` e `AndroidFavoriteFoldersRepository` por implementações SQLite usando `id` único imutável e corrigir o bug de restauração de página/zoom no viewport.

**Independent Test**: `SqlitePdfDocumentStoreTest`, `SqliteRecentsRepositoryTest`, `SqliteFavoriteFoldersRepositoryTest` e `ReaderViewModelTest`.

### Tests for User Story 1 (TDD First)

- [x] T007 [P] [US1] Escrever `SqlitePdfDocumentStoreTest.kt` em `app/src/test/java/com/madruga665/pdfreader/core/storage/` cobrindo criação com UUID único, reabertura sem duplicação (`CA-01`), clamping `RN-02`, preservação de `custom_default_speed_px_per_sec` ao atualizar progresso (`CA-02`), reset para padrão global e `app_settings`
- [x] T008 [P] [US1] Escrever `SqliteRecentsRepositoryTest.kt` e `SqliteFavoriteFoldersRepositoryTest.kt` em `app/src/test/java/com/madruga665/pdfreader/core/home/`
- [x] T009 [P] [US1] Atualizar `ReaderViewModelTest.kt` para verificar persistência via `documentId`, restauração de página/zoom/velocidade e ausência de reconciliação implícita por nome de arquivo (`RN-01`)

### Implementation for User Story 1

- [x] T010 [US1] Implementar `SqlitePdfDocumentStore.kt` em `app/src/main/java/com/madruga665/pdfreader/core/storage/` estendendo `ReadingSessionStore`
- [x] T011 [P] [US1] Implementar `SqliteRecentsRepository.kt` e `SqliteFavoriteFoldersRepository.kt` em `app/src/main/java/com/madruga665/pdfreader/core/home/`
- [x] T012 [US1] Atualizar `ReaderViewModel.kt`, `ReaderViewState.kt` e `ReaderIntent.kt` para operar com `activeDocumentId` estável e remover chamada a `FilenameSpeedReconciler`
- [x] T013 [US1] Corrigir `PdfDocumentViewport.kt` e `AdaptiveReaderScaffold.kt` para inicializar `LazyListState` na página restaurada (`currentPageIndex`) e `zoomScale` da sessão sem sobrescrever com página `0` na montagem (`CA-02`)

**Checkpoint**: Cadastro único por PDF, persistência contínua de página/zoom/velocidade, recentes e pastas favoritas 100% funcionais no SQLite.

---

## Phase 4: User Story 2 — Migração Automática, Única e Idempotente do DataStore (Priority: P1)

**Goal**: Importar automaticamente `pdf_reader_prefs`, `recents_prefs` e `favorite_folders_prefs` na primeira execução após atualização, uma única vez (`RF-04`, `RN-08`, `CA-03`).

**Independent Test**: `LegacyDataStoreMigratorTest` com `PreferenceDataStoreFactory` real + `JdbcSqliteDriver`.

### Tests for User Story 2 (TDD First)

- [x] T014 [US2] Escrever `LegacyDataStoreMigratorTest.kt` em `app/src/test/java/com/madruga665/pdfreader/core/storage/` cobrindo migração completa dos 3 DataStores, atribuição de UUID único, tolerância a JSON legado parcial/corrompido e idempotência em execuções repetidas (`CA-03`, `RN-08`)

### Implementation for User Story 2

- [x] T015 [US2] Implementar `LegacyDataStoreReader.kt` (lendo somente as chaves legadas dos 3 DataStores) e `LegacyDataStoreMigrator.kt` em `app/src/main/java/com/madruga665/pdfreader/core/storage/` executando em transação única e gravando `datastore_migration_completed = "1"`
- [ ] T016 [US2] Integrar execução da migração única na inicialização em `MainActivity.kt` antes de expor fluxos de leitura às telas

**Checkpoint**: Dados legados migrados automaticamente e sem duplicação entre reinicializações.

---

## Phase 5: User Story 3 — Detecção de Indisponibilidade, "Restaurar PDF" e Conflito de URI (Priority: P1)

**Goal**: Detectar PDFs indisponíveis sem crash, permitir revincular via **"Restaurar PDF"** preservando `id` e velocidades com ajuste seguro de página, tratar cancelamento/erro/conflito de URI e permitir remoção explícita (`RF-05`..`RF-08`, `RN-03`..`RN-05`, `CA-04`..`CA-07`).

**Independent Test**: `DocumentRelinkPolicyTest`, `HomeViewModelTest` e `ReaderViewModelTest`.

### Tests for User Story 3 (TDD First)

- [ ] T017 [P] [US3] Escrever `DocumentRelinkPolicyTest.kt` em `app/src/test/java/com/madruga665/pdfreader/core/documents/` validando preservação de `id`, ajuste seguro de página (`RN-04`: `savedPage = 5`, `newPageCount = 3` -> `2`), detecção de conflito de URI (`RN-05`) e consolidação
- [ ] T018 [P] [US3] Atualizar `HomeViewModelTest.kt` cobrindo detecção de indisponibilidade, `RequestRelinkPdf`, `CompleteRelinkPdf`, cancelamento, arquivo inválido (`CA-06`), conflito de URI (`CA-07`) e remoção explícita de indisponível (`RF-08`)

### Implementation for User Story 3

- [ ] T019 [US3] Implementar `DocumentAvailabilityChecker.kt` e `DocumentRelinkPolicy.kt` em `app/src/main/java/com/madruga665/pdfreader/core/documents/`
- [ ] T020 [US3] Atualizar `HomeIntent.kt`, `HomeViewState.kt`, `HomeSideEffect.kt` e `HomeViewModel.kt` com revalidação de disponibilidade, revinculação e tratamento de conflitos
- [ ] T021 [US3] Atualizar `RecentDocumentCard.kt` (badge `"Arquivo indisponível"` + botão `"Restaurar PDF"` + botão remover), `ErrorDialog.kt` (botão `"Restaurar PDF"` em `FileNotFound` cadastrado), `HomeScreen.kt` e adicionar `RelinkConflictDialog` (seguindo tema Neo-Brutalism Claro/Escuro)
- [ ] T022 [US3] Conectar `ActivityResultContracts.OpenDocument` para revinculação e revalidação automática de disponibilidade em `ON_RESUME` na `MainActivity.kt`

**Checkpoint**: PDFs movidos, renomeados ou apagados não geram crash e podem ser restaurados ou removidos com fluidez.

---

## Phase 6: User Story 4 — Backup e Restauração Direta do Banco SQLite `.db` (Priority: P1)

**Goal**: Substituir exportação/importação JSON por backup e restauração direta do arquivo SQLite `.db` com validação estrita, prévia, modos `Mesclar` e `Substituir`, e textos atualizados sem menção a JSON (`RF-09`..`RF-11`, `RF-13`, `RN-06`, `RN-07`, `CA-08`..`CA-11`, `CA-13`).

**Independent Test**: `SqliteBackupValidatorTest`, `SqliteBackupRepositoryTest`, `BackupMergerTest` e `AndroidSafBackupFileGatewayTest`.

### Tests for User Story 4 (TDD First)

- [ ] T023 [P] [US4] Escrever `SqliteBackupValidatorTest.kt` em `app/src/test/java/com/madruga665/pdfreader/core/backup/` testando arquivo vazio, `.json`, texto comum, bytes corrompidos, schema sem tabelas obrigatórias, `PRAGMA user_version` não suportado e arquivo `.db` válido com contagens exatas (`CA-09`)
- [ ] T024 [P] [US4] Escrever `SqliteBackupRepositoryTest.kt` e atualizar `BackupMergerTest.kt` / `AndroidSafBackupFileGatewayTest.kt` cobrindo exportação `.db` (`CA-08`), preview, modo `Substituir` (`CA-10`), modo `Mesclar` (`CA-11`), preservação de `datastore_migration_completed = "1"` e rollback em falha de transação (`RNF-02`)

### Implementation for User Story 4

- [ ] T025 [US4] Atualizar `BackupModels.kt`, `BackupFileGateway.kt` e `AndroidSafBackupFileGateway.kt` para streaming binário via arquivos temporários até `20 MB`
- [ ] T026 [US4] Implementar `SqliteBackupValidator.kt` (validação magic header `"SQLite format 3\u0000"`, `OPEN_READONLY`, `PRAGMA integrity_check`, `PRAGMA user_version`, tabelas/colunas e extração sanitizada)
- [ ] T027 [US4] Atualizar `BackupMerger.kt` (mesclagem por `id` e `document_uri` sem duplicação) e implementar `SqliteBackupRepository.kt` (exportação segura API 26+ + restauração transacional atômica)
- [ ] T028 [US4] Atualizar `BackupDialogs.kt`, `HomeViewModel.kt` e `MainActivity.kt` (`CreateDocument("application/octet-stream")`, nome `.db`, `OpenDocument` `.db`, textos 100% SQLite sem nenhuma menção a JSON — `CA-13`)

**Checkpoint**: Backup `.db` exporta, valida, visualiza prévia e restaura em ambos os modos sem qualquer referência a JSON.

---

## Phase 7: User Story 5 — Recuperação Pós-Backup em Outro Caminho/Dispositivo (Priority: P2)

**Goal**: Garantir que documentos restaurados de um backup `.db` com URIs inacessíveis no dispositivo atual apareçam como indisponíveis preservando velocidades/progresso e permitam revinculação imediata via **"Restaurar PDF"** (`RF-12`, `CA-12`).

- [ ] T029 [US5] Adicionar teste em `HomeViewModelTest.kt` e `SqliteBackupRepositoryTest.kt` simulando restauração `.db` com URI inacessível no dispositivo atual -> marcação `isAvailable = false` -> revinculação via "Restaurar PDF" preservando `customDefaultSpeedPxPerSec` e progresso (`CA-12`)
- [ ] T030 [US5] Validar integração no `HomeViewModel.confirmRestore()` disparando revalidação de disponibilidade imediatamente após restaurar o backup SQLite e atualizando `HomeViewState`

**Checkpoint**: Fluxo ponta a ponta de backup `.db` + revinculação validado.

---

## Phase 8: Polish, Limpeza de Código Legado e Verificação Final

- [ ] T031 [P] Remover arquivos e testes legados substituídos (`BackupJsonCodec.kt`, `BackupJsonCodecTest.kt`, `FilenameSpeedReconciler.kt`, `FilenameSpeedReconcilerTest.kt`, `DataStoreReadingSessionStore*`, `DataStoreRecentsRepository*`, `AndroidFavoriteFoldersRepository*`) e remover dependência `libs.json` de `app/build.gradle.kts` / `gradle/libs.versions.toml` se não houver mais referências
- [ ] T032 [P] Verificar ausência total da palavra `JSON` em textos voltados ao usuário (`BackupDialogs.kt`, `HomeViewModel.kt`, `SqliteBackupValidator.kt`, `HomeScreen.kt`) e revisar acessibilidade/contraste WCAG em Neo-Brutalism Claro/Escuro (`RNF-06`)
- [ ] T033 Executar `rg -i "json" app/src/main` e conferir zero menções na UI/backup
- [ ] T034 Executar `./gradlew testDebugUnitTest` e `./gradlew assembleDebug` com JDK 21 e confirmar 100% de aprovação (`CA-13`, `RNF-05`)
