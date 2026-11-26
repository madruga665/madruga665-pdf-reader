# Contract: Sistema de Design Neo-Brutalism (`madruga665-pdf-reader`)

**Feature**: `005-neo-brutalism-theme`  
**Date**: 2026-11-25  
**Target Package**: `com.madruga665.pdfreader.ui.*`

---

## 1. API Pública de Tema e Modificadores

### 1.1 `ui/theme/Color.kt`
- Exporta `@Immutable data class NeobrutalismColors(...)` conforme [data-model.md](../data-model.md).
- Exporta `val LightNeobrutalismColors: NeobrutalismColors` e `val MochaDarkNeobrutalismColors: NeobrutalismColors`.
- Exporta `object NeobrutalismFolderAccent` com função pura `accentForKey(key: String, colors: NeobrutalismColors): Color` (testável na JVM sem dependências Android).

### 1.2 `ui/theme/Type.kt`
- Exporta `val NeobrutalismTypography: Typography` (mantendo alias `val Typography = NeobrutalismTypography` se houver referência externa):
  - `displayLarge`: `SansSerif`, `ExtraBold`, `32.sp`, `lineHeight = 38.sp`, `letterSpacing = (-0.5).sp`
  - `headlineSmall`: `SansSerif`, `ExtraBold`, `22.sp`, `lineHeight = 28.sp`
  - `titleLarge`: `SansSerif`, `ExtraBold`, `20.sp`, `lineHeight = 26.sp`
  - `titleMedium`: `SansSerif`, `Bold`, `16.sp`, `lineHeight = 22.sp`
  - `titleSmall`: `SansSerif`, `Bold`, `14.sp`, `lineHeight = 20.sp`
  - `bodyLarge`: `SansSerif`, `Normal`, `16.sp`, `lineHeight = 24.sp`
  - `bodyMedium`: `SansSerif`, `Normal`, `14.sp`, `lineHeight = 20.sp`
  - `bodySmall`: `SansSerif`, `Medium`, `12.sp`, `lineHeight = 16.sp`
  - `labelLarge`: `SansSerif`, `Bold`, `14.sp`, `lineHeight = 18.sp`
  - `labelMedium`: `SansSerif`, `Bold`, `12.sp`, `lineHeight = 16.sp`
  - `labelSmall`: `SansSerif`, `Bold`, `11.sp`, `lineHeight = 14.sp`

### 1.3 `ui/theme/Modifiers.kt`
```kotlin
object NeobrutalismMetrics {
    val BorderWidth = 2.5.dp
    val BorderWidthCompact = 2.dp
    val ShadowOffset = 4.dp
    val ShadowOffsetCompact = 2.dp
    val PressedShadowOffset = 1.dp
    val CornerRadiusSmall = 8.dp
    val CornerRadiusMedium = 12.dp
    val CornerRadiusLarge = 16.dp
    val GridSize = 24.dp
    val GridStrokeWidth = 1.dp
    val ExpandedReaderRailWidth = 276.dp
}

fun Modifier.neobrutalistShadow(
    shadowColor: Color,
    borderColor: Color,
    borderWidth: Dp = NeobrutalismMetrics.BorderWidth,
    shadowOffset: Dp = NeobrutalismMetrics.ShadowOffset,
    shape: Shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusMedium)
): Modifier

fun Modifier.neobrutalistGridBackground(
    gridColor: Color,
    gridSize: Dp = NeobrutalismMetrics.GridSize,
    strokeWidth: Dp = NeobrutalismMetrics.GridStrokeWidth
): Modifier
```
- **Regras obrigatórias de implementação dos modificadores**:
  1. `neobrutalistShadow` DEVE forçar `graphicsLayer { shadowElevation = 0f }`, desenhar o `Outline` sólido deslocado em `(shadowOffset.toPx(), shadowOffset.toPx())` sem desfoque em `drawBehind`, e aplicar `.border(borderWidth, borderColor, shape)` por cima.
  2. `neobrutalistShadow` NUNCA deve ser precedido por `.clip(shape)` que corte a projeção da sombra deslocada.
  3. `neobrutalistGridBackground` DEVE desenhar somente linhas verticais e horizontais espaçadas em `gridSize` (`24.dp`) com espessura `strokeWidth` (`1.dp`) dentro de `size` em `drawBehind`, sem alterar layout/medição do nó.

### 1.4 `ui/theme/Theme.kt`
```kotlin
val LocalNeobrutalismColors = staticCompositionLocalOf { LightNeobrutalismColors }

object NeobrutalismTheme {
    val colors: NeobrutalismColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNeobrutalismColors.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography
}

@Composable
fun PdfReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
)
```
- `PdfReaderTheme` NUNCA utiliza `dynamicLightColorScheme` nem `dynamicDarkColorScheme`.
- Sincroniza `isAppearanceLightStatusBars` / `isAppearanceLightNavigationBars` com `!darkTheme` quando em contexto `Activity` (e `!view.isInEditMode`).

