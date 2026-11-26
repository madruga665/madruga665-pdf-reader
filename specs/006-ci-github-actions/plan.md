# Implementation Plan: CI com GitHub Actions para Testes Unitários e Build Debug

**Branch**: `006-ci-github-actions` | **Date**: 2026-11-26 | **Spec**: [spec.md](file:///home/madruga665/Projetos/madruga665-pdf-reader/specs/006-ci-github-actions/spec.md)  
**Porte**: `pequena`  
**Input**: Feature specification from `/specs/006-ci-github-actions/spec.md`

---

## Summary

Implantar o pipeline de Integração Contínua (CI) nativo no GitHub Actions em `.github/workflows/ci.yml` para validar `./gradlew testDebugUnitTest --stacktrace` e `./gradlew assembleDebug --stacktrace` em Pull Requests direcionados à branch `main`, garantindo portabilidade de JDK entre o runner `ubuntu-latest` e o ambiente local do desenvolvedor pela remoção de `org.gradle.java.home` absoluto em `gradle.properties` e adoção de `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=21`).

---

## Technical Context

**Language/Version**: Kotlin `2.4.0` (`JvmTarget.JVM_17` em `app/build.gradle.kts`) + YAML (GitHub Actions)  
**Primary Dependencies**: Android Gradle Plugin `8.7.2`, Gradle Wrapper `8.11.1` (`gradle-wrapper.jar` SHA-256 oficial `76805e32c009c0cf0dd5d206bddc9fb22ea42e84db904b764f3047de095493f3`), JDK `21` para o Gradle Daemon  
**Storage**: N/A (apenas cache gerenciado do Gradle User Home via `gradle/actions/setup-gradle` e artefatos temporários de relatório de teste em falhas)  
**Testing**: Suíte unitária JVM (`./gradlew testDebugUnitTest` — 13 arquivos em `app/src/test/`) e build Debug (`./gradlew assembleDebug`)  
**Target Platform**: Runner hospedado Linux `ubuntu-latest` do GitHub Actions + ambiente local Linux do desenvolvedor (`ANDROID_HOME` local e `JAVA_HOME=/usr/lib/jvm/default` OpenJDK 27 com JDK 21 em `~/.jdks/jbrsdk_jcef-21.0.11`); aplicativo Android `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`  
**Project Type**: Aplicativo Android nativo (`:app`, repositório público `github.com/madruga665/madruga665-pdf-reader`)  
**Performance Goals**: Retorno do pipeline <= `10` minutos em PR com cache aquecido; `timeout-minutes: 20` explícito no job  
**Constraints**: Permissões mínimas (`contents: read`), zero segredos, actions fixadas por SHA completo de 40 caracteres, cache somente leitura para forks, zero menções locais absolutas versionadas e zero regressão no ambiente local  
**Scale/Scope**: 3 arquivos no repositório (`gradle.properties` atualizado, `gradle/gradle-daemon-jvm.properties` novo e `.github/workflows/ci.yml` novo)

---

## Constitution Check

*Referência: `.specify/memory/constitution.md` (v1.0.0) e `AGENTS.md`.*

- **I. Smooth & Predictable Rendering (Performance First)**: PASS — O pipeline de CI não altera código de produção, renderização PDF nem UI Compose.
- **II. Modular & Engine-Agnostic Core (Library-First)**: PASS — A separação existente entre módulos puros JVM (`core/`) e Android UI permanece intacta e tem sua testabilidade contínua garantida em cada PR via `testDebugUnitTest`.
- **III. Test-First & Quality Gates (NON-NEGOTIABLE)**: PASS — Reforça diretamente a seção *Development Workflow & Quality Gates*, tornando automática a execução da suíte unitária e do build Debug antes do merge na `main` sem mascarar falhas.
- **IV. Responsive & Accessible Reader Experience**: PASS — Sem alterações em telas, acessibilidade ou contratos de UI.
- **V. Observability & Graceful Error Recovery**: PASS — Garante visibilidade diagnóstica no CI com `--stacktrace` e publicação condicional do relatório de testes em caso de falha (`unit-test-reports`).

---

## Project Structure

### Documentation (this feature — porte `pequena`)

```text
specs/006-ci-github-actions/
├── spec.md              # Especificação funcional e critérios mensuráveis
└── plan.md              # Este plano técnico enxuto e checklist de implementação
```

### Source Code & Repository Files (a criar/alterar na implementação)

```text
/
├── gradle.properties                         # UPDATE: remover org.gradle.java.home absoluto local
├── gradle/
│   └── gradle-daemon-jvm.properties          # NEW: toolchainVersion=21 (via ./gradlew updateDaemonJvm --jvm-version=21)
└── .github/
    └── workflows/
        └── ci.yml                            # NEW: workflow "Android CI" para PRs -> main
```

---

## Decisões Arquiteturais e de Segurança

### 1. Portabilidade de JDK (`gradle/gradle-daemon-jvm.properties` vs alternativas)

