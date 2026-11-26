# Feature Specification: Banco de Dados Local SQLite, Revinculação de PDF e Backup `.db`

**Feature Branch**: `008-local-sqlite-database`  
**Created**: 2026-11-26  
**Status**: Draft  
**Harness**: `agy`  
**Porte**: `completa`  
**Input**: PRD "Banco de Dados Local (madruga665-pdf-reader)"

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastro Único e Persistência Contínua de Leitura em SQLite (Priority: P1)

Como leitor, quero que todo PDF aberto pelo seletor do sistema ou por uma pasta favorita seja cadastrado em um banco de dados local SQLite com um identificador único estável (`id`), caminho/URI, nome do arquivo, total de páginas e configurações de leitura (velocidade preferida, velocidade padrão salva do PDF, página atual, progresso normalizado, zoom e timestamp), além da velocidade padrão global, recentes e pastas favoritas unificados no mesmo banco, para que minhas preferências e posição de leitura sejam restauradas exatamente ao reabrir o aplicativo.

**Why this priority**: Substituir o DataStore Preferences como fonte de verdade pelo banco SQLite e corrigir a restauração fiel de página/zoom/velocidade é o núcleo estrutural da entrega (`RF-01`, `RF-02`, `RF-03`, `RN-01`, `RN-02`).

**Independent Test**: Abrir um PDF inédito na página 1, navegar até a página 5, ajustar zoom e velocidade, salvar velocidade padrão do documento, fechar e reabrir o mesmo PDF; verificar no banco SQLite a existência de um único registro com UUID estável e restauração exata de página, progresso, zoom e velocidade.

**Acceptance Scenarios**:

1. **Given** nenhum registro prévio para um PDF, **When** o usuário abre o documento pelo seletor de arquivos ou por uma pasta favorita (`CA-01`), **Then** o sistema cria um registro na tabela `pdf_documents` com um `id` UUID v4 não vazio e imutável, `document_uri`, `file_name`, `page_count` e valores iniciais válidos de rolagem/leitura (`RN-02`), mantendo o mesmo `id` sem duplicação ao fechar e reabrir a mesma URI.
2. **Given** um PDF aberto no leitor, **When** o usuário altera a velocidade de rolagem, salva a velocidade padrão do documento, navega para outra página (ou aplica zoom), encerra completamente o aplicativo e reabre o mesmo PDF (`CA-02`), **Then** a velocidade salva, a página atual (incluindo posicionamento inicial do viewport sem sobrescrita para página 0), o progresso normalizado e o zoom são restaurados exatamente conforme persistidos no SQLite.
3. **Given** o usuário altera a velocidade padrão global ou adiciona/remove pastas favoritas e documentos recentes, **When** o aplicativo consulta ou reinicia os dados locais, **Then** todas as leituras e escritas ocorrem no SQLite (`RF-02`) respeitando as faixas válidas de `RN-02`.

---

### User Story 2 - Migração Automática, Única e Idempotente do DataStore Existente (Priority: P1)

Como usuário que já utiliza versões anteriores do aplicativo, quero que meus dados salvos anteriormente no DataStore Preferences (velocidade padrão global, sessões de leitura, velocidades personalizadas por PDF, documentos recentes e pastas favoritas) sejam importados automaticamente para o SQLite na primeira abertura após a atualização, sem ação manual e sem duplicação em inicializações futuras.

**Why this priority**: Sem migração automática e idempotente (`RF-04`, `RN-08`, `CA-03`), usuários existentes perderiam histórico, velocidades por PDF e pastas ao atualizar o aplicativo.

**Independent Test**: Popular os três DataStores legados (`pdf_reader_prefs`, `recents_prefs`, `favorite_folders_prefs`) com dados reais, executar a inicialização da camada de banco SQLite duas vezes seguidas e validar que todos os registros foram importados com UUIDs únicos na primeira execução e que a segunda execução não cria duplicatas nem sobrescreve alterações posteriores.

**Acceptance Scenarios**:

1. **Given** um dispositivo com dados previamente gravados no DataStore Preferences e flag de migração ausente no SQLite (`CA-03`), **When** o aplicativo atualizado inicia pela primeira vez, **Then** o migrador lê os três DataStores, consolida sessões e recentes por URI, atribui um `id` UUID único a cada PDF distinto, importa pastas favoritas e velocidade global aplicando clamping de `RN-02` e grava tudo junto com a flag `datastore_migration_completed = "1"` em uma única transação SQLite.
2. **Given** que a migração já foi concluída (`datastore_migration_completed == "1"`), **When** o aplicativo é reiniciado ou quando dados locais são alterados posteriormente, **Then** a migração não volta a executar e nenhum registro é duplicado (`RN-08`).