---

## 2. Primitivos Reutilizáveis (`ui/components/NeobrutalistPrimitives.kt`)

| Componente | Responsabilidade | Assinatura / Contrato |
|---|---|---|
| `NeobrutalistButton` | Botão com borda sólida, sombra dura e compressão tátil (`animateDpAsState`) | `onClick`, `containerColor`, `contentColor = colors.onAccent` (ou `onSurface` quando `surface`), `borderColor`, `shadowColor`, `shape = 12.dp`, `borderWidth = 2.5.dp`, `shadowOffset = 4.dp`, `contentPadding`, `enabled`, `content` (+ overload com `text`) |
| `NeobrutalistIconButton` | Botão quadrado/compacto de ícone com borda, sombra curta (`2.dp`) e compressão tátil + preservação de `contentDescription` | `onClick`, `containerColor = colors.surface`, `contentColor`, `size = 40.dp`, `borderWidth = 2.dp`, `shadowOffset = 2.dp`, `shape = 10.dp`, `enabled`, `content` |
| `NeobrutalistBadge` | Etiqueta compacta com borda sólida (`2.dp`), sombra curta (`2.dp` ou `0.dp`), fundo opaco | `text`, `containerColor`, `contentColor`, `shadowOffset = 2.dp`, `shape = 8.dp`, `leadingIcon` opcional |
| `NeobrutalistCard` | Contêiner 100% opaco com `neobrutalistShadow` e `background(containerColor, shape)` | `containerColor = colors.surface`, `borderWidth = 2.5.dp`, `shadowOffset = 4.dp`, `shape = 16.dp`, `onClick` opcional, `content` |
| `NeobrutalistDialog` | Modal customizado baseado em `Dialog(usePlatformDefaultWidth = false)` 100% opaco, borda `2.5.dp`, sombra `4.dp`, cantos `16.dp`, sem blur | `onDismissRequest`, `icon`, `title`, `content`, `actions` |
| `NeobrutalistProgressBar` | Barra de progresso retangular/levemente arredondada (`4.dp`–`6.dp`) com borda sólida `1.5.dp`–`2.dp`, track opaco `progressTrack` e barra preenchida sólida (`accentGreen` / `accentYellow`) | `progress: Float`, `fillColor`, `trackColor`, `borderColor`, `modifier` |
| `NeobrutalistSnackbarHost` | `SnackbarHost` customizado renderizando alertas em contêiner opaco com borda `2.5.dp`, sombra `4.dp` e contraste AA | `hostState: SnackbarHostState`, `isError: Boolean = true` |

---

## 3. Contrato de Integração por Tela e Componente

