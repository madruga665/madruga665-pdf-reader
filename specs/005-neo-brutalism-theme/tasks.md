---

description: "Actionable, dependency-ordered task list for applying Neo-Brutalism design system (Light & Catppuccin Mocha Dark)"
---

# Tasks: Aplicar Estilo Neo-Brutalism (Tema Claro e Escuro Catppuccin Mocha)

**Input**: Design documents from `/specs/005-neo-brutalism-theme/`  
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/neobrutalism-design-system.md`, `quickstart.md`

**Tests**: Obrigatórios (TDD é mandatório pela Constituição Princípio III e `AGENTS.md`). Escrever e validar testes JVM de contrato de tema e contraste WCAG 2.2 AA antes da implementação dos tokens e componentes.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos distintos, sem dependência bloqueante)
- **[Story]**: Rastreabilidade para a User Story (`[US1]`, `[US2]`, `[US3]`, `[US4]`, `[US5]`)
- Caminhos exatos incluídos em todas as tarefas

---

## Phase 1: Setup & TDD Theme Contracts

**Purpose**: Estabelecer testes unitários JVM de contrato visual, métricas, opacidade e conformidade WCAG 2.2 AA antes de alterar os arquivos de tema.

- [x] T001 [P] [US1] Create `NeobrutalismThemeContractTest` verifying Light (`#F8F8F8`, `#FFFFFF`, `#000000`, `#555555`, `#E2E2E2`, `#FFD600`, `#7C5CFF`, `#FF6B00`, `#3399FF`) and Catppuccin Mocha Dark (`#1E1E2E`, `#313244`, `#CDD6F4`, `#A6ADC8`, `#11111B`, `#28283D`, `#F9E2AF`, `#CBA6F7`, `#FAB387`, `#89B4FA`, `#A6E3A1`, `#F38BA8`) color tokens, 100% surface opacity (`alpha == 1f`), grid line distinctness, `NeobrutalismMetrics` bounds (`2dp..3dp` border, `2dp..4dp` shadow, `8dp..16dp` corners, `24dp` grid), deterministic `NeobrutalismFolderAccent` mapping, and WCAG 2.2 AA contrast ratios (`>= 4.5:1` for text/onAccent pairs) in `app/src/test/java/com/madruga665/pdfreader/ui/theme/NeobrutalismThemeContractTest.kt`
- [x] T002 [P] [US1] Run `./gradlew testDebugUnitTest --tests "com.madruga665.pdfreader.ui.theme.NeobrutalismThemeContractTest"` to confirm TDD red phase before theme implementation in `/home/madruga665/Projetos/madruga665-pdf-reader`

---

## Phase 2: Foundational — Design System Tokens, Modifiers & Reusable Primitives (`[US1]`)

**Goal**: Substituir o tema Material roxo/dinâmico pela fundação determinística Neo-Brutalism (Claro e Catppuccin Mocha Escuro) e disponibilizar modificadores e componentes táteis reutilizáveis para todas as telas e diálogos.

**Independent Test**: `NeobrutalismThemeContractTest` passa 100% na JVM e `PdfReaderTheme` compila sem `dynamicColor` em `MainActivity.kt` e `ReaderScreenTest.kt`.

- [x] T003 [P] [US1] Implement `NeobrutalismColors`, `LightNeobrutalismColors`, `MochaDarkNeobrutalismColors`, and `NeobrutalismFolderAccent` (removing legacy `Purple*` / `Pink*` / semi-transparent overlay constants) in `app/src/main/java/com/madruga665/pdfreader/ui/theme/Color.kt`
- [x] T004 [P] [US1] Implement `NeobrutalismTypography` (and keep `Typography` alias) using `FontFamily.SansSerif` with `ExtraBold`/`Bold` display, title, and action label hierarchy in `app/src/main/java/com/madruga665/pdfreader/ui/theme/Type.kt`
- [x] T005 [P] [US1] Create `NeobrutalismMetrics`, `Modifier.neobrutalistShadow`, and `Modifier.neobrutalistGridBackground` (`drawBehind` zero-blur hard shadow + `24dp x 24dp` `1dp` grid) in `app/src/main/java/com/madruga665/pdfreader/ui/theme/Modifiers.kt`
- [x] T006 [US1] Update `PdfReaderTheme` to disable `dynamicColor`, provide `LocalNeobrutalismColors` and `NeobrutalismTheme`, map full Material 3 `lightColorScheme`/`darkColorScheme`, and sync system bar icon luminance via `WindowCompat.getInsetsController` in `app/src/main/java/com/madruga665/pdfreader/ui/theme/Theme.kt` (depends on T003, T004)
- [x] T007 [US1] Create reusable tactile and opaque primitives (`NeobrutalistButton` with `text` and `content` overloads + `animateDpAsState` press compression, `NeobrutalistIconButton`, `NeobrutalistBadge`, `NeobrutalistCard`, `NeobrutalistDialog`, `NeobrutalistProgressBar`, `NeobrutalistSnackbarHost`) in `app/src/main/java/com/madruga665/pdfreader/ui/components/NeobrutalistPrimitives.kt` (depends on T003, T004, T005, T006)
- [x] T008 [US1] Verify `NeobrutalismThemeContractTest` passes green on JVM via `./gradlew testDebugUnitTest --tests "com.madruga665.pdfreader.ui.theme.NeobrutalismThemeContractTest"`

