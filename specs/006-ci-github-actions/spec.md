# Feature Specification: CI com GitHub Actions para Testes Unitários e Build Debug

**Feature Branch**: `006-ci-github-actions`  
**Created**: 2026-11-26  
**Status**: Draft  
**Porte**: `pequena`  
**Input**: PRD aprovado em `/home/madruga665/OpenBot/Shared/prds-pessoal/madruga665-pdf-reader/ci-github-actions.md` — Implantar pipeline de Integração Contínua (CI) no GitHub Actions para o repositório `madruga665-pdf-reader` executando automaticamente os testes unitários JVM (`testDebugUnitTest`) e o build Android Debug (`assembleDebug`) em todos os Pull Requests direcionados à branch `main`, resolvendo a dependência de caminho absoluto local de JDK sem quebrar o ambiente local.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Validação Automática de Testes Unitários e Build Debug em PRs para `main` (Priority: P1) 🎯 MVP

Como mantenedor do `madruga665-pdf-reader`, eu quero que todo Pull Request apontando para a branch `main` dispare automaticamente a execução completa dos testes unitários JVM (`testDebugUnitTest`) e a compilação do APK Debug (`assembleDebug`) em um runner limpo do GitHub Actions, exibindo status verde ou vermelho nos checks do PR, para impedir merges com erros de compilação ou regressões funcionais.

**Why this priority**: É o núcleo de valor da demanda (`RF-01`, `RF-02`, `RF-03`, `RN-01`, `RN-02`). Sem gatilho focado na `main` e propagação fiel de falhas de testes e build, a `main` permanece exposta a regressões silenciosas.

**Independent Test**: Abrir um Pull Request de uma branch de feature para `main` e observar a execução automática do workflow `Android CI` na aba Checks do PR concluindo `./gradlew testDebugUnitTest --stacktrace` e `./gradlew assembleDebug --stacktrace` com status verde; adicionalmente, inspecionar a configuração para confirmar restrição de gatilho à branch `main` e ausência de mascaramento de exit code.

**Acceptance Scenarios**:

1. **Given** um Pull Request cuja branch base de destino é `main` (`CA-01`, `RF-01`, `RN-01`),  
   **When** o PR é aberto (`opened`), recebe novos commits (`synchronize`) ou é reaberto (`reopened`),  
   **Then** o workflow de CI inicia automaticamente sobre o commit mais recente do PR e publica o check correspondente na página do Pull Request.

2. **Given** um Pull Request cuja branch de destino é diferente de `main` (`CA-02`, `RF-01`, `RN-01`),  
   **When** o PR é aberto ou atualizado,  
   **Then** o gatilho deste workflow não é acionado para essa branch alvo.

3. **Given** um PR para `main` com todos os testes unitários JVM em `app/src/test/` íntegros e código/recursos Android válidos (`CA-03`, `CA-05`, `RF-02`, `RF-03`),  
   **When** o job de CI executa em runner Linux limpo (`ubuntu-latest`),  
   **Then** `./gradlew testDebugUnitTest --stacktrace` executa toda a suíte unitária com sucesso e `./gradlew assembleDebug --stacktrace` gera o APK Debug (`BUILD SUCCESSFUL`), finalizando o check como aprovado (verde).

4. **Given** um PR para `main` contendo ao menos um teste unitário falhando ou erro de compilação/recurso Android (`CA-04`, `CA-06`, `RN-02`),  
   **When** o workflow executa,  
   **Then** a respectiva etapa falha com exit code não-zero propagado sem mascaramento, encerrando o workflow com status de falha (vermelho) no PR e identificando a etapa e o erro nos logs.

---

### User Story 2 - Portabilidade de Ambiente entre CI e Máquina Local (Priority: P1) 🎯 MVP

