# Research: Aplicar Estilo Neo-Brutalism (Tema Claro e Escuro Catppuccin Mocha)

**Feature**: `005-neo-brutalism-theme`  
**Date**: 2026-11-25  
**References Audited**:
- Repositório alvo: `/home/madruga665/Projetos/madruga665-pdf-reader` (`ui/theme/`, `ui/home/`, `ui/components/`, `MainActivity.kt`, `ReaderScreenTest.kt`)
- Referência Android: `/home/madruga665/Projetos/madruga665-bookmarks/madruga665-bookmarks-app` (`Color.kt`, `Theme.kt`, `Type.kt`, `Modifiers.kt`, `NeobrutalistButton.kt`, `NeobrutalistFolderCard.kt`, `NeobrutalistBookmarkCard.kt`, `DeleteConfirmationDialog.kt`, `SyncStatusBadge.kt`, `specs/012-grid-background/contracts/ui-grid-background-contract.md`, `GridBackgroundTest.kt`)
- Referência Desktop/Web: `ThemeTokens.qml` (`madruga665-bookmarks-desktop`) e estilos Neo-Brutalism do `madruga665-site`

---

## 1. Desativação de Dynamic Color & Compatibilidade da API de Tema

- **Diagnóstico atual**: `app/src/main/java/com/madruga665/pdfreader/ui/theme/Theme.kt` define `PdfReaderTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit)` usando `Purple80`/`Purple40` e `dynamicLightColorScheme`/`dynamicDarkColorScheme` em Android 12+.
- **Decisão**: Remover o suporte ativo a cores dinâmicas na composição padrão de `PdfReaderTheme`, expondo `PdfReaderTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)` (preservando chamada sem parâmetros em `MainActivity.kt` e `ReaderScreenTest.kt`) e fornecendo `LocalNeobrutalismColors` via `CompositionLocalProvider` junto ao objeto utilitário `NeobrutalismTheme.colors` e `NeobrutalismTheme.typography`.
- **Mapeamento Material 3**: Mapear integralmente `lightColorScheme` / `darkColorScheme` dentro de `PdfReaderTheme` (`primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `secondary`, `onSecondary`, `secondaryContainer`, `onSecondaryContainer`, `background`, `onBackground`, `surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`, `outline`, `outlineVariant`, `error`, `onError`) para que componentes M3 reutilizados (`Slider`, `RadioButton`, `CircularProgressIndicator`, `LinearProgressIndicator`) nunca herdem o roxo default do M3.

---

## 2. Auditoria Matemática de Contraste WCAG 2.2 AA

Cálculo de luminância relativa WCAG 2.2 (`>= 4,5:1` texto normal; `>= 3,0:1` texto grande e componentes gráficos):

| Par (Foreground / Background) | Tema Claro | Razão Claro | Tema Escuro (Mocha) | Razão Mocha | Decisão |
|---|---|---|---|---|---|
| Texto principal / Superfície | `#000000` / `#FFFFFF` | **21,00:1** | `#CDD6F4` / `#313244` | **8,69:1** | Aprovado (AAA) |
| Texto principal / Fundo base | `#000000` / `#F8F8F8` | **19,77:1** | `#CDD6F4` / `#1E1E2E` | **11,34:1** | Aprovado (AAA) |
| Subtexto / Superfície | `#555555` / `#FFFFFF` | **7,46:1** | `#A6ADC8` / `#313244` | **5,65:1** | Aprovado (AA+) |
| Subtexto / Fundo base | `#555555` / `#F8F8F8` | **7,02:1** | `#A6ADC8` / `#1E1E2E` | **7,37:1** | Aprovado (AA+) |
| `onAccent` / Amarelo primário | `#000000` / `#FFD600` | **14,87:1** | `#11111B` / `#F9E2AF` | **14,76:1** | Aprovado (AAA) |
| `onAccent` / Roxo | `#000000` / `#7C5CFF` | **4,83:1** | `#11111B` / `#CBA6F7` | **9,23:1** | **Obrigatório usar `#000000` no Claro** (`#FFFFFF` sobre `#7C5CFF` dá 4,35:1 e falha texto normal) |
| `onAccent` / Laranja | `#000000` / `#FF6B00` | **7,36:1** | `#11111B` / `#FAB387` | **10,59:1** | **Obrigatório usar `#000000` no Claro** (`#FFFFFF` sobre `#FF6B00` dá 2,86:1 e falha!) |
| `onAccent` / Azul | `#000000` / `#3399FF` | **7,14:1** | `#11111B` / `#89B4FA` | **8,91:1** | Aprovado (AA+) |
| `onAccent` / Verde | `#000000` / `#22C55E` | **9,22:1** | `#11111B` / `#A6E3A1` | **12,61:1** | Aprovado (AAA) |
| `onAccent` / Vermelho (botões destrutivos) | `#000000` / `#FF4B4B` | **6,36:1** | `#11111B` / `#F38BA8` | **8,10:1** | **Obrigatório usar `#000000` sobre `#FF4B4B`** (`#FFFFFF` dá 3,30:1 e falha texto normal) |
| Texto de erro direto em superfície | `#C62828` / `#FFFFFF` | **5,62:1** | `#F38BA8` / `#313244` | **5,43:1** | Usar `errorText` (`#C62828` no Claro / `#F38BA8` no Mocha) em itens de texto como `"Unpin folder"` e `"Restaurar"` sobre fundo claro |

- **Ponto crítico descoberto**: Em `SpeedSelectorControl.kt`, o texto `"Padrão salvo: X px/s"` atualmente usa `color = MaterialTheme.colorScheme.primary`. Como no tema Neo-Brutalism o acento primário é amarelo (`#FFD600` / `#F9E2AF`), aplicar amarelo direto como cor de texto sobre superfície branca (`#FFFFFF`) teria contraste catastrófico (~1,4:1). Solução: renderizar `"Padrão salvo: X px/s"` como **badge preenchida** com `containerColor = accentYellow` e texto/ícone `onAccent` (`#000000` no Claro, `#11111B` no Escuro) — atingindo **14,87:1** (Claro) e **14,76:1** (Escuro)!

---

## 3. Sombras Duras Sem Desfoque & Regra Anti-Clipping

- **Referência `madruga665-bookmarks-app` (`Modifiers.kt`)**:
  ```kotlin
  Modifier
      .graphicsLayer { shadowElevation = 0f }
      .drawBehind {
          val shadowPx = shadowOffset.toPx()
          val outline = shape.createOutline(size, layoutDirection, this)
          drawContext.canvas.save()
          drawContext.canvas.translate(shadowPx, shadowPx)
          drawOutline(outline = outline, color = shadowColor)
          drawContext.canvas.restore()
      }
      .border(borderWidth, borderColor, shape)
  ```
- **Métricas alinhadas ao PRD (`2dp`–`3dp` borda, `2dp`–`4dp` sombra, `8dp`–`16dp` cantos)**:
  - Cartões principais, diálogos, painel flutuante, FAB: `borderWidth = 2.5.dp`, `shadowOffset = 4.dp`, `shape = RoundedCornerShape(12.dp)` ou `16.dp`.
  - Aba de pasta, caixa de ícone interna, botões compactos, ações de `TopAppBar`, chips, badges: `borderWidth = 2.dp`, `shadowOffset = 2.dp`, `shape = RoundedCornerShape(8.dp)` ou `10.dp`.
- **Prevenção de recorte (`clipping`)**:
  1. Em Compose, `Modifier.clip(shape)` corta tudo o que é desenhado depois dele no eixo fora dos bounds — portanto `.clip()` NUNCA deve envolver a chamada a `.neobrutalistShadow()`, e o `background(color, shape)` deve ser aplicado usando `shape` (sem `clip` externo cortando o deslocamento `+shadowOffset` à direita/baixo).
  2. Em `LazyRow` (`HomeScreen.kt`), adicionar `contentPadding = PaddingValues(start = 16.dp, end = 20.dp, bottom = 6.dp)` (no modo expandido `start = 0.dp, end = 6.dp, bottom = 6.dp` dentro da grade que já tem `24.dp` laterais) para que a sombra `4dp` do primeiro/último `RecentDocumentCard` e da base do card não seja cortada pelas bordas do viewport da `LazyRow`.
  3. Em `LazyColumn` e `LazyVerticalGrid`, garantir `padding`/`contentPadding` inferior e lateral >= `shadowOffset` (`4dp`+) e `Arrangement.spacedBy(12.dp..16.dp)`.
  4. Em `NeobrutalistButton`, o deslocamento ao pressionar move o botão até `(shadowOffset - 1.dp)` em `(x, y)` enquanto a sombra encolhe de `shadowOffset` para `1.dp` — o limite máximo de ocupação visual permanece `width + shadowOffset` e `height + shadowOffset`, evitando saltos de layout. Para `NeobrutalistButton` e `FloatingActionButton`, reservar `padding(end = shadowOffset, bottom = shadowOffset)` quando posicionados rente às bordas (como o `FAB` no `Scaffold` ou botões dentro de filas apertadas).

---

## 4. Fundo Quadriculado Fixo & Opacidade Total

- **Contrato (`ui-grid-background-contract.md`)**: `Modifier.neobrutalistGridBackground(gridColor, gridSize = 24.dp, strokeWidth = 1.dp)` desenha linhas verticais e horizontais via `drawBehind` no contêiner raiz fixo da `HomeScreen` (atrás de `HomeEmptyState`, `CompactHomeContent` e `ExpandedHomeContent`). Como o modifier está no `Box` raiz externo às listas (`LazyColumn`/`LazyVerticalGrid`), o grid permanece fixo durante o scroll a custo mínimo de draw pass (`60fps`, `RNF-01`).
- **Correções obrigatórias de transparência detectadas no código atual**:
  - `RecentDocumentCard.kt:L46`: usa `MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)` → **substituir** por `NeobrutalismTheme.colors.surface` (100% opaco, `RN-04`).
  - `FloatingAutoScrollOverlay.kt:L51`: usa `MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)` + `tonalElevation = 8.dp` + `shadowElevation = 8.dp` → **substituir** por `NeobrutalismTheme.colors.surface` (100% opaco, `0.dp` elevação tonal/difusa, `RN-02`/`RN-04`).
  - `FolderContentsDialog.kt:L102` e `SpeedSelectorControl.kt:L98`: usam `outlineVariant.copy(alpha = 0.5f)` em `HorizontalDivider` → usar divisores sólidos nítidos (`colors.border` ou `colors.border.copy(alpha = 0.25f)` apenas em separadores sutis de lista, mantendo `colors.border` sólido `2dp` nas divisórias estruturais entre barra/painel e cabeçalhos).

---

## 5. Preservação do PDF & Ergonomia do Leitor (`AdaptiveReaderScaffold` e `SpeedSelectorControl`)

- **Preservação do PDF (`RN-03`)**: `PdfDocumentViewport.kt` e `PdfPageItem.kt` renderizam bitmaps do `PdfRenderer` sem qualquer modificação de cor ou filtro. O surround da viewport no `AdaptiveReaderScaffold` recebe fundo neutro opaco dedicado (`readerViewportBackground`: `#EAEAEA` no Claro, `#181825` Mocha Mantle no Escuro) tanto no modo Compacto quanto no Expandido — diferenciando claramente folhas brancas sem introduzir grid quadriculado atrás das páginas.
- **Correções estruturais de layout identificadas no leitor**:
  1. `AdaptiveReaderScaffold.kt:L64-68`: o título na `TopAppBar` tem `maxLines = 1` sem `overflow = TextOverflow.Ellipsis`. Adicionar `overflow = TextOverflow.Ellipsis` com `Modifier.weight(1f)` implícito do `TopAppBar` para não empurrar o badge `"Page X of Y"` nem cortar abruptamente.
  2. `AdaptiveReaderScaffold.kt:L99-125`: no modo Expandido (`WindowWidthSizeClass.Expanded`), o código atual coloca `SpeedSelectorControl` dentro de `NavigationRail` (`widthIn(max = 240.dp)`, sem scroll vertical garantido no rail). Um `NavigationRail` M3 possui largura mínima/padrão estreita (80dp) e padding interno incompatível com os botões `"Salvar neste PDF"` / `"Restaurar"` e chips de preset. Substituir por uma `Column` dedicada (`width = 276.dp`, `fillMaxHeight()`, `background(colors.surface)`, `verticalScroll(rememberScrollState())`) separada da viewport por uma borda vertical sólida à direita (`2.5.dp` `colors.border`), mantendo o `Box` viewport com `Modifier.weight(1f).fillMaxHeight()`.
  3. `SpeedSelectorControl.kt:L102-165`: colocar o indicador `"Padrão salvo: X px/s"` / `"Padrão geral do app"` em sua própria linha (badge/pill acima ou alinhada à esquerda) e os botões `"Restaurar"` e `"Salvar neste PDF"` em uma linha dedicada com `Arrangement.spacedBy(8.dp)` quando em larguras estreitas (ou fluxo flexível `FlowRow` / duas linhas limpas), evitando compressão dos rótulos em 360dp e no painel lateral de 276dp.

---

## 6. Diálogos, Menus e Feedbacks

- `BackupDialogs.kt`, `FolderContentsDialog.kt` e `ErrorDialog.kt` atualmente usam Material 3 `AlertDialog` (cantos 28dp e sombra difusa padrão). Criar um componente compartilhado `NeobrutalistDialog` usando `androidx.compose.ui.window.Dialog(onDismissRequest = ..., properties = DialogProperties(usePlatformDefaultWidth = false))` com padding externo (`24.dp`), largura limitada (`widthIn(max = 480.dp)`), sombra dura `4.dp`, borda `2.5.dp`, cantos `16.dp` e superfície 100% opaca.
- `FavoriteFolderCard.kt`: `DropdownMenu` do Compose Material 3 aceita `containerColor`, `tonalElevation = 0.dp`, `shadowElevation = 0.dp`, `border = BorderStroke(2.5.dp, colors.border)` e `shape = RoundedCornerShape(12.dp)` na versão Compose BOM `2024.11.00`. Caso `DropdownMenu` apresente limitação em alguma propriedade de assinatura, verificar no código durante implementação ou envolver conteúdo interno em `Column` opaca com borda/sombra; no BOM `2024.11.00` (Material 3 1.3.x), `DropdownMenu` expõe `shape`, `containerColor`, `tonalElevation`, `shadowElevation` e `border`.
- `HomeScreen.kt` `SnackbarHost`: usar `NeobrutalistSnackbarHost` customizando o `Snackbar` lambda para renderizar cartão opaco com borda `2.5.dp`, sombra dura `4.dp` (destaque semântico de erro ou informação em alto contraste), mantendo `SnackbarHostState.showSnackbar(error)` e `HomeIntent.DismissError` idênticos.

---

## 7. Sincronia das Barras do Sistema (`RNF-03`)

- Em `Theme.kt`, atualizar `WindowCompat.getInsetsController(window, view)` dentro de `SideEffect` (apenas quando `!view.isInEditMode` e o `Context` resolver para `Activity`) ajustando `isAppearanceLightStatusBars = !darkTheme` e `isAppearanceLightNavigationBars = !darkTheme` e `window.statusBarColor` / `navigationBarColor` quando aplicável, assegurando que os ícones da barra de status fiquem escuros no Claro (`#F8F8F8` / `#FFFFFF`) e claros no Escuro (`#1E1E2E`).