**Checkpoint**: Fundação de design system pronta e verificada — implementação das telas pode prosseguir em paralelo por história.

---

## Phase 3: User Story 2 — Tela Inicial, Grid Fixo, Estado Vazio e Cartões (`[US2]`)

**Goal**: Aplicar Neo-Brutalism completo na `HomeScreen`, `HomeEmptyState`, `RecentDocumentCard` e `FavoriteFolderCard`, garantindo grid `24dp` fixo, superfícies 100% opacas, visual de fichário nas pastas, dropdown opaco e zero clipping em Compacto e Expandido.

**Independent Test**: Renderizar Home vazia e populada em smartphone e tablet (Claro e Escuro), verificando grid fixo no scroll, opacidade de cartões/barras/FAB, abertura/remoção de recente e menu `"Unpin folder"`.

- [x] T009 [P] [US2] Redesign `HomeEmptyState` as an opaque `NeobrutalistCard` over the grid with framed `MenuBook` accent icon (`accentYellow`) and tactile `"Open PDF"` (primary) and `"Pin Folder"` (secondary) `NeobrutalistButton`s in `app/src/main/java/com/madruga665/pdfreader/ui/components/HomeEmptyState.kt`
- [x] T010 [P] [US2] Redesign `RecentDocumentCard` with 100% opaque `surface` (removing `alpha = 0.7f`), `2.5.dp` border, `4.dp` hard shadow without pre-shadow clipping, framed PDF icon box, 1-line truncated title, delimited `X` remove button, `"Page X of Y • Z%"` text, and `NeobrutalistProgressBar` in `app/src/main/java/com/madruga665/pdfreader/ui/components/RecentDocumentCard.kt`
- [x] T011 [P] [US2] Redesign `FavoriteFolderCard` inspired by `madruga665-bookmarks` (colored top folder tab + `46.dp` framed icon box using `NeobrutalismFolderAccent`, `100%` opaque `surface` body with `2.5.dp` border and `4.dp` shadow, `NeobrutalistBadge` for `"1 PDF"`/`"N PDFs"`, and zero-elevation opaque `DropdownMenu` with `2.5.dp` border and `errorText` `"Unpin folder"`) in `app/src/main/java/com/madruga665/pdfreader/ui/components/FavoriteFolderCard.kt`
- [x] T012 [US2] Update `HomeScreen` to attach `background(colors.background).neobrutalistGridBackground(colors.gridLine)` to the fixed root container, restyle `TopAppBar` (framed brand icon, bold `"PDF Reader"`, solid bottom border, `NeobrutalistIconButton` actions preserving all `contentDescription`s), update `SectionHeader` with `NeobrutalistBadge` count, apply tactile Neo-Brutalism `FAB` / `ExtendedFloatingActionButton`, restyle backup `LinearProgressIndicator` and `NeobrutalistSnackbarHost`, and enforce anti-clipping padding on `LazyRow`, `LazyColumn`, `LazyVerticalGrid`, and `FAB` in `app/src/main/java/com/madruga665/pdfreader/ui/home/HomeScreen.kt` (depends on T009, T010, T011)

**Checkpoint**: User Story 2 completa e verificável de forma independente na Tela Inicial.

---

## Phase 4: User Story 3 — Tela do Leitor, Preservação PDF e Controles de Auto-Scroll (`[US3]`)

