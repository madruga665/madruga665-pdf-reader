# Quickstart Validation Guide: Estilo Neo-Brutalism (`madruga665-pdf-reader`)

**Feature**: `005-neo-brutalism-theme`  
**Date**: 2026-11-25

---

## 1. Portões Automatizados (Obrigatórios)

```bash
cd /home/madruga665/Projetos/madruga665-pdf-reader

# 1. Suíte completa de testes unitários JVM (inclui NeobrutalismThemeContractTest + todos os testes existentes)
./gradlew testDebugUnitTest

# 2. Análise estática Android Lint
./gradlew lintDebug

# 3. Compilação do APK de depuração
./gradlew assembleDebug
```

Quando houver aparelho ou emulador conectado via ADB:

```bash
# 4. Instalar build atualizado no dispositivo/tablet
./gradlew installDebug

# 5. Executar testes instrumentados de UI Compose (quando emulador/dispositivo disponível)
./gradlew connectedDebugAndroidTest
```

---

## 2. Checklist de Validação Visual e Interativa (Claro & Escuro Catppuccin Mocha)

Execute os cenários abaixo em **Smartphone (~360dp retrato e paisagem)** e **Tablet / Dobrável (expandido)**, validando nos modos **Claro** e **Escuro (Catppuccin Mocha)**:

1. **Tema Claro & Ausência de Dynamic Color (`CA-01`)**:
   - Configure um papel de parede colorido no Android 12+ em Modo Claro.
   - Abra o app: confirme `#F8F8F8` de fundo, `#FFFFFF` nos cartões, `#000000` em bordas/sombras/textos e acentos `#FFD600`, `#7C5CFF`, `#FF6B00`, `#3399FF` sem qualquer tom roxo/dinâmico herdado do sistema.
2. **Troca Dinâmica para Catppuccin Mocha (`CA-02`, `RNF-03`)**:
   - Com o app aberto (na Home, depois no Leitor com auto-scroll configurado, e com um diálogo aberto), alterne o dispositivo para Modo Escuro.
   - Confirme transição imediata para `#1E1E2E`, `#313244`, `#CDD6F4`, bordas/sombras `#11111B` e acentos Mocha sem perda de estado.
3. **Bordas Sólidas, Sombras Duras e Compressão Tátil (`CA-03`, `CA-04`)**:
   - Verifique bordas `2dp`–`3dp` e sombras deslocadas `2dp`–`4dp` sem blur em todos os cartões, botões, chips, FAB, overlay e diálogos.
   - Pressione e segure botões principais (`Open PDF`, `Pin Folder`, FAB, Play/Pause, botões de diálogo) e confirme o movimento na direção da sombra e retorno ao soltar.
4. **Grid Fixo & Opacidade Total (`CA-05`)**:
   - Na Home vazia e na Home populada (rolando para cima e para baixo), confirme o grid `24dp x 24dp` (`1dp`) fixo no fundo e 100% oculto atrás de barras, cards, FAB, menus e diálogos.
5. **Home Vazia & Populada (`CA-06`, `CA-07`, `CA-08`, `CA-09`)**:
   - Verifique TopAppBar (Backup, Fixar Pasta, Abrir PDF), estado vazio (`"Your Library is Empty"`), cabeçalhos com badges de contagem, FAB/ExtendedFAB, `RecentDocumentCard` (título em 1 linha, `X`, progresso e barra) e `FavoriteFolderCard` (aba superior colorida, caixa de ícone, badge de PDFs, menu `"Unpin folder"` opaco).
6. **Leitor, Preservação das Páginas PDF e Auto-Scroll (`CA-10`, `CA-11`, `CA-12`, `CA-13`)**:
   - Abra um PDF colorido/ilustrado nos dois temas: confirme preservação fiel das cores da folha, ausência de grid atrás do documento, badge `"Page X of Y"`, loading e estado vazio estilizados.
   - No **Compacto**, teste o painel flutuante opaco, contraste Play (amarelo/pausado) vs Pause (laranja/ativo), chips `Slow`/`Medium`/`Fast`, `-`/`+`, slider `10..500 px/s`, `"Salvar neste PDF"`, `"Restaurar"` e toggle por toque na folha.
   - No **Expandido (tablet)**, confirme painel lateral opaco com divisória vertical sólida e controles sem cortes ao lado da viewport fluida a `60fps`.
7. **Diálogos, Erro & Snackbar (`CA-14`, `CA-15`)**:
   - Abra `BackupActionsDialog`, `RestorePreviewDialog` (alternando `Mesclar (padrão)` e `Substituir tudo`), `FolderContentsDialog` (com arquivos e vazio) e `ErrorDialog` + `Snackbar` (arquivo indisponível/backup inválido) nos dois temas.
8. **Zero Clipping (`CA-16`)**:
   - Confira margens externas, primeiro/último card da `LazyRow` de recentes, último card da lista/grade de pastas, `FAB` e bordas dos diálogos — nenhuma sombra `4dp` pode estar cortada.