---

### User Story 3 - Detecção de PDF Indisponível, Restauração/Revinculação e Remoção Explícita (Priority: P1)

Como leitor cujo PDF foi renomeado, movido de pasta, apagado ou teve a permissão revogada fora do aplicativo, quero que o aplicativo sinalize o documento como indisponível sem sofrer crash e me permita acionar **"Restaurar PDF"** para apontar o novo arquivo mantendo o mesmo `id`, velocidade personalizada e configurações de rolagem (ou remover o registro explicitamente caso tenha sido apagado de vez).

**Why this priority**: Resolve a fragilidade central de depender da URI bruta (`RF-05`, `RF-06`, `RF-07`, `RF-08`, `RN-03`, `RN-04`, `RN-05`) e entrega o principal diferencial funcional da versão.

**Independent Test**: Cadastrar um PDF com velocidade personalizada `160 px/s` na página 5, simular indisponibilidade da URI, retornar à Home (verificando badge de indisponibilidade e ação "Restaurar PDF"), executar revinculação com um arquivo de 3 páginas (validando ajuste seguro para índice `2`) e testar cancelamento, arquivo inválido e conflito com outra URI já cadastrada.

**Acceptance Scenarios**:

1. **Given** um PDF salvo no banco com `custom_default_speed_px_per_sec = 160.0` e `current_page_index = 4` (página 5) cuja URI deixa de estar acessível fora do app (`CA-04`), **When** o usuário retorna à tela inicial (`ON_RESUME` / `Refresh`) ou tenta abrir o documento, **Then** o aplicativo não sofre crash (`RNF-04`), marca `is_available = false` no banco preservando o registro e suas configurações (`RN-03`), exibe indicador visual claro de documento indisponível no `RecentDocumentCard` e apresenta a ação **"Restaurar PDF"**.
2. **Given** um PDF marcado como indisponível na tela inicial ou no diálogo de erro do leitor (`CA-05`), **When** o usuário aciona **"Restaurar PDF"** e seleciona o PDF renomeado/movido no seletor do sistema, **Then** o sistema solicita permissão persistível (`takePersistableUriPermission`), valida o PDF via `PdfRenderer`, mantém o mesmo `id` único, atualiza `document_uri`, `file_name` e `page_count`, ajusta `current_page_index = min(savedPage, newPageCount - 1)` (`RN-04`), preserva `preferred_speed_px_per_sec`, `custom_default_speed_px_per_sec` e `zoom_scale`, marca `is_available = true` e abre o documento imediatamente no leitor.
3. **Given** um PDF indisponível durante o fluxo **"Restaurar PDF"** (`CA-06`), **When** o usuário cancela o seletor do sistema ou escolhe um arquivo não-PDF/corrompido/ilegível, **Then** o registro original permanece intacto em `is_available = false` com todas as configurações preservadas e o usuário recebe mensagem clara caso tenha escolhido um arquivo inválido.
4. **Given** um PDF indisponível `A` e outro PDF disponível `B` já cadastrado no banco (`CA-07`), **When** o usuário tenta revincular `A` selecionando a mesma `document_uri` já pertencente a `B`, **Then** o sistema bloqueia a duplicação (`RN-05`) e exibe diálogo explícito de conflito permitindo: **Cancelar**, **Consolidar neste registro** (substitui a URI no registro `A` mantendo seu `id` e configurações e remove `B` em transação atômica) ou **Abrir registro existente** (`B`).
5. **Given** um PDF indisponível que foi excluído definitivamente (`RF-08`), **When** o usuário aciona a ação de remover no cartão (`X`), **Then** o registro é excluído definitivamente do banco SQLite.

---

### User Story 4 - Backup e Restauração Direta do Banco SQLite `.db` (Priority: P1)

Como usuário, quero exportar e restaurar um backup local diretamente como arquivo de banco de dados SQLite (`.db`), visualizando prévia com as contagens antes de confirmar **Mesclar** ou **Substituir**, sem menções ao formato JSON na interface.

