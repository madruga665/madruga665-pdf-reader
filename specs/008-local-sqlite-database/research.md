# Technical Research: Banco de Dados Local SQLite (`008-local-sqlite-database`)

**Feature**: `008-local-sqlite-database` | **Date**: 2026-11-26

---

## R1. Motor de Persistência SQLite: SDK Nativo (`SQLiteOpenHelper`) vs Room + KSP

- **Decisão**: Utilizar `android.database.sqlite.SQLiteOpenHelper` e `SQLiteDatabase` nativos do Android SDK atrás de uma interface fina `SqliteDriver`, complementada nos testes JVM por `org.xerial:sqlite-jdbc:3.47.1.0` (`testImplementation` apenas).
- **Racional**:
  1. O projeto usa **Kotlin `2.4.0`** (`gradle/libs.versions.toml`) sem plugin `ksp` ou `kapt`. Adicionar KSP + Room gera alto risco de quebra de compatibilidade binária com o compilador Kotlin 2.4.0 e alonga o tempo de build.
  2. Room cria `room_master_table` com hash de identidade gerado em compile-time; se qualquer migração ou validação de backup `.db` precisar inspecionar tabelas puras de negócio, tabelas internas de framework acoplam o formato exportado ao compilador Room.
  3. O SQLite nativo possui apenas 3 tabelas (`pdf_documents`, `favorite_folders`, `app_settings`), nenhuma biblioteca nova de runtime (`implementation`), controle total sobre `PRAGMA integrity_check`, `PRAGMA user_version`, `journal_mode`, `CHECK` constraints e transações manuais `BEGIN IMMEDIATE`/`ROLLBACK`.
  4. Com `JdbcSqliteDriver` na suíte `src/test/`, os testes executam o **SQL real** (DDL, foreign/check/unique constraints, rollback e arquivos `.db` binários) diretamente na JVM via `./gradlew testDebugUnitTest` (`RNF-05`).
- **Alternativas consideradas**:
  - *Room + KSP*: descartada pelo acoplamento ao Kotlin 2.4.0 e geração de metadados proprietários no `.db`.
  - *SQLDelight*: exigiria plugin Gradle adicional e runtime driver extra sem necessidade para 3 tabelas simples.

---

## R2. Exportação Consistente de `.db` Compatível com `minSdk = 26`

- **Decisão**: Configurar `SQLiteOpenHelper` com `setWriteAheadLoggingEnabled(false)` (modo `DELETE`, arquivo único sem `-wal`/`-shm` pendentes), serializar acessos via `Mutex` em `Dispatchers.IO`, gravar metadados de exportação em transação, executar `PRAGMA wal_checkpoint(TRUNCATE)` como guarda defensiva, copiar o arquivo físico `pdf_reader.db` para staging temporário em `cacheDir`, validar `PRAGMA integrity_check == "ok"` no staging e só então transmitir os bytes ao destino SAF (`OutputStream`).
- **Racional**:
  - `VACUUM INTO` só foi introduzido no SQLite `3.27.0` (Android 11 / API 30). Como o app declara `minSdk = 26` (Android 8.0, SQLite `3.19.4`), usar `VACUUM INTO` causaria crash em aparelhos Android 8–10.
  - Em `journal_mode = DELETE`, após o commit da última transação e fora de escrita ativa (garantido pelo `Mutex`), todo o estado consistente reside em um único arquivo `.db`.
  - Validar `PRAGMA integrity_check` na cópia de staging garante que o arquivo enviado ao SAF é íntegro antes de retornar sucesso (`RNF-02`, `CA-08`).
- **Alternativa considerada**:
  - *`VACUUM INTO`*: descartada por incompatibilidade com `minSdk = 26`.

---

## R3. Validação e Restauração Segura de Backup `.db` (Staging Read-Only + Transação vs Troca Direta de Arquivo)

- **Decisão**: Ao selecionar um backup no SAF (`OpenDocument`), copiar o stream para arquivo temporário em `cacheDir` (teto `MAX_BACKUP_BYTES = 20 MB`), rejeitar imediatamente se vazio ou se os primeiros 16 bytes diferirem de `"SQLite format 3\u0000"`, abrir em `OPEN_READONLY`, verificar `PRAGMA integrity_check == "ok"`, `PRAGMA user_version in 1..1` e presença das tabelas/colunas obrigatórias, ler e sanitizar os dados para `BackupPayload`, e limpar o staging em `finally`. Na confirmação (`MERGE` ou `REPLACE`), aplicar o payload resultante sobre o banco ativo em uma **única transação SQLite (`BEGIN IMMEDIATE ... COMMIT`)** com rollback automático em qualquer exceção.
- **Racional**:
  - Abrir arquivos arbitrários diretamente com `SQLiteOpenHelper` pode fazer o Android tentar criar ou atualizar arquivos inválidos. Validar o cabeçalho mágico de 16 bytes antes de passar ao motor SQLite descarta `.json`, `.txt` e arquivos truncados imediatamente (`CA-09`).
  - Aplicar as alterações via transação SQLite dentro da conexão já gerenciada evita `SQLiteDatabaseLockedException` e race conditions de troca de arquivo com `SQLiteOpenHelper` aberto, garantindo atomicidade real em `MERGE` e `REPLACE` (`RN-07`, `RNF-02`).
  - Preservar sempre `app_settings['datastore_migration_completed'] = "1"` após `REPLACE` evita reimportação acidental de resíduos do DataStore legado.