**Goal**: Estilizar a tela do leitor, o overlay flutuante compacto e o painel lateral expandido em Neo-Brutalism mantendo ergonomia em `360dp` e tablets, sem interferir na renderização das páginas PDF (`RN-03`).

**Independent Test**: Abrir PDF no leitor em smartphone e tablet (Claro e Escuro), confirmar páginas PDF íntegras sem grid/bordas, alternar visibilidade do overlay ao tocar na folha, acionar Play/Pause, presets, `-`/`+`, slider, `"Salvar neste PDF"` e `"Restaurar"`.

- [x] T013 [P] [US3] Redesign `SpeedSelectorControl` with Neo-Brutalism preset chips (`Slow`, `Medium`, `Fast` with `accentPurple` + `onAccent` when selected), square `NeobrutalistIconButton` `-`/`+`, high-contrast `Slider` (`10f..500f`), prominent `px/s` badge, solid divider, and non-cramping saved-speed row (`NeobrutalistBadge` `"Padrão salvo: X px/s"` in `accentYellow`, destructive `"Restaurar"`, and tactile `"Salvar neste PDF"` button) in `app/src/main/java/com/madruga665/pdfreader/ui/components/SpeedSelectorControl.kt`
- [x] T014 [P] [US3] Redesign `FloatingAutoScrollOverlay` removing `alpha = 0.95f` and M3 tonal/blur elevations in favor of an opaque `NeobrutalistCard` (`2.5.dp` border, `4.dp` hard shadow, `16.dp` radius) with high-contrast tactile Play/Pause control (`accentYellow` paused vs `accentOrange` scrolling, `onAccent` icon) in `app/src/main/java/com/madruga665/pdfreader/ui/components/FloatingAutoScrollOverlay.kt` (depends on T013)
- [x] T015 [US3] Update `AdaptiveReaderScaffold` with opaque Neo-Brutalism `TopAppBar` (solid bottom border, tactile Back/Open PDF icon buttons, ellipsized 1-line title, `"Page X of Y"` `NeobrutalistBadge`), neutral `readerViewportBackground` without grid behind PDFs, dedicated scrollable `276.dp` opaque side panel with solid `2.5.dp` vertical divider and Play/Pause button on `Expanded`, and Neo-Brutalism loading / `"No PDF document opened"` states in `app/src/main/java/com/madruga665/pdfreader/ui/components/AdaptiveReaderScaffold.kt` (depends on T013, T014)
- [x] T016 [US3] Audit `PdfDocumentViewport` and `PdfPageItem` to verify zero visual alteration, zero filter/inversion, zero invasive page borders, and zero grid interference on rendered PDF pages (`RN-03`), keeping only theme-consistent loading spinner color in `app/src/main/java/com/madruga665/pdfreader/ui/components/PdfDocumentViewport.kt` and `app/src/main/java/com/madruga665/pdfreader/ui/components/PdfPageItem.kt`

**Checkpoint**: User Story 3 completa — leitor e controles de auto-scroll integrados nos layouts Compacto e Expandido.

---

## Phase 5: User Story 4 — Diálogos de Backup, Pasta e Erro (`[US4]`)

**Goal**: Migrar todos os diálogos (`BackupActionsDialog`, `RestorePreviewDialog`, `FolderContentsDialog`, `ErrorDialog`) do `AlertDialog` genérico para `NeobrutalistDialog` 100% opaco com hierarquia visual clara e preservação integral dos fluxos.

**Independent Test**: Abrir cada diálogo nos modos Claro e Escuro, alternar `Mesclar (padrão)` vs `Substituir tudo`, navegar na lista de PDFs da pasta (e pasta vazia) e acionar `ErrorDialog`.