**Why this priority**: Substitui o backup JSON paralelo por cópia consistente e portável do próprio banco SQLite com garantia atômica (`RF-09`, `RF-10`, `RF-11`, `RF-13`, `RN-06`, `RN-07`).

**Independent Test**: Popular o banco SQLite com PDFs, velocidades personalizadas, recentes e pastas; exportar arquivo `.db` via gateway; validar cabeçalho SQLite e contagens em arquivo exportado; tentar importar `.json`, texto aleatório e `.db` corrompido (rejeição sem alteração local); importar backup válido em modo `Substituir` e em modo `Mesclar`.

**Acceptance Scenarios**:

1. **Given** o banco SQLite populado (`CA-08`), **When** o usuário aciona **"Exportar backup (.db)"** no diálogo de Backup e Restauração e escolhe um destino via SAF (`CreateDocument("application/octet-stream")`), **Then** o sistema consolida os dados sob bloqueio mútuo em `Dispatchers.IO`, gera uma cópia íntegra `.db` (validada com `PRAGMA integrity_check == "ok"` antes do streaming) com nome sugerido `pdf-reader-backup-YYYYMMDD-HHmm.db` e exibe confirmação com as contagens exportadas (PDFs/sessões, velocidades personalizadas, recentes e pastas favoritas).
2. **Given** o usuário aciona **"Importar backup (.db)"** (`CA-09`), **When** seleciona um arquivo vazio, `.json`, texto comum ou `.db` corrompido/sem tabelas esperadas/versão incompatível, **Then** o validador rejeita o arquivo em área temporária de staging somente leitura antes de qualquer alteração no banco local, exibe erro claro e mantém 100% do banco atual intacto (`RN-07`).
3. **Given** um arquivo `.db` válido selecionado para importação (`CA-10`, `CA-11`), **When** a prévia exibe as contagens exatas do backup e o usuário confirma **"Substituir tudo"** (`REPLACE`) ou **"Mesclar (padrão)"** (`MERGE`), **Then** o repositório aplica a restauração em uma única transação SQLite atômica (com rollback automático em falha de E/S) mantendo `datastore_migration_completed = "1"`, evitando duplicações de `id` ou `document_uri` no modo Mesclar, e atualiza a interface imediatamente.
4. **Given** os diálogos e mensagens de backup (`CA-13`, `RF-13`), **When** o usuário abre o diálogo de Backup e Restauração, visualiza a prévia ou recebe mensagens de erro/sucesso, **Then** todos os textos referem-se ao banco de dados local SQLite (`.db`) e não há nenhuma menção ao formato JSON.

---

### User Story 5 - Recuperação de PDFs Após Restauração de Backup em Outro Caminho ou Dispositivo (Priority: P2)

Como usuário que restaurou um backup SQLite `.db` em um aparelho novo ou após mover meus arquivos PDF de pasta, quero que os registros importados com URIs inacessíveis permaneçam visíveis como indisponíveis preservando suas velocidades e progresso para que eu possa revinculá-los via **"Restaurar PDF"**.

**Why this priority**: Conecta a portabilidade do backup SQLite ao fluxo de revinculação (`RF-12`, `CA-12`), tornando a troca de dispositivo realmente útil mesmo sob restrições de permissão SAF do Android.

**Independent Test**: Importar um backup `.db` contendo um PDF com URI inexistente no dispositivo/teste e `custom_default_speed_px_per_sec = 175.0`; executar `HomeIntent.Refresh`, confirmar `is_available = false`, revincular via "Restaurar PDF" para uma nova URI legível e validar abertura com `175.0 px/s` preservado.

**Acceptance Scenarios**:

1. **Given** um backup `.db` restaurado cujas URIs não possuem permissão de leitura ou apontam para caminhos inexistentes no dispositivo atual (`CA-12`), **When** a restauração termina e a Home atualiza a disponibilidade dos documentos, **Then** os registros preservam `id`, `file_name`, `page_count`, `current_page_index`, `normalized_scroll_ratio`, `zoom_scale`, `preferred_speed_px_per_sec` e `custom_default_speed_px_per_sec`, sendo exibidos como indisponíveis (`is_available = false`) com o botão **"Restaurar PDF"**.
2. **Given** um registro importado indisponível dessa forma, **When** o usuário aciona **"Restaurar PDF"** e seleciona o arquivo PDF correspondente no dispositivo, **Then** a URI e o nome são atualizados mantendo o `id` e todas as configurações trazidas pelo backup, e o leitor abre imediatamente com essas configurações.

