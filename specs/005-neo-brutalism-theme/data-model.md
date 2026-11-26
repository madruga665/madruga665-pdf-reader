# Data Model & Design Tokens: Aplicar Estilo Neo-Brutalism

**Feature**: `005-neo-brutalism-theme`  
**Date**: 2026-11-25  
**Scope**: Modelos imutáveis de tema, geometria e estado de interação visual em `ui/theme/` e `ui/components/`. Nenhuma alteração em entidades de domínio (`core/model/*`), backup (`core/backup/*`) ou persistência DataStore.

---

## 1. `NeobrutalismColors` (Tokens Cromáticos)

Definido em `app/src/main/java/com/madruga665/pdfreader/ui/theme/Color.kt` com `@Immutable`:

```kotlin
@Immutable
data class NeobrutalismColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val readerViewportBackground: Color,
    val onSurface: Color,
    val subtext: Color,
    val border: Color,
    val shadow: Color,
    val onAccent: Color,
    val gridLine: Color,
    val progressTrack: Color,
    val accentYellow: Color,
    val accentPurple: Color,
    val accentOrange: Color,
    val accentBlue: Color,
    val accentGreen: Color,
    val accentRed: Color,
    val errorText: Color
)
```

### Tabela de Valores Exatos

| Token | Tema Claro (`LightNeobrutalismColors`) | Tema Escuro Mocha (`MochaDarkNeobrutalismColors`) | Papel / Uso Principal |
|---|---|---|---|
| `isDark` | `false` | `true` | Controle de contraste e barras do sistema |
| `background` | `#F8F8F8` | `#1E1E2E` (Base) | Fundo da Home (sob o grid) e scaffolds |
| `surface` | `#FFFFFF` | `#313244` (Surface0) | Cartões, TopAppBars, diálogos, overlay, painel lateral, menus (100% opacos) |
| `readerViewportBackground` | `#EAEAEA` | `#181825` (Mantle) | Fundo neutro ao redor das páginas PDF no leitor (sem grid) |
| `onSurface` | `#000000` | `#CDD6F4` (Text) | Títulos, rótulos principais, ícones sobre `surface`/`background` |
| `subtext` | `#555555` | `#A6ADC8` (Subtext0) | Subtítulos, caminhos de pasta, contadores secundários, descrições |
| `border` | `#000000` | `#11111B` (Crust) | Contornos sólidos (`2dp`–`2.5dp`) e divisórias estruturais |
| `shadow` | `#000000` | `#11111B` (Crust) | Sombras deslocadas duras sem blur (`2dp`–`4dp`) |
| `onAccent` | `#000000` | `#11111B` (Crust) | Texto/ícones dentro de qualquer superfície preenchida com acento |
| `gridLine` | `#E2E2E2` | `#28283D` | Linhas do grid fixo `24dp x 24dp` (`1dp`) na Home |
| `progressTrack` | `#E5E5E5` | `#1E1E2E` | Trilha inativa de barras de progresso e slider |
| `accentYellow` | `#FFD600` | `#F9E2AF` (Yellow) | Ação primária, FAB, Play pausado, badge de velocidade salva, pasta |
| `accentPurple` | `#7C5CFF` | `#CBA6F7` (Mauve) | Chip de preset selecionado, destaque secundário, pasta |
| `accentOrange` | `#FF6B00` | `#FAB387` (Peach) | Estado ativo/rolando do auto-scroll (Pause), pasta |
| `accentBlue` | `#3399FF` | `#89B4FA` (Blue) | Badges de seção/página, ícone PDF/informação, pasta |
| `accentGreen` | `#22C55E` | `#A6E3A1` (Green) | Sucesso, barra de progresso, confirmações positivas |
| `accentRed` | `#FF4B4B` | `#F38BA8` (Red) | Ações destrutivas preenchidas (`Unpin`, erro crítico) |
| `errorText` | `#C62828` | `#F38BA8` (Red) | Texto/ícone semântico de erro diretamente sobre `surface` (`>= 5,4:1`) |

---

## 2. `NeobrutalismMetrics` (Métricas Geométricas)

Definido em `app/src/main/java/com/madruga665/pdfreader/ui/theme/Modifiers.kt`:

| Constante | Valor | Regra / Uso |
|---|---|---|
| `BorderWidth` | `2.5.dp` | Cartões principais, `TopAppBar` divisória, painel flutuante, diálogos, FAB, botões primários |
| `BorderWidthCompact` | `2.dp` | Botões menores (`TopAppBar` icon buttons, `-`/`+`, fechar `X`), chips, badges, barras de progresso, caixa de ícone e aba de pasta |
| `ShadowOffset` | `4.dp` | Cartões (`RecentDocumentCard`, `FavoriteFolderCard`), empty state block, `FloatingAutoScrollOverlay`, `FAB`, `NeobrutalistDialog`, botões principais |
| `ShadowOffsetCompact` | `2.dp` | Botões da `TopAppBar`, botões `-`/`+`, `X`, chips ativos, badges de seção/página, aba superior da pasta, caixa de ícone da pasta |
| `PressedShadowOffset` | `1.dp` | Offset residual da sombra enquanto um controle tátil está pressionado |
| `CornerRadiusSmall` | `8.dp` | Badges, chips, botões compactos, caixa de ícone, aba de pasta |
| `CornerRadiusMedium` | `12.dp` | Botões padrão, ações de diálogo, dropdown menu, barras flutuantes |
| `CornerRadiusLarge` | `16.dp` | Cartões principais (`RecentDocumentCard`, `FavoriteFolderCard`), empty state, diálogos, painel flutuante |
| `GridSize` | `24.dp` | Passo fixo do grid quadriculado da Home |
| `GridStrokeWidth` | `1.dp` | Espessura da linha do grid |
| `ExpandedReaderRailWidth` | `276.dp` | Largura fixa do painel lateral opaco de controles no leitor em `WindowWidthSizeClass.Expanded` |

---

## 3. `NeobrutalismFolderAccent` (Atribuição Determinística de Cor por Pasta)

- **Motivação**: Evitar persistência extra em `FavoriteFolder` (fora do escopo v1 escolher cor manual por pasta), garantindo visual diversificado e consistente entre sessões.
- **Regra**:
  ```kotlin
  object NeobrutalismFolderAccent {
      fun accentForKey(key: String, colors: NeobrutalismColors): Color {
          val palette = listOf(
              colors.accentYellow,
              colors.accentPurple,
              colors.accentOrange,
              colors.accentBlue
          )
          val index = kotlin.math.abs(key.hashCode()) % palette.size
          return palette[index]
      }
  }
  ```
- **Chave recomendada**: `folder.treeUri.ifBlank { folder.displayName }`.

---

## 4. `NeobrutalistPressState` (Modelo de Compressão Tátil)

- Para todo controle com feedback tátil (`NeobrutalistButton`, `NeobrutalistIconButton`, FAB/ExtendedFAB, cards clicáveis quando configurados):
  - `isPressed && enabled` → `activeShadowOffset = 1.dp` e `contentTranslation = (restShadowOffset - 1.dp)` em `X` e `Y`.
  - `!isPressed && enabled` → `activeShadowOffset = restShadowOffset` e `contentTranslation = 0.dp`.
  - `!enabled` → `activeShadowOffset = 0.dp`, `contentTranslation = 0.dp`, opacidade de container/texto atenuada mantendo legibilidade.
- Transição animada via `animateDpAsState` (duração rápida padrão Compose tween/spring sem overshoot excessivo para manter precisão a `60fps`).

---

## 5. Preservação de Modelos Existentes (Zero Mudanças)

Os seguintes modelos e repositórios **NÃO** sofrem alteração:
- `core/model/`: `PdfDocument`, `RecentDocument`, `FavoriteFolder`, `FolderPdfItem`, `ReadingSession`, `ScrollConfiguration`, `SpeedPreset`, `ReaderErrorState`
- `core/backup/*`, `core/home/*`, `core/repository/*`, `core/storage/*`, `core/engine/*`
- `ui/home/*` (`HomeIntent`, `HomeViewState`, `HomeViewModel`, `HomeSideEffect`)
- `ui/reader/*` (`ReaderIntent`, `ReaderViewState`, `ReaderViewModel`, `ReaderSideEffect`)
- `ui/navigation/AppScreen.kt`