- **Fato observado no ambiente local**: `JAVA_HOME` do shell aponta para `/usr/lib/jvm/default` (`OpenJDK 27.0.1.1`), enquanto o JDK 21 está em `/home/madruga665/.jdks/jbrsdk_jcef-21.0.11`, e `~/.gradle/gradle.properties` não existe.
- **Capacidade verificada no Gradle `8.11.1`**: o `DefaultToolchainConfiguration` habilita `autoDetectEnabled = true` com `IntellijInstallationSupplier` varrendo `~/.jdks`, e o launcher já implementa `DaemonJvmCriteria` / `DaemonJavaToolchainQueryService`.
- **Decisão**: remover a linha `org.gradle.java.home=/home/madruga665/.jdks/jbrsdk_jcef-21.0.11` (e seu comentário específico) de `gradle.properties`, preservando todas as demais propriedades (`org.gradle.jvmargs`, `org.gradle.parallel`, `org.gradle.caching`, `android.useAndroidX`, `kotlin.suppressGradlePluginWarnings`, `org.gradle.tooling.parallel`), e versionar `gradle/gradle-daemon-jvm.properties` contendo `toolchainVersion=21` (gerado com `./gradlew updateDaemonJvm --jvm-version=21`).
  - **Alternativas rejeitadas**:
    - *Mover `org.gradle.java.home` para `~/.gradle/gradle.properties`*: exigiria configuração manual fora do Git em cada máquina.
    - *Passar `-Dorg.gradle.java.home` apenas no CI e manter caminho local em `gradle.properties`*: manteria um caminho pessoal acoplado no repositório e falharia com `org.gradle.java.home` inválido para qualquer outro colaborador ou script sem override.
- **Contingência documentada**: se, durante a implementação local, o launcher sob Java 27 falhar ao consultar toolchains para o Daemon antes de iniciar, o implementador deve validar e documentar a alternativa mínima em `gradle.properties` (por exemplo, confirmar `org.gradle.java.installations.auto-detect=true` ou checar execução de `updateDaemonJvm`), **sem nunca reintroduzir caminho absoluto local no repositório**.

### 2. Estrutura de Job Único vs Jobs Paralelos

- **Decisão**: usar um único job (`verify`, `name: Unit Tests & Debug Build`) em `ubuntu-latest` executando `./gradlew testDebugUnitTest --stacktrace` seguido de `./gradlew assembleDebug --stacktrace`.
- **Justificativa**: reaproveita o mesmo checkout, o Gradle Daemon já aquecido e as saídas compiladas/transformações da primeira invocação na segunda; evita duplicar custo de setup/cache e entrega um único check objetivo para Branch Protection futura (`RNF-02`, `RNF-05`).

### 3. Política de Cache no `gradle/actions/setup-gradle`

- **Fato observado no `action.yml` do `setup-gradle@v6.4.1`**: o padrão de `cache-read-only` é `${{ github.event.repository != null && github.ref_name != github.event.repository.default_branch }}`. Como este workflow roda apenas em `pull_request` (e não em `push` na `main`), deixar o padrão impediria que PRs internos gravassem o cache do Gradle na primeira execução (`RNF-02`).
- **Decisão**: configurar explicitamente `cache-read-only: ${{ github.event.pull_request.head.repo.fork == true }}`.
- **Justificativa**: PRs abertos a partir de branches do próprio repositório leem e gravam cache (garantindo retorno <= 10 min após aquecimento), enquanto PRs originados de forks externos mantêm cache estritamente somente leitura para proteger o repositório público contra poluição de cache (`RNF-03`).

### 4. Segurança e Fixação de Versões (Supply Chain)

Todas as actions oficiais serão fixadas por commit SHA completo de 40 caracteres verificado na API do GitHub:

- `actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1` — com `persist-credentials: false`.
- `actions/setup-java@d1374f8ac74474200d16ef8ecee22c7ae7a51880 # v6.3.0` — com `distribution: temurin` e `java-version: '21'`.
- `gradle/actions/setup-gradle@8da46b539cd8fe6ab27ca75128919a18d757604c # v6.4.1` — com `validate-wrappers: true` (SHA-256 do `gradle-wrapper.jar` já confirmado como oficial) e `cache-read-only: ${{ github.event.pull_request.head.repo.fork == true }}`.
- `actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9 # v7.0.2` — com `retention-days: 7` e `if-no-files-found: warn`.
- Permissões no topo do workflow: `permissions: { contents: read }`. Nenhum segredo utilizado.

### 5. Especificação Exata do Workflow (`.github/workflows/ci.yml`)