Como desenvolvedor trabalhando no repositório local e revisando PRs no GitHub Actions, eu quero que a seleção da JVM do Gradle Daemon deixe de depender do caminho absoluto local `/home/madruga665/.jdks/jbrsdk_jcef-21.0.11` em `gradle.properties`, utilizando critérios portáveis versionados no projeto, para que `testDebugUnitTest` e `assembleDebug` passem tanto no runner limpo do GitHub Actions quanto localmente sem arquivos ignorados pelo Git (`local.properties`).

**Why this priority**: Bloqueador técnico direto (`RN-03`, `RNF-01`, `CA-09`): o `gradle.properties` versionado hoje aponta para um caminho inexistente nos runners do GitHub Actions, enquanto o `JAVA_HOME` padrão local (`OpenJDK 27`) não deve impedir o Gradle 8.11.1 de descobrir automaticamente o JDK 21 local em `~/.jdks`.

**Independent Test**: Remover `org.gradle.java.home` absoluto de `gradle.properties`, configurar `gradle/gradle-daemon-jvm.properties` com `toolchainVersion=21` e validar localmente em um shell limpo (com `JAVA_HOME=/usr/lib/jvm/default`) que `./gradlew --version --no-daemon` seleciona o JDK 21 local e que `./gradlew testDebugUnitTest` e `./gradlew assembleDebug` passam integralmente, repetindo a validação no runner limpo do GitHub Actions sem `local.properties`.

**Acceptance Scenarios**:

1. **Given** o repositório sem caminho absoluto local em `gradle.properties` e com `gradle/gradle-daemon-jvm.properties` versionado (`CA-09`, `RN-03`, `RNF-01`),  
   **When** um runner limpo do GitHub Actions (sem `local.properties`, com JDK 21 via `actions/setup-java` e `ANDROID_HOME` pré-instalado) executa `./gradlew testDebugUnitTest --stacktrace` e `./gradlew assembleDebug --stacktrace`,  
   **Then** ambos executam sem erros de `org.gradle.java.home` inválido ou arquivos locais ausentes.

2. **Given** o ambiente local do desenvolvedor com `JAVA_HOME=/usr/lib/jvm/default` (JDK 27) e JDK 21 instalado em `/home/madruga665/.jdks/jbrsdk_jcef-21.0.11` (`CA-09`, `RN-03`, `RNF-01`),  
   **When** o desenvolvedor executa `./gradlew testDebugUnitTest` e `./gradlew assembleDebug` localmente sem editar arquivos fora do projeto,  
   **Then** o Gradle descobre automaticamente o JDK 21 compatível e conclui testes e build sem regressão.

---

### User Story 3 - Diagnóstico em Falhas de Teste, Concorrência e Segurança do Workflow (Priority: P2)

Como revisor de PRs, eu quero que falhas em testes unitários publiquem os relatórios gerados como artefato baixável no GitHub Actions, que novos commits no mesmo PR cancelem execuções antigas em andamento, e que o workflow opere com permissões mínimas, actions fixadas por SHA, cache compatível com PRs e timeout explícito, para diagnosticar falhas rapidamente e manter o CI seguro e ágil.

**Why this priority**: Atende às garantias operacionais, de segurança e de tempo de resposta do PRD (`RF-04`, `RF-05`, `RN-04`, `RN-05`, `RNF-02`–`RNF-05`, `CA-07`, `CA-08`).

**Independent Test**: Inspecionar o manifesto `.github/workflows/ci.yml` verificando política de `concurrency`, `permissions`, `timeout-minutes`, fixação de actions por commit SHA completo de 40 caracteres, configuração de cache seguro para forks e step condicional de `upload-artifact` na falha de `testDebugUnitTest`; validar por execução real em PR com commits sucessivos ou análise de logs/artefatos.

**Acceptance Scenarios**:

1. **Given** uma execução de CI em que a etapa de testes unitários (`testDebugUnitTest`) falha (`CA-07`, `RF-04`, `RN-04`),  
   **When** o job se encerra,  
   **Then** os relatórios de teste em `app/build/reports/tests/testDebugUnitTest/` e `app/build/test-results/testDebugUnitTest/` são enviados como artefato `unit-test-reports` no GitHub Actions com retenção finita.