- [x] T017 [P] [US4] Migrate `BackupActionsDialog` and `RestorePreviewDialog` to `NeobrutalistDialog` with framed icon, bordered summary container, clearly highlighted `Mesclar (padrão)` vs `Substituir tudo` selectable option cards, solid divider, and tactile primary/secondary `NeobrutalistButton` hierarchy in `app/src/main/java/com/madruga665/pdfreader/ui/components/BackupDialogs.kt`
- [x] T018 [P] [US4] Migrate `FolderContentsDialog` to `NeobrutalistDialog` with framed folder header, crisp bordered/divided `FolderPdfListItem` rows, styled loading/empty states, destructive `accentRed` `"Unpin"` `NeobrutalistButton`, and secondary `"Close"` `NeobrutalistButton` in `app/src/main/java/com/madruga665/pdfreader/ui/components/FolderContentsDialog.kt`
- [x] T019 [P] [US4] Migrate `ErrorDialog` to `NeobrutalistDialog` with `accentRed` semantic error badge/header, readable body contrast, primary `"Choose Another File"` button, and secondary `"Dismiss"` button in `app/src/main/java/com/madruga665/pdfreader/ui/components/ErrorDialog.kt`

**Checkpoint**: User Story 4 completa — todos os diálogos modais e feedbacks unificados em Neo-Brutalism.

---

## Phase 6: User Story 5 & Polish — Responsividade Anti-Clipping, Testes e Portões de Validação (`[US5]`)

**Goal**: Garantir ausência de clipping de bordas/sombras em todas as larguras (`360dp` a tablets), compatibilidade dos testes UI Compose, zero regressões funcionais e aprovação completa nos portões de qualidade.

- [x] T020 [US5] Audit and tune padding, spacing, and `widthIn`/`contentPadding` margins across `HomeScreen`, `RecentDocumentCard`, `FavoriteFolderCard`, `AdaptiveReaderScaffold`, `FloatingAutoScrollOverlay`, `SpeedSelectorControl`, and all dialogs to guarantee zero shadow/border clipping and zero text overlap at `360dp` portrait, landscape, and tablet sizes
- [x] T021 [P] [US5] Verify and extend Compose UI tests in `app/src/androidTest/java/com/madruga665/pdfreader/ui/ReaderScreenTest.kt` to cover `PdfReaderTheme(darkTheme = false)` and `PdfReaderTheme(darkTheme = true)` rendering without errors while preserving existing assertions (`"No PDF document opened"`, `"Open PDF"`)
- [x] T022 [US5] Run full JVM unit test suite (`./gradlew testDebugUnitTest`) and confirm 100% of existing tests plus `NeobrutalismThemeContractTest` pass
- [x] T023 [US5] Run Android static lint (`./gradlew lintDebug`) and resolve any layout, accessibility, or API warnings introduced by theme/UI updates
- [x] T024 [US5] Build debug APK (`./gradlew assembleDebug`) and confirm zero compilation errors
- [ ] T025 [US5] Complete `specs/005-neo-brutalism-theme/quickstart.md` visual/interactive verification checklist across Light and Catppuccin Mocha Dark modes and update task statuses *(Portões automatizados validados: `testDebugUnitTest`, `compileDebugAndroidTestKotlin`, `lintDebug` e `assembleDebug` 100% aprovados; inspeção visual/interativa em dispositivo físico pendente para QA manual)*

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup & TDD)**: Sem dependências — inicia imediatamente.
- **Phase 2 (Foundational `[US1]`)**: Depende da escrita de T001/T002 (TDD red). **BLOQUEIA** Phases 3, 4 e 5.
- **Phase 3 (`[US2]`), Phase 4 (`[US3]`) e Phase 5 (`[US4]`)**: Todas dependem da conclusão da Phase 2 e podem ser executadas em paralelo entre si (arquivos disjuntos).
- **Phase 6 (`[US5]` & Polish)**: Depende das Phases 3, 4 e 5 concluídas.

### Parallel Opportunities

- `T003`, `T004`, `T005` podem ser implementados em paralelo após `T002`.
- `T009`, `T010`, `T011` podem ser implementados em paralelo antes de consolidar `HomeScreen.kt` (`T012`).
- `T017`, `T018`, `T019` (todos os diálogos) são 100% independentes e paralelizáveis.
- `T013` e `T016` podem rodar em paralelo com a Phase 3 e Phase 5.

---

## Notes

- Nunca usar `dynamicLightColorScheme` ou `dynamicDarkColorScheme` (`RN-01`).
- Nunca aplicar `.clip(...)` antes de `.neobrutalistShadow(...)` para evitar corte da sombra deslocada.
- Nunca colocar grid quadriculado ou filtros visuais atrás/sobre páginas renderizadas do PDF (`RN-03`).
- Preservar integralmente todos os textos/rótulos atuais e `contentDescription` existentes em cada tela e componente (`RN-05`).