```yaml
name: Android CI

on:
  pull_request:
    branches:
      - main
    types:
      - opened
      - synchronize
      - reopened

permissions:
  contents: read

concurrency:
  group: ${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}
  cancel-in-progress: true

jobs:
  verify:
    name: Unit Tests & Debug Build
    runs-on: ubuntu-latest
    timeout-minutes: 20

    steps:
      - name: Checkout repository
        uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
        with:
          persist-credentials: false

      - name: Set up JDK 21
        uses: actions/setup-java@d1374f8ac74474200d16ef8ecee22c7ae7a51880 # v6.3.0
        with:
          distribution: temurin
          java-version: '21'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@8da46b539cd8fe6ab27ca75128919a18d757604c # v6.4.1
        with:
          validate-wrappers: true
          cache-read-only: ${{ github.event.pull_request.head.repo.fork == true }}

      - name: Run unit tests
        id: unit-tests
        run: ./gradlew testDebugUnitTest --stacktrace

      - name: Upload unit test reports on failure
        if: ${{ failure() && steps.unit-tests.outcome == 'failure' }}
        uses: actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9 # v7.0.2
        with:
          name: unit-test-reports
          path: |
            app/build/reports/tests/testDebugUnitTest/
            app/build/test-results/testDebugUnitTest/
          if-no-files-found: warn
          retention-days: 7

      - name: Build Debug APK
        run: ./gradlew assembleDebug --stacktrace
```

### 6. Android SDK em `ubuntu-latest` e Contingência de Licenças

- Os runners `ubuntu-latest` disponibilizam `ANDROID_HOME` pré-instalado; na v1 não se inclui action de terceiros para gerenciar SDK (`NFR-005`).
- **Contingência**: caso a primeira execução no PR para `main` falhe por licença não aceita do Android SDK 35, adicionar antes da etapa `Run unit tests` apenas o comando mínimo nativo:
  `yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null || true`

---

## Checklist de Implementação Ordenada (Porte: `pequena`)

- **T001** — Criar/alternar para a branch `006-ci-github-actions` a partir da `main` limpa e garantir `.specify/feature.json` sincronizado.
- **T002** — Atualizar `gradle.properties` removendo `org.gradle.java.home=/home/madruga665/.jdks/jbrsdk_jcef-21.0.11` e executar `./gradlew updateDaemonJvm --jvm-version=21` para gerar `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=21`).
- **T003** — Validar portabilidade local sob o `JAVA_HOME` padrão do sistema (`/usr/lib/jvm/default`): confirmar que `./gradlew --version --no-daemon` escolhe o Daemon JVM 21 e executar `./gradlew testDebugUnitTest` + `./gradlew assembleDebug` localmente até passarem com código `0`.
- **T004** — Criar `.github/workflows/ci.yml` exatamente conforme o contrato especificado neste plano.
- **T005** — Auditar os arquivos alterados/criados confirmando: YAML válido, SHAs completos de 40 caracteres, `permissions: contents: read`, `persist-credentials: false`, zero segredos, zero referências a caminhos absolutos locais e zero menções indevidas em código/commits.
- **T006** — Preparar evidências para Code Review e validação em PR real direcionado à `main` no GitHub Actions (observando autorização explícita do usuário antes de `git push` / abertura de PR remoto, conforme regra do repositório).

---

## Plano de Verificação e Evidências (`CA-01`..`CA-09`)

### Comandos de Validação Local (após T002–T004)

```bash
# 1. Confirmar remoção de caminho absoluto e geração do critério do Daemon JVM
git diff gradle.properties
cat gradle/gradle-daemon-jvm.properties

# 2. Confirmar seleção do Daemon JDK 21 usando JAVA_HOME padrão local (OpenJDK 27)
JAVA_HOME=/usr/lib/jvm/default ./gradlew --version --no-daemon

# 3. Validar suíte unitária JVM e build Debug localmente (CA-09)
./gradlew testDebugUnitTest --stacktrace
./gradlew assembleDebug --stacktrace

# 4. Validar sintaxe YAML do workflow
python3 -c 'import yaml; yaml.safe_load(open(".github/workflows/ci.yml"))'
```

### Checklist de Evidências para Code Review e QA

| Critério | Evidência Esperada |
|----------|--------------------|
| `CA-01` / `CA-02` | `on.pull_request.branches: [main]` com `types: [opened, synchronize, reopened]` no YAML + execução disparada no PR para `main` |
| `CA-03` / `CA-05` | Log verde no GitHub Actions comprovando `testDebugUnitTest` e `assembleDebug` com `BUILD SUCCESSFUL` no PR de validação |
| `CA-04` / `CA-06` | Comandos `./gradlew` sem `|| true` nem `continue-on-error: true` (revisão + evidência de propagação de falha se testada) |
| `CA-07` | Step `Upload unit test reports on failure` com `if: ${{ failure() && steps.unit-tests.outcome == 'failure' }}` e caminhos `testDebugUnitTest` |
| `CA-08` | Bloco `concurrency` com grupo `${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}` e `cancel-in-progress: true` |
| `CA-09` | Passagem verde local sob `JAVA_HOME=/usr/lib/jvm/default` + passagem verde em runner limpo `ubuntu-latest` sem `local.properties` |

---

## Complexity Tracking

Nenhuma violação à Constituição identificada — solução mínima em 3 arquivos usando exclusivamente recursos nativos do Gradle `8.11.1` e GitHub Actions.