- **Alternativa considerada**:
  - *Fechar `SQLiteOpenHelper`, sobrescrever `pdf_reader.db` no filesystem e reabrir*: descartada para `MERGE` (não funciona sem leitura + escrita) e arriscada para `REPLACE` caso falhe no meio ou haja leitores/coroutines concorrentes.

---

## R4. Modelo Unificado em `pdf_documents` e Identidade Imutável (`RN-01`)

- **Decisão**: Unificar documento cadastrado, sessão de leitura, velocidades e participação na lista de recentes na tabela única `pdf_documents` indexada por `id TEXT PRIMARY KEY` (UUID v4) com `document_uri TEXT NOT NULL UNIQUE`.
- **Racional**:
  - No modelo legado, sessão (`DataStoreReadingSessionStore`) e recente (`DataStoreRecentsRepository`) ficavam em DataStores diferentes e frequentemente dessincronizavam (ex.: `fileName` vazio na sessão, `custom_default_speed` perdido ao mudar URI).
  - Uma tabela única garante integridade referencial natural: quando um documento é revinculado via "Restaurar PDF", atualizar `document_uri`, `file_name` e `page_count` na mesma linha de `id` preserva instantaneamente sessão, velocidade preferida, velocidade padrão do PDF, zoom e histórico recente sem junções nem risco de órfãos.
  - O campo `is_in_recents` permite remover um documento disponível da faixa "Jump Back In" sem destruir necessariamente o histórico se desejado, enquanto remover um documento indisponível (`is_available = 0`) apaga a linha definitivamente (`RF-08`).
- **Alternativa considerada**:
  - *Tabelas separadas `documents` e `reading_sessions`*: descartada por adicionar complexidade de join/cascade desnecessária para relação estritamente 1:1.

---

## R5. Detecção de Indisponibilidade SAF sem Impactar 60fps (`RF-05`, `RNF-01`, `RNF-04`)

- **Decisão**: Implementar `DocumentAvailabilityChecker` com `isReadable(uri)` em `Dispatchers.IO` usando `contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false`, tratando `SecurityException`, `FileNotFoundException` e demais `Exception` sem propagar falhas. Executar a checagem apenas na inicialização da Home, em `HomeIntent.Refresh`, no `ON_RESUME` da `MainActivity` (quando o usuário volta ao app após mover/renomear/apagar arquivos) e como atualização reativa se `ReaderViewModel.openDocument` falhar com `FileNotFoundException` ou `SecurityException`.
- **Racional**:
  - Não consulta `ContentResolver` dentro de composables nem a cada frame, mantendo `HomeScreen` e `ReaderScreen` livres de I/O na thread de UI.
  - Cobre todos os cenários do `CA-04`: arquivo renomeado, movido para outra pasta, apagado ou com permissão SAF revogada.

---

## R6. Resolução Explícita de Conflito de URI na Revinculação (`RN-05`, `CA-07`)

- **Decisão**: Quando o usuário escolher no seletor de **"Restaurar PDF"** uma URI que já pertence a outro registro (`existing.id != target.id`), impedir gravação direta (reforçado também por `UNIQUE(document_uri)` no SQLite) e exibir `RelinkConflictDialog` com três ações claras:
  1. **Cancelar** (mantém ambos os registros intocados).
  2. **Consolidar neste registro** (atualiza o registro alvo `target.id` para a nova URI, preservando seu `id` e configurações de rolagem com ajuste seguro de página, e remove o registro conflitante `existing.id` dentro da mesma transação).
  3. **Abrir registro existente** (mantém `target` indisponível intocado e abre `existing` no leitor).
- **Racional**: Atende literalmente `RN-05` ("impedir duplicação silenciosa e informar o usuário ou consolidar de forma explícita sem perda inesperada") com previsibilidade total.

---

## R7. Remoção da Reconciliação Implícita por Nome de Arquivo (`FilenameSpeedReconciler`)

- **Decisão**: Remover a busca automática de `custom_default_speed` por `fileName` ao abrir um PDF inédito, mantendo o match exato por `document_uri` (ou revinculação explícita por `id` via "Restaurar PDF").
- **Racional**: Dois arquivos diferentes com o mesmo nome (`relatorio.pdf` em pastas distintas) não devem compartilhar identidade silenciosamente; pelo PRD (`RN-01`, `RF-06`, seção 7 Fora de Escopo), documentos movidos/renomeados são vinculados explicitamente pelo usuário via "Restaurar PDF".

---

## R8. Correção do Bug Pré-Existente de Restauração de Página e Zoom no `PdfDocumentViewport`

- **Decisão**: Incluir no escopo desta entrega a atualização de `PdfDocumentViewport.kt` e `AdaptiveReaderScaffold.kt` para inicializar `LazyListState` na página restaurada (`initialFirstVisibleItemIndex = viewState.currentPageIndex`) e `zoomScale` no valor de `viewState.zoomScale`, emitindo `onPageChanged` apenas após inicialização/alteração real do usuário ou rolagem e propagando alterações de pinch-zoom via `ReaderIntent.SetZoomScale`.
- **Racional**: Inspeção direta de `PdfDocumentViewport.kt` mostrou que hoje `rememberLazyListState()` começa sempre em `0` e o `LaunchedEffect(firstVisibleItemIndex)` grava `0` imediatamente sobre o valor carregado do armazenamento, o que faria `CA-02` e `CA-05` falharem no aplicativo real mesmo com SQLite perfeito.