2. **Given** um Pull Request para `main` com execução de CI em progresso (`CA-08`, `RF-05`),  
   **When** um novo commit é enviado para a mesma branch do PR,  
   **Then** a execução anterior desse PR é cancelada automaticamente pela política de concorrência (`${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}`), mantendo execuções de outros PRs intactas.

3. **Given** a definição do workflow em ambiente público (`RN-05`, `RNF-02`, `RNF-03`, `RNF-04`, `RNF-05`),  
   **When** o arquivo `.github/workflows/ci.yml` é auditado,  
   **Then** declara `permissions: { contents: read }`, zero segredos, `persist-credentials: false`, actions fixadas por SHA completo, timeout explícito (`20` minutos), validação de wrapper (`validate-wrappers: true`) e cache de Gradle gravável em PRs internos e somente leitura para PRs vindos de forks.

---

### Edge Cases

- **Erro de compilação antes de gerar relatórios de teste**: se `testDebugUnitTest` falhar antes de criar `app/build/reports/tests/testDebugUnitTest/`, o upload condicional com `if-no-files-found: warn` não deve ocultar o erro original nem gerar falha secundária confusa.
- **PR direcionado a branch diferente de `main`**: o filtro `branches: [main]` no evento `pull_request` garante que o workflow não dispare.
- **PR originado de fork público**: o workflow continua rodando com permissões somente leitura (`contents: read`), sem segredos, sem persistência de credenciais no checkout (`persist-credentials: false`) e com `cache-read-only: true` no `setup-gradle` para impedir envenenamento de cache.
- **Múltiplos pushes rápidos no mesmo PR vs PRs simultâneos**: o grupo de concorrência inclui o identificador do PR (`github.event.pull_request.number || github.ref`), cancelando apenas runs antigas do mesmo PR sem afetar outros PRs abertos em paralelo.
- **Runner limpo sem `local.properties`**: o Gradle utiliza `ANDROID_HOME` pré-configurado no `ubuntu-latest` em vez de depender de `sdk.dir` local ignorado pelo Git.
- **Máquina local com `JAVA_HOME` em versão incompatível (JDK 27)**: `gradle/gradle-daemon-jvm.properties` orienta o Gradle Wrapper/Launcher a localizar automaticamente o JDK 21 em `~/.jdks` sem exigir alteração de `JAVA_HOME` global do usuário nem de `~/.gradle/gradle.properties`.
- **Job preso ou travado em rede/build**: `timeout-minutes: 20` impede consumo indefinido do runner.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001** *(rastreia `RF-01`, `RN-01`, `CA-01`, `CA-02`)*: O repositório DEVE definir um workflow de GitHub Actions (`.github/workflows/ci.yml`) disparado exclusivamente em `pull_request` direcionado à branch `main` para os tipos `opened`, `synchronize` e `reopened`.
- **FR-002** *(rastreia `RF-02`, `RN-02`, `CA-03`, `CA-04`)*: O workflow DEVE executar a suíte completa de testes unitários JVM do projeto via `./gradlew testDebugUnitTest --stacktrace`, propagando exit code não-zero imediatamente em caso de falha de compilação de teste ou reprovação de qualquer caso de teste.
- **FR-003** *(rastreia `RF-03`, `RN-02`, `CA-05`, `CA-06`)*: O workflow DEVE executar o build da variante Debug via `./gradlew assembleDebug --stacktrace`, falhando o check do PR diante de qualquer erro de compilação Kotlin, recursos Android ou empacotamento do APK.
- **FR-004** *(rastreia `RF-04`, `RN-04`, `CA-07`)*: O workflow DEVE expor o resultado das etapas nos checks do Pull Request e, sempre que a etapa de testes unitários falhar (`failure() && steps.unit-tests.outcome == 'failure'`), publicar os diretórios de relatório (`app/build/reports/tests/testDebugUnitTest/` e `app/build/test-results/testDebugUnitTest/`) como artefato `unit-test-reports` (com `if-no-files-found: warn` e retenção de `7` dias).
- **FR-005** *(rastreia `RF-05`, `CA-08`)*: O workflow DEVE configurar `concurrency` com grupo escopado ao workflow + PR/ref (`${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}`) e `cancel-in-progress: true`, cancelando execuções anteriores do mesmo PR sem interferir em outros PRs.
- **FR-006** *(rastreia `RN-03`, `RNF-01`, `CA-09`)*: O repositório DEVE remover o caminho absoluto local `org.gradle.java.home=/home/madruga665/.jdks/jbrsdk_jcef-21.0.11` de `gradle.properties` e declarar o critério portável de JVM do Daemon em `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=21`), garantindo execução sem erros tanto em runner limpo (com JDK 21) quanto localmente.
- **FR-007** *(rastreia `RN-03`, `RNF-01`, `RNF-04`)*: A execução no CI NÃO PODE depender de `local.properties` nem de arquivos ignorados pelo `.gitignore`, utilizando `ANDROID_HOME` do runner `ubuntu-latest` e `./gradlew` versionado.
- **FR-008** *(rastreia `RN-05`, `RNF-03`)*: O workflow DEVE restringir permissões globais a `contents: read`, usar `persist-credentials: false` no checkout e não exigir segredos ou tokens externos na v1.