| Arquivo | Grid `24dp`? | Superfície Opaca | Acentos & Semântica | Preservação Obrigatória |
|---|---|---|---|---|
| `HomeScreen.kt` | **Sim** (no `Box` raiz fixo atrás de todo conteúdo) | `TopAppBar` (`surface` + borda inferior `2.5.dp`), `FAB` (`accentYellow`), `ExtendedFAB` (`accentYellow`), `LinearProgressIndicator` (`accentPurple` / `progressTrack` com borda), `Snackbar` | Marca `MenuBook` enquadrada em `accentYellow`; ações `TopAppBar` em `NeobrutalistIconButton`; contadores `SectionHeader` em `NeobrutalistBadge` (`accentBlue`) | Textos `"PDF Reader"`, `"Jump Back In"`, `"Favorite Folders"`, `"Open PDF"`; `contentDescription` `"Backup e Restauração"`, `"Pin folder"`, `"Open PDF"`; `padding` anti-clipping em `LazyRow`, `LazyColumn`, `LazyVerticalGrid` e `FAB` |
| `HomeEmptyState.kt` | Herda da `HomeScreen` | Cartão central `NeobrutalistCard` (`surface`, `4.dp` shadow) | Ícone `MenuBook` em caixa `accentYellow` com borda/sombra; `"Open PDF"` primário (`accentYellow`); `"Pin Folder"` secundário (`surface`) | Texto `"Your Library is Empty"`, descrição, `"Open PDF"`, `"Pin Folder"` |
| `RecentDocumentCard.kt` | Não (está sobre a Home) | `surface` (remover `alpha = 0.7f`) | Caixa ícone PDF (`accentRed` ou `accentOrange` com `onAccent`); botão `X` em controle delimitado `2.dp` borda; `NeobrutalistProgressBar` (`accentGreen`) | `document.fileName` (1 linha, `Ellipsis`), `contentDescription = "Remove from recents"`, `"Page X of Y • Z%"` |
| `FavoriteFolderCard.kt` | Não (está sobre a Home) | `surface` (remover `ElevatedCard`), `DropdownMenu` `surface` + borda `2.5.dp` + `0.dp` elevação | Aba superior (`90.dp x 16.dp`) e caixa de ícone (`46.dp`) com `NeobrutalismFolderAccent.accentForKey(...)`; badge contagem `"1 PDF"`/`"N PDFs"` (`accentYellow` ou `background` com borda); item `"Unpin folder"` em `errorText` | `folder.displayName`, `folder.pathSummary`, `contentDescription = "Folder options"`, `"Unpin folder"` |
| `AdaptiveReaderScaffold.kt` | **Não** (`RN-03`) | `TopAppBar` (`surface` + borda inferior `2.5.dp`); painel lateral expandido (`276.dp`, `surface` + borda direita `2.5.dp`); surround viewport em `readerViewportBackground` | Badge `"Page X of Y"` (`accentBlue`); Play/Pause no painel expandido (`accentYellow` pausado vs `accentOrange` ativo); loading `CircularProgressIndicator(accentYellow)` + card vazio Neo-Brutalism | `contentDescription`: `"Back to library"`, `"Open PDF"`, `"Toggle Auto-Scroll"`; textos `"PDF Reader"`, `"No PDF document opened"`, `"Tap the folder icon above to open a PDF"`; título com `maxLines = 1, overflow = TextOverflow.Ellipsis` |
| `FloatingAutoScrollOverlay.kt` | Não | `surface` 100% opaco (remover `alpha = 0.95f` e `tonalElevation`/`shadowElevation` M3) | Play/Pause (`56.dp` tátil): `accentYellow` (pausado) vs `accentOrange` (ativo/rolando) com `onAccent`; borda `2.5.dp`, sombra `4.dp`, cantos `16.dp` | `contentDescription`: `"Pause Auto-Scroll"` / `"Start Auto-Scroll"`, toggle por toque na página |
| `SpeedSelectorControl.kt` | Não | Herda painel opaco | Chips `Slow`/`Medium`/`Fast`: selecionado `accentPurple` + `onAccent` + sombra `2.dp`, inativo `surface` + borda `2.dp`; botões `-`/`+` `NeobrutalistIconButton`; badge `X px/s` destacado; `"Padrão salvo: X px/s"` em badge `accentYellow` + `onAccent`; `"Restaurar"` destrutivo (`errorText` ou `accentRed` bordado); `"Salvar neste PDF"` em `NeobrutalistButton` (`accentBlue` ou `surface`) | `contentDescription`: `"Decrease Speed"`, `"Increase Speed"`; slider `10f..500f`; textos `"px/s"`, `"Padrão salvo: ..."`, `"Padrão geral do app"`, `"Restaurar"`, `"Salvar neste PDF"` |
| `BackupDialogs.kt` | Não | `NeobrutalistDialog` (`surface` 100% opaco) | `BackupActionsDialog`: ícone enquadrado `accentBlue`, `"Exportar backup (.json)"` (`accentYellow`), `"Importar backup (.json)"` (`surface`), `"Fechar"` (`surface`). `RestorePreviewDialog`: bloco resumo com borda `2.dp` em `background`, opções `Mesclar (padrão)` e `Substituir tudo` em cartões clicáveis com borda destacada quando selecionada (`accentYellow` / `accentRed` tint sutil + borda espessa), `"Restaurar"` (`accentYellow`) e `"Cancelar"` (`surface`) | Todos os títulos, descrições e rótulos de resumo existentes em português |
| `FolderContentsDialog.kt` | Não | `NeobrutalistDialog` (`surface` 100% opaco) | Ícone pasta enquadrado, itens `FolderPdfListItem` com borda/divisor nítido e ícone `MenuBook`, `"Unpin"` em botão destrutivo (`accentRed` + `onAccent`), `"Close"` em `surface` | Textos `"No PDF files found in this folder."`, `"Close"`, `"Unpin"`, formatação de tamanho |
| `ErrorDialog.kt` | Não | `NeobrutalistDialog` (`surface` 100% opaco) | Cabeçalho/ícone de erro em `accentRed` + `onAccent`, `"Choose Another File"` primário (`accentYellow`), `"Dismiss"` secundário (`surface`) | Títulos/mensagens dos 4 erros (`FileNotFound`, `PasswordRequired`, `CorruptedDocument`, `OutOfMemory`), `"Choose Another File"`, `"Dismiss"` |
| `PdfDocumentViewport.kt` / `PdfPageItem.kt` | **Não** | Folha PDF intacta (`RN-03`) | Apenas `CircularProgressIndicator` de carregamento de página herda `MaterialTheme.colorScheme.primary` (agora mapeado para `accentYellow` / `onSurface`) | Zero alteração em renderização de bitmap, zoom (`0.5f..4.0f`), tap toggle e loop `withFrameNanos` |