---

### Edge Cases

- **Revinculação para PDF menor**: Se `savedPageIndex = 8` (página 9) e o arquivo selecionado na revinculação tiver `3` páginas (`pageCount = 3`), `current_page_index` deve ser ajustado para `2` (`pageCount - 1`) e `normalized_scroll_ratio` recalculado de forma coerente dentro de `[0.0, 1.0]`, preservando `preferred_speed_px_per_sec` e `custom_default_speed_px_per_sec` (`RN-04`).
- **Cancelamento do seletor SAF na revinculação**: Se o usuário fechar o seletor sem escolher arquivo, nenhum dado no banco é alterado e o diálogo de conflito/erro não dispara falsamente (`CA-06`).
- **Arquivo não-PDF ou corrompido na revinculação**: Se o URI escolhido retornar falha no `PdfRenderer` ou `pageCount < 1`, o registro original continua indisponível intacto e exibe feedback de erro claro (`CA-06`).
- **Conflito de URI na revinculação**: Se a URI escolhida já pertencer a outro registro no banco (`UNIQUE(document_uri)`), o sistema nunca falha silenciosamente nem cria duplicata; apresenta `RelinkConflictDialog` (`CA-07`, `RN-05`).
- **Reabrir um PDF que foi removido apenas da lista de recentes**: Quando um documento **disponível** é removido via botão `X` dos recentes, o registro pode manter suas configurações com `is_in_recents = 0` (ou, ao reabrir a mesma URI, reusar o mesmo `id` e velocidades se preservado); quando um documento **indisponível** é removido via `X`, o registro é removido definitivamente (`RF-08`).
- **Importação de backup corrompido, vazio, `.json` ou versão futura (`PRAGMA user_version > 1`)**: Rejeitado na fase de inspeção somente leitura em staging; arquivo temporário é apagado em `finally` e o banco local permanece intocado (`RN-07`, `CA-09`).
- **Falha de E/S ou transação durante `restoreBackup`**: Rollback automático da transação SQLite mantém o estado anterior integralmente válido (`RNF-02`).
- **Backup de origem sem `datastore_migration_completed`**: Ao restaurar via `REPLACE`, o sistema garante `datastore_migration_completed = "1"` no banco local restaurado para que a migração legada do DataStore não reimporte lixo antigo sobre o backup restaurado.
- **Compatibilidade com Android 8.0+ (`minSdk = 26`)**: Exportação NÃO utiliza `VACUUM INTO` (indisponível abaixo da API 30); usa `journal_mode = DELETE`, bloqueio por `Mutex`, checkpoint defensivo e cópia atômica para staging validada antes de gravar no destino SAF.
- **Temas Claro/Escuro e Layouts Compact/Expanded**: Todos os novos badges, botões `"Restaurar PDF"`, texto de backup `.db` e `RelinkConflictDialog` obedecem ao design system Neo-Brutalism (`NeobrutalismTheme.colors`) e não sofrem corte de sombras deslocadas em larguras compactas ou expandidas (`RNF-06`).

---

## Requirements *(mandatory)*

### Functional Requirements & PRD Traceability