### Non-Functional Requirements

- **NFR-001** *(rastreia `RNF-01`, `CA-09`)*: Portabilidade bidirecional validada — `./gradlew testDebugUnitTest` e `./gradlew assembleDebug` devem concluir com sucesso tanto no ambiente local (com `JAVA_HOME=/usr/lib/jvm/default` + JDK 21 em `~/.jdks`) quanto em runner limpo `ubuntu-latest` do GitHub Actions.
- **NFR-002** *(rastreia `RNF-02`)*: Performance e cache — usar `gradle/actions/setup-gradle` com cache habilitado para PRs do próprio repositório (`cache-read-only: ${{ github.event.pull_request.head.repo.fork == true }}`), visando tempo de execução <= `10` minutos com cache aquecido.
- **NFR-003** *(rastreia `RNF-03`)*: Segurança na cadeia de suprimentos — todas as actions oficiais (`actions/checkout`, `actions/setup-java`, `gradle/actions/setup-gradle`, `actions/upload-artifact`) DEVEM ser fixadas pelo commit SHA completo de 40 caracteres com comentário da tag de versão, e `setup-gradle` DEVE manter `validate-wrappers: true`.
- **NFR-004** *(rastreia `RNF-04`)*: Confiabilidade e previsibilidade — job configurado com `timeout-minutes: 20` explícito e execução determinística usando exclusivamente arquivos versionados.
- **NFR-005** *(rastreia `RNF-05`)*: Simplicidade e manutenibilidade — pipeline nativo em um único arquivo `.github/workflows/ci.yml` com um único job claro e enxuto, sem ações ou ferramentas de análise estática fora de escopo.

### Matriz de Rastreabilidade PRD → Spec

| Origem PRD | Requisito na Spec | Critério de Aceite / Sucesso |
|------------|-------------------|------------------------------|
| `RF-01`, `RN-01` | `FR-001` | `CA-01`, `CA-02`, `SC-001` |
| `RF-02`, `RN-02` | `FR-002` | `CA-03`, `CA-04`, `SC-002` |
| `RF-03`, `RN-02` | `FR-003` | `CA-05`, `CA-06`, `SC-002` |
| `RF-04`, `RN-04` | `FR-004` | `CA-07`, `SC-003` |
| `RF-05` | `FR-005` | `CA-08` |
| `RN-03`, `RNF-01` | `FR-006`, `FR-007`, `NFR-001` | `CA-09`, `SC-005`, `SC-006` |
| `RNF-02` | `NFR-002` | `SC-004` |
| `RN-05`, `RNF-03` | `FR-008`, `NFR-003` | Auditoria do `.github/workflows/ci.yml` |
| `RNF-04`, `RNF-05` | `NFR-004`, `NFR-005` | `SC-004`, revisão arquitetural |

