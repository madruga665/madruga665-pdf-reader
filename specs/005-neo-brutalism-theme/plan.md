# Implementation Plan: Aplicar Estilo Neo-Brutalism (Tema Claro e Escuro Catppuccin Mocha)

**Branch**: `005-neo-brutalism-theme` | **Date**: 2026-11-25 | **Spec**: [spec.md](file:///home/madruga665/Projetos/madruga665-pdf-reader/specs/005-neo-brutalism-theme/spec.md)

**Input**: Feature specification from `/specs/005-neo-brutalism-theme/spec.md`

## Summary

Aplicar integralmente o sistema visual **Neo-Brutalism** do ecossistema `madruga665` em toda a interface Jetpack Compose do **PDF Reader**, sincronizado automaticamente com o tema do sistema operacional (**Tema Claro** e **Tema Escuro Catppuccin Mocha**) e desabilitando `dynamicColor`. A solução introduz tokens tipados (`NeobrutalismColors`), tipografia de alto contraste (`NeobrutalismTypography`), modificadores dedicados de desenho em GPU/Canvas sem blur (`neobrutalistShadow` e `neobrutalistGridBackground`) e primitivos táteis reutilizáveis (`NeobrutalistButton`, `NeobrutalistIconButton`, `NeobrutalistBadge`, `NeobrutalistCard`, `NeobrutalistDialog`, `NeobrutalistProgressBar`, `NeobrutalistSnackbarHost`), atualizando todas as telas, cartões, controles de auto-scroll e diálogos — mantendo 100% intactas as páginas renderizadas do PDF, os fluxos de negócio, a camada `core/` e a suíte de testes existente.

## Technical Context

**Language/Version**: Kotlin 2.4.0 (JVM target 17 via DSL `compilerOptions`)  
**Primary Dependencies**: Jetpack Compose (BOM `2024.11.00`), Material 3, Material Icons Extended, `material3-window-size-class`, `androidx.compose.material3.adaptive` — zero novas dependências externas  
**Storage**: N/A (nenhuma alteração nos DataStores `pdf_reader_prefs`, `recents_prefs` e `favorite_folders_prefs`)  
**Testing**: JUnit 4 + testes JVM de contrato/contraste WCAG 2.2 AA (`./gradlew testDebugUnitTest`), Android Lint (`./gradlew lintDebug`), Compose UI tests (`app/src/androidTest/.../ReaderScreenTest.kt`) e build (`./gradlew assembleDebug`)  
**Target Platform**: Android (`minSdk = 26`, `compileSdk = 35`, `targetSdk = 35` — smartphones, dobráveis e tablets)  
**Project Type**: Aplicativo Android nativo (`:app` único, Clean Architecture + MVI)  
**Performance Goals**: 60fps estáveis no scroll da biblioteca com fundo quadriculado fixo (`24dp x 24dp`) e no loop de auto-scroll do PDF (`withFrameNanos`); zero alocação pesada por frame em `drawBehind`  
**Constraints**: Contraste WCAG 2.2 AA (`>= 4,5:1` texto normal; `>= 3:1` grande/componentes); zero `dynamicColor`; zero sombras difusas/blur; superfícies 100% opacas sobre o grid; páginas PDF preservadas sem filtros ou grid; zero regressão funcional  
**Scale/Scope**: ~13 arquivos Kotlin de UI (`ui/theme/*`, `ui/components/*`, `ui/home/HomeScreen.kt`), 1 novo arquivo de testes JVM de contrato de tema e verificação do `ReaderScreenTest.kt`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Smooth & Predictable Rendering (Performance First)**: PASS — Os modificadores `neobrutalistGridBackground` e `neobrutalistShadow` executam desenho direto em `drawBehind` sem blur, sem alocação de `Bitmap` e sem impacto na thread principal ou no loop `withFrameNanos` de `PdfDocumentViewport`.
- **II. Modular & Engine-Agnostic Core (Library-First)**: PASS — Todo o pacote `core/` (`engine`, `backup`, `home`, `model`, `repository`, `storage`) permanece 100% desacoplado e intocado. Regras puras de atribuição determinística de cor e métricas ficam testáveis na JVM sem dependência do framework Android.
- **III. Test-First & Quality Gates (NON-NEGOTIABLE)**: PASS — Testes unitários JVM de contrato visual (`NeobrutalismThemeContractTest`) validando valores exatos Light & Catppuccin Mocha, opacidade 100%, métricas de borda/sombra/grid, atribuição de cor de pasta e razões de contraste WCAG 2.2 AA são escritos antes da implementação (TDD), executando junto à suíte completa (`./gradlew testDebugUnitTest`), `./gradlew lintDebug` e `./gradlew assembleDebug`.
- **IV. Responsive & Accessible Reader Experience**: PASS — Layouts Compacto, Médio e Expandido recebem margens anti-clipping para sombras deslocadas (`4dp`), painel lateral expandido dimensionado em `276.dp` com rolagem vertical para acomodar `SpeedSelectorControl` sem cortes, atalhos de teclado (`Space`, `+`, `-`) e `contentDescription` preservados integralmente, além de contraste WCAG 2.2 AA verificado matematicamente.
- **V. Observability & Graceful Error Recovery**: PASS — `ErrorDialog`, `SnackbarHost` e tratamento de falhas SAF continuam intactos e ganham semântica visual de erro em alto contraste sem mascarar mensagens nem fluxos de recuperação.

## Project Structure

### Documentation (this feature)

```text
specs/005-neo-brutalism-theme/
├── spec.md                                     # Especificação funcional e critérios mensuráveis
├── plan.md                                     # Este plano técnico
├── research.md                                 # Decisões arquiteturais e auditoria WCAG 2.2 AA
├── data-model.md                               # Tokens de cor, métricas e estados táteis
├── quickstart.md                               # Roteiro de verificação automatizada e QA manual
├── checklists/
│   └── requirements.md                         # Checklist de qualidade da spec
├── contracts/
│   └── neobrutalism-design-system.md           # Contrato público do sistema de design e telas
└── tasks.md                                    # Tarefas ordenadas por fase e User Story
```

### Source Code (repository root)

```text
app/src/main/
├── AndroidManifest.xml                         # UPDATE: Inclui uiMode em configChanges para troca imediata de tema sem recriar Activity
└── java/com/madruga665/pdfreader/
    ├── MainActivity.kt                         # UPDATE: Reutiliza readerViewModel.repository e preserva currentScreenState via onSaveInstanceState
    ├── core/                                   # INALTERADO (zero impacto em domínio, engine, backup ou DataStore)
    └── ui/
        ├── reader/
        │   └── ReaderViewModel.kt              # UPDATE: Expõe val repository para compartilhamento seguro com a Activity
        ├── theme/
        │   ├── Color.kt                        # UPDATE: NeobrutalismColors, Light & Catppuccin Mocha palettes, NeobrutalismFolderAccent
        │   ├── Type.kt                         # UPDATE: NeobrutalismTypography (SansSerif, Bold/ExtraBold hierarchy)
        │   ├── Modifiers.kt                    # NEW: NeobrutalismMetrics, neobrutalistShadow, neobrutalistGridBackground
        │   └── Theme.kt                        # UPDATE: LocalNeobrutalismColors, NeobrutalismTheme, PdfReaderTheme (sem dynamicColor + system bars)
        ├── components/
        │   ├── NeobrutalistPrimitives.kt       # NEW: NeobrutalistButton, IconButton, Badge, Card, Dialog, ProgressBar, SnackbarHost
        │   ├── HomeEmptyState.kt               # UPDATE: Cartão opaco Neo-Brutalism + botões táteis
        │   ├── RecentDocumentCard.kt           # UPDATE: Superfície 100% opaca, sombra dura, barra Neo-Brutalism
        │   ├── FavoriteFolderCard.kt           # UPDATE: Visual de fichário com aba colorida, caixa de ícone, badge e dropdown opaco
        │   ├── AdaptiveReaderScaffold.kt       # UPDATE: TopAppBar com badge Page X of Y, painel lateral expandido opaco, surround neutro sem grid
        │   ├── FloatingAutoScrollOverlay.kt    # UPDATE: Painel opaco sem blur, Play/Pause contrastado pausado vs ativo
        │   ├── SpeedSelectorControl.kt         # UPDATE: Chips, -/+, slider, badge px/s, badge padrão salvo e botões sem clipping
        │   ├── BackupDialogs.kt                # UPDATE: Migrar BackupActionsDialog e RestorePreviewDialog para NeobrutalistDialog
        │   ├── FolderContentsDialog.kt         # UPDATE: Migrar para NeobrutalistDialog com lista delimitada e Unpin destrutivo
        │   ├── ErrorDialog.kt                  # UPDATE: Migrar para NeobrutalistDialog com destaque semântico de erro
        │   ├── PdfDocumentViewport.kt          # AUDIT ONLY: Manter renderização/gestos/auto-scroll 100% intactos (RN-03)
        │   └── PdfPageItem.kt                  # AUDIT ONLY: Manter bitmaps PDF intactos; loading herda tema atualizado (RN-03)
        └── home/
            └── HomeScreen.kt                   # UPDATE: Grid 24dp fixo, TopAppBar estilizada, SectionHeader com badge, FAB/ExtendedFAB tátil, SnackbarHost

app/src/test/java/com/madruga665/pdfreader/
└── ui/
    └── theme/
        └── NeobrutalismThemeContractTest.kt    # NEW: Testes JVM de paletas, opacidade, métricas, acento de pasta e contraste WCAG 2.2 AA

app/src/androidTest/java/com/madruga665/pdfreader/
└── ui/
    └── ReaderScreenTest.kt                     # VERIFY / EXTEND: Confirmar testes Compose existentes com novo tema Claro/Escuro
```

**Structure Decision**: Manter a estrutura single-module `:app` existente (Clean Architecture + MVI), concentrando toda a fundação visual em `ui/theme/` e `ui/components/NeobrutalistPrimitives.kt` para eliminar duplicação de bordas/sombras/animações táteis nos 11 componentes de tela afetados.

## Complexity Tracking

Nenhuma violação à Constituição identificada. Zero novas dependências de terceiros adicionadas.