- **FR-001 (`RF-01`, `RN-01`)**: Ao abrir qualquer PDF (seletor SAF, pasta favorita ou intent externa), o sistema MUST consultar `pdf_documents` por `document_uri`; se inexistente, inserir registro com `id` UUID v4 gerado uma única vez; se existente, preservar o `id` original e atualizar metadados de acesso.
- **FR-002 (`RF-02`)**: O sistema MUST armazenar no banco SQLite local (`pdf_reader.db`) todas as tabelas de domínio: `pdf_documents` (documentos, sessões de leitura, velocidades por PDF e recentes), `favorite_folders` e `app_settings` (`global_default_speed`, controle de migração e metadados), tornando o SQLite a fonte única de verdade em runtime.
- **FR-003 (`RF-03`, `RN-02`)**: Alterações em `preferred_speed_px_per_sec` (`[10.0, 500.0]`), `custom_default_speed_px_per_sec` (salvar/resetar padrão do PDF sem ser apagado por salvamentos comuns de progresso), `current_page_index` (`[0, page_count - 1]`), `normalized_scroll_ratio` (`[0.0, 1.0]`) e `zoom_scale` (`[0.5, 4.0]`) MUST ser persistidas pelo `id` estável do documento e restauradas fielmente ao reabrir o PDF (incluindo inicialização da posição de página e zoom no `PdfDocumentViewport`).
- **FR-004 (`RF-04`, `RN-08`)**: Na inicialização do app (antes da leitura de UI/repositórios), caso `app_settings["datastore_migration_completed"] != "1"`, o sistema MUST executar migração única e transacional dos DataStores legados (`pdf_reader_prefs`, `recents_prefs`, `favorite_folders_prefs`) para o SQLite atribuindo `id` UUID v4 a cada PDF distinto, e marcar `datastore_migration_completed = "1"`.
- **FR-005 (`RF-05`, `RNF-04`)**: O sistema MUST verificar a acessibilidade real dos documentos listados na inicialização, em `HomeIntent.Refresh`, no `ON_RESUME` e ao falhar a abertura no leitor (`FileNotFoundException` / `SecurityException`), marcando `is_available = false` sem encerrar o app nem apagar configurações (`RN-03`).
- **FR-006 (`RF-05`, `RF-06`)**: Documentos com `is_available = false` MUST exibir indicação visual clara de indisponibilidade e a ação **"Restaurar PDF"** no `RecentDocumentCard` e no `ErrorDialog` (quando originado de documento cadastrado).
- **FR-007 (`RF-06`, `RN-01`, `RN-04`)**: Ao selecionar um novo arquivo via **"Restaurar PDF"**, o sistema MUST solicitar permissão persistível de leitura (`takePersistableUriPermission`), inspecionar o PDF com `PdfRenderer` para validar integridade e obter `file_name` + `page_count`, atualizar `document_uri`, `file_name`, `page_count`, ajustar `current_page_index = min(savedPage, newPageCount - 1)`, manter o mesmo `id` e velocidades, marcar `is_available = true`, `is_in_recents = true` e navegar abrindo o documento imediatamente.
- **FR-008 (`RF-07`, `RN-05`)**: Em caso de cancelamento ou PDF inválido durante **"Restaurar PDF"**, o registro original MUST permanecer inalterado em `is_available = false` com mensagem de erro quando aplicável. Se a nova URI já pertencer a outro `id` em `pdf_documents`, o sistema MUST impedir duplicação silenciosa e exibir opção explícita ao usuário (cancelar, consolidar preservando o registro restaurado ou abrir o registro existente).
- **FR-009 (`RF-08`)**: Documentos indisponíveis MUST continuar oferecendo remoção explícita da lista e do banco de dados.
- **FR-10 (`RF-09`, `RN-06`, `RNF-02`)**: A exportação de backup MUST produzir exclusivamente arquivo SQLite `.db` íntegro e consistente (validado via `PRAGMA integrity_check`), gravado via SAF `CreateDocument`, retornando resumo com quantidades de PDFs/sessões, velocidades personalizadas, recentes e pastas favoritas.
- **FR-11 (`RF-10`, `RN-07`, `RNF-02`)**: A importação de backup MUST copiar o arquivo selecionado via SAF para staging temporário (com limite de tamanho), validar magic header SQLite, abrir em modo somente leitura, verificar `PRAGMA integrity_check`, `PRAGMA user_version` e schema obrigatório, e apresentar diálogo de prévia com as contagens antes de qualquer modificação; arquivos `.json`, texto, vazios ou `.db` corrompidos/incompatíveis MUST ser rejeitados sem alterar o banco local.
- **FR-12 (`RF-11`, `RN-07`)**: Na confirmação da prévia, o usuário MUST escolher entre **Substituir** (`REPLACE`) ou **Mesclar** (`MERGE` — sem duplicar `id` nem `document_uri`), executando a gravação em transação SQLite atômica com rollback em falha e atualização imediata da tela inicial.
- **FR-13 (`RF-12`)**: Após restauração de backup cujas URIs estejam inacessíveis no ambiente atual, os registros restaurados MUST permanecer disponíveis na lista com suas configurações de rolagem para revinculação via **"Restaurar PDF"**.
- **FR-14 (`RF-13`)**: Todos os rótulos, descrições, filtros/extensões sugeridas e mensagens de backup/restauração na UI e viewmodels MUST indicar banco de dados local SQLite (`.db`), removendo qualquer menção ao formato JSON.
- **FR-15 (`RN-01`, `RNF-05`)**: Reabrir um documento inédito por uma URI desconhecida NÃO deve herdar implicitamente velocidade personalizada de outro documento apenas pelo nome do arquivo (a vinculação de arquivo movido/renomeado ocorre explicitamente por "Restaurar PDF"). Todas as regras puras (clamping, migração, revinculação, conflito de URI, merge e operações SQLite) MUST ser cobertas por testes unitários na JVM.