### Key Entities / CI Contract

- **Workflow `Android CI` (`.github/workflows/ci.yml`)**: definição declarativa única do pipeline de PR para `main`, contendo gatilho, permissões `contents: read`, política de concorrência por PR, job único `verify` (`Unit Tests & Debug Build`) em `ubuntu-latest` com `timeout-minutes: 20`, setup de JDK 21 + Gradle, validação de testes e build Debug.
- **Critério de JVM do Daemon (`gradle/gradle-daemon-jvm.properties`)**: arquivo versionado do Gradle (`toolchainVersion=21`) que substitui o acoplamento local de `org.gradle.java.home` em `gradle.properties`.
- **Artefato `unit-test-reports`**: pacote publicado pelo `actions/upload-artifact` exclusivamente quando a etapa `testDebugUnitTest` falha, reunindo relatórios HTML e XML de `app/build/reports/tests/testDebugUnitTest/` e `app/build/test-results/testDebugUnitTest/`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos Pull Requests abertos, atualizados ou reabertos mirando `main` disparam o check `Unit Tests & Debug Build`; PRs mirando outras branches não disparam o workflow.
- **SC-002**: 0 falsos positivos — falhas introduzidas em testes unitários ou na compilação Debug encerram o workflow com status de falha no PR.
- **SC-003**: Em execuções onde `testDebugUnitTest` falha após gerar relatórios, 100% das runs publicam o artefato `unit-test-reports` acessível na interface da run do GitHub Actions.
- **SC-004**: Execuções em PR com cache aquecido concluem em <= `10` minutos no runner `ubuntu-latest`, com teto máximo de segurança (`timeout-minutes`) de `20` minutos.
- **SC-005**: `0` caminhos absolutos específicos de máquina (`/home/...`, `/Users/...`, `C:\...`) versionados em `gradle.properties` ou em `.github/workflows/ci.yml`.
- **SC-006**: `./gradlew testDebugUnitTest` e `./gradlew assembleDebug` finalizam com código de saída `0` tanto no ambiente local do desenvolvedor quanto no PR de validação executado no GitHub Actions.

---

## Assumptions & Out of Scope (v1)

### Assumptions

- Na v1, "testes" refere-se exclusivamente à suíte completa de **testes unitários JVM** (`./gradlew testDebugUnitTest`, atualmente composta por 13 arquivos em `app/src/test/`), executável sem emulador Android.
- Na v1, "build" refere-se exclusivamente à compilação do **APK Debug** (`./gradlew assembleDebug`), que valida Kotlin/Compose, recursos Android e empacotamento sem exigir keystore de assinatura de release.
- O runner padrão `ubuntu-latest` do GitHub Actions disponibiliza `ANDROID_HOME` com Android SDK compatível; a etapa de validação em PR real confirmará se licenças do Android SDK 35 já estão aceitas ou se demandam aceitação explícita em script enxuto.
- Localmente, o desenvolvedor possui JDK 21 instalado em `/home/madruga665/.jdks/jbrsdk_jcef-21.0.11`, detectável pelo `IntellijInstallationSupplier` do Gradle 8.11.1.

### Out of Scope (v1)

- Execução de testes instrumentados (`app/src/androidTest/`) em emulador Android ou dispositivos em nuvem.
- Build `Release`, assinatura criptográfica de APK/AAB, criação de GitHub Releases ou entrega contínua (CD) para lojas/distribuição.
- Introdução de novas ferramentas de lint/análise estática não presentes hoje no projeto (`ktlint`, `detekt`, `SonarQube`) ou exigência de percentual mínimo de cobertura.
- Configuração automática de regras de proteção de branch (*Branch Protection Rules* / *Rulesets*) no GitHub — será ativada manualmente pelo mantenedor após o primeiro check ficar visível.
- Gatilhos em `push` direto na `main` ou execuções agendadas (`schedule`).