### Key Entities

- **`PdfDocumentRecord`**: Entidade raiz persistida na tabela `pdf_documents`. Contém `id` (UUID v4 imutável), `documentUri` (único), `fileName`, `pageCount`, `currentPageIndex`, `normalizedScrollRatio`, `zoomScale`, `preferredSpeedPxPerSec`, `customDefaultSpeedPxPerSec`, `lastAccessedAt`, `isInRecents`, `isAvailable`, `createdAt`, `updatedAt`.
- **`FavoriteFolder`**: Pasta favorita persistida em `favorite_folders` (`treeUri` PK, `displayName`, `pathSummary`, `pinnedTimestamp`, `cachedFileCount`).
- **`AppSettings`**: Pares chave-valor em `app_settings` (`global_default_speed`, `datastore_migration_completed`, `datastore_migration_at`, `app_version`, `last_backup_exported_at`).
- **`BackupPayload` / `BackupSummary`**: Snapshot tipado extraído do SQLite (versão de schema `1`, timestamp de exportação, versão do app, `globalDefaultSpeedPxPerSec`, lista completa de `PdfDocumentRecord` e `FavoriteFolder`) para prévia, exportação e mesclagem determinística.
- **`RelinkConflict`**: Estado representando colisão de `document_uri` ao tentar revincular um `targetDocumentId` para uma URI já ocupada por `conflictingDocumentId`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001 (`CA-01`, `CA-02`)**: 100% dos PDFs abertos recebem `id` UUID único na tabela `pdf_documents`, sem registros duplicados por URI e com restauração fiel de página, progresso, zoom e velocidade ao reabrir após fechamento completo do app.
- **SC-002 (`CA-03`, `RN-08`)**: Migração única dos 3 DataStores legados preserva 100% dos dados válidos com 0 duplicações em múltiplas inicializações subsequentes.
- **SC-003 (`CA-04`, `CA-05`, `CA-06`, `CA-07`)**: 0 crashes diante de arquivos renomeados, movidos, apagados, sem permissão SAF ou inválidos; 100% dos casos de revinculação preservam `id` e velocidades, ajustam página quando `newPageCount < savedPage + 1` e bloqueiam duplicação em conflito de URI.
- **SC-004 (`CA-08`, `CA-09`, `CA-10`, `CA-11`, `CA-12`)**: Arquivos `.db` exportados passam em `PRAGMA integrity_check == "ok"`, arquivos não-SQLite (incluindo `.json`) ou corrompidos são rejeitados com 0 alterações locais, modos `Substituir` e `Mesclar` executam em transação atômica e documentos restaurados aceitam revinculação posterior mantendo suas configurações.
- **SC-005 (`CA-13`, `RNF-05`)**: `./gradlew testDebugUnitTest` e `./gradlew assembleDebug` passam com 0 falhas e 0 erros de compilação na JVM; zero ocorrências do termo "JSON" em textos visíveis de backup na UI.

---

## Assumptions & Scope Boundaries

### Assumptions
- O ambiente de execução local e CI utiliza JDK 21 para rodar o Gradle (`gradle/gradle-daemon-jvm.properties`) compilando para Java 17 (`app/build.gradle.kts`), com `minSdk = 26` e `targetSdk = 35`.
- Arquivos PDF locais continuam acessados exclusivamente via Android Storage Access Framework (SAF) com `OpenDocument` e `OpenDocumentTree`, sem busca automática em diretórios arbitrários do armazenamento.

### Out of Scope (v1)
- Sincronização em nuvem, contas de usuário ou backup automático agendado.
- Varredura automática do sistema de arquivos para adivinhar arquivos movidos sem seleção do usuário.
- Importação retroativa de arquivos de backup antigos `.json` (apenas dados já presentes no DataStore local são migrados automaticamente).
- Criptografia com senha do arquivo `.db` exportado.
- Armazenamento de bytes/conteúdo binário de arquivos PDF dentro do SQLite.
- Anotações, marcadores de página ou leitura remota.
