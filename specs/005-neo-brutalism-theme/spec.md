# Feature Specification: Aplicar Estilo Neo-Brutalism (Tema Claro e Escuro Catppuccin Mocha)

**Feature Branch**: `005-neo-brutalism-theme`  
**Created**: 2026-11-25  
**Status**: Draft  
**Input**: PRD aprovado em `/home/madruga665/OpenBot/Shared/prds-pessoal/madruga665-pdf-reader/estilo-neo-brutalism.md` — Aplicar integralmente a identidade visual Neo-Brutalism do ecossistema `madruga665` (Tema Claro e Tema Escuro Catppuccin Mocha) em toda a interface do aplicativo Android `madruga665-pdf-reader`, mantendo preservação total das páginas PDF e zero regressão funcional.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sistema de Temas Neo-Brutalism, Geometria, Tipografia e Feedback Tátil (Priority: P1) 🎯 MVP

Como usuário do ecossistema `madruga665`, eu quero que o **PDF Reader** utilize exclusivamente a paleta Neo-Brutalism nos modos **Claro** e **Escuro (Catppuccin Mocha)** seguindo o tema do sistema operacional, com contornos sólidos espessos, sombras deslocadas duras sem desfoque, tipografia contrastada e compressão tátil ao pressionar controles, para ter identidade visual consistente e leitura rápida da hierarquia de ações em qualquer dispositivo.

**Why this priority**: A fundação de cores, tipografia, bordas, sombras duras e interação tátil sustenta todas as telas, cartões e diálogos do aplicativo. Sem ela, o tema roxo genérico e as cores dinâmicas do papel de parede continuam vazando na interface.

**Independent Test**: Abrir o aplicativo em um dispositivo Android 12+ com papel de parede colorido (ex.: verde ou azul) no Modo Claro e alternar o tema do sistema para Modo Escuro com o app aberto. Verificar que nenhum tom roxo padrão ou cor dinâmica aparece, que todos os contêineres e botões visíveis possuem borda sólida (`2dp`–`3dp`) e sombra deslocada dura (`2dp`–`4dp`, sem blur) e que os botões principais se comprimem em direção à sombra ao serem pressionados.

**Acceptance Scenarios**:

1. **Given** um dispositivo Android configurado em **Modo Claro** com qualquer papel de parede ativo (`CA-01`),  
   **When** o usuário abre o aplicativo,  
   **Then** toda a interface utiliza fundo base `#F8F8F8`, superfícies opacas `#FFFFFF`, texto principal `#000000`, subtexto `#555555`, bordas/sombras `#000000` e acentos Neo-Brutalism (`#FFD600`, `#7C5CFF`, `#FF6B00`, `#3399FF`, verde e vermelho semânticos), sem aplicação de cores dinâmicas ou tons roxos herdados.

2. **Given** o aplicativo aberto no Modo Claro em qualquer tela ou diálogo (`CA-02`, `RNF-03`),  
   **When** o tema do sistema operacional é alternado para **Modo Escuro**,  
   **Then** todas as telas, controles, barras, linhas de grid e diálogos abertos atualizam imediatamente para a paleta **Catppuccin Mocha** (fundo `#1E1E2E`, superfícies `#313244`, texto `#CDD6F4`, subtexto `#A6ADC8`, bordas/sombras `#11111B`, grid `#28283D` e acentos Mocha `#F9E2AF`, `#CBA6F7`, `#FAB387`, `#89B4FA`, `#A6E3A1`, `#F38BA8`) sem perda do documento, posição de rolagem, velocidade ou diálogo ativo.

3. **Given** qualquer cartão, botão, chip, botão flutuante, painel ou diálogo visível nos temas Claro ou Escuro (`CA-03`),  
   **When** o elemento é inspecionado visualmente,  
   **Then** ele exibe contorno sólido espesso (`2dp`–`3dp`) e sombra sólida deslocada (`2dp`–`4dp`) com zero desfoque e zero elevação tonal difusa.

4. **Given** qualquer controle interativo principal (botões de ação, ações da barra superior, chips, FAB, cartões clicáveis) (`CA-04`),  
   **When** o usuário pressiona e solta o controle,  
   **Then** o controle desloca-se fisicamente em direção à sombra durante o pressionamento (reduzindo o offset da sombra) e retorna à posição original ao soltar, disparando a ação esperada.

---

### User Story 2 - Tela Inicial com Fundo Quadriculado Fixo, TopAppBar, Estado Vazio e Cartões (Priority: P1) 🎯 MVP

Como leitor acessando a biblioteca do aplicativo, eu quero visualizar a tela inicial com fundo quadriculado fixo discreto, barra superior bem delimitada, cabeçalhos com contadores em destaque, estado vazio convidativo, botão flutuante marcante e cartões opacos para documentos recentes e pastas favoritas, para navegar rapidamente entre meus PDFs e coleções.

**Why this priority**: A Tela Inicial é o ponto de entrada principal do aplicativo em instalações limpas e recorrentes, concentrando o fundo quadriculado característico do ecossistema, os cartões de recentes e o visual de fichário das pastas favoritas.

**Independent Test**: Testar a `HomeScreen` em dois estados (biblioteca vazia e biblioteca populada com >= 1 recente e >= 1 pasta favorita) em smartphone e tablet, nos temas Claro e Escuro, verificando o grid fixo `24dp x 24dp`, a opacidade total dos elementos sobrepostos, o funcionamento das ações da TopAppBar, estado vazio, abertura/remoção de recente e menu contextual `"Unpin folder"`.

**Acceptance Scenarios**:

1. **Given** a Tela Inicial em estado vazio ou populado nos temas Claro e Escuro (`CA-05`),  
   **When** o usuário observa a tela e realiza rolagem vertical na lista populada,  
   **Then** o fundo exibe um padrão quadriculado uniforme (`24dp x 24dp`, traço `1dp`, cor `#E2E2E2` no Claro e `#28283D` no Escuro) ancorado estaticamente à tela (sem rolar com o conteúdo) e completamente oculto sob cartões, barras, FAB e diálogos 100% opacos.

2. **Given** a Tela Inicial sem documentos recentes nem pastas favoritas (`CA-06`),  
   **When** a tela é carregada,  
   **Then** a `TopAppBar` exibe marca/ícone em destaque, título `"PDF Reader"` em peso forte, borda inferior sólida e as três ações (`Backup e Restauração`, `Pin folder`, `Open PDF`) em botões Neo-Brutalism; e o bloco `"Your Library is Empty"` exibe contêiner opaco Neo-Brutalism com ícone enquadrado e botões táteis `"Open PDF"` (primário) e `"Pin Folder"` (secundário) totalmente funcionais.

3. **Given** a Tela Inicial populada com documentos recentes e pastas favoritas (`CA-07`),  
   **When** exibida em viewport compacta (smartphone) e expandida (tablet),  
   **Then** os cabeçalhos `"Jump Back In"` e `"Favorite Folders"` exibem contadores em badges Neo-Brutalism com borda sólida, e o botão flutuante (`FAB` compacto e `ExtendedFAB` `"Open PDF"` expandido) adota acento primário, borda sólida espessa, sombra dura e resposta tátil ao toque.

4. **Given** ao menos um documento recente em `"Jump Back In"` (`CA-08`),  
   **When** o cartão é exibido e interagido,  
   **Then** exibe superfície opaca, borda sólida espessa, sombra dura sem recorte, ícone de PDF enquadrado, nome truncado em 1 linha, botão `X` (`Remove from recents`) bem delimitado, texto `"Page X of Y • Z%"` e barra de progresso Neo-Brutalism com borda sólida; tocar no cartão abre o PDF e tocar em `X` remove o item.

5. **Given** ao menos uma pasta favorita em `"Favorite Folders"` (`CA-09`),  
   **When** o cartão é exibido e o usuário abre o menu de opções (`MoreVert`),  
   **Then** o cartão apresenta identidade de fichário (aba superior em cor de acento vibrante determinística, caixa de ícone com borda e sombra, título em destaque, resumo de caminho legível e badge de contagem `"1 PDF"` / `"N PDFs"`), e o menu suspenso abre em superfície opaca com borda sólida contendo `"Unpin folder"` funcional.

---

### User Story 3 - Tela do Leitor, Preservação das Páginas PDF e Controles de Auto-Scroll (Priority: P1) 🎯 MVP

Como usuário lendo um documento ou partitura no leitor, eu quero que a barra superior, os estados auxiliares e os controles de auto-scroll (flutuante compacto e painel lateral expandido) adotem a identidade Neo-Brutalism com legibilidade imediata de estado, enquanto as páginas renderizadas do PDF permanecem 100% preservadas sem grid ou interferências visuais.

**Why this priority**: O leitor com rolagem automática contínua é a funcionalidade central do aplicativo; clareza entre estado pausado/ativo e velocidade personalizada salva evita erros durante uso rápido e leitura musical/técnica.

**Independent Test**: Abrir um PDF no leitor em smartphone (compacto) e tablet (expandido), nos modos Claro e Escuro, inspecionar a preservação das cores/conteúdo da página PDF, alternar visibilidade do overlay ao tocar na folha, acionar Play/Pause, alternar presets (`Slow`, `Medium`, `Fast`), ajustar com `-`/`+`/slider, salvar velocidade no PDF (`"Salvar neste PDF"`) e restaurar (`"Restaurar"`).

**Acceptance Scenarios**:

1. **Given** um PDF aberto no leitor em Tema Claro ou Tema Escuro (`CA-10`, `RN-03`),  
   **When** o documento é exibido,  
   **Then** a barra superior exibe superfície opaca Neo-Brutalism com borda inferior sólida, botões voltar (`"Back to library"`) e abrir PDF (`"Open PDF"`) estilizados, título truncado em 1 linha sem corte abrupto e badge de alto contraste `"Page X of Y"`, enquanto a área de leitura utiliza fundo neutro sem padrão quadriculado e as páginas PDF mantêm renderização original sem filtros, inversões ou bordas brutais invasivas.

2. **Given** o leitor em estado de carregamento ou sem documento aberto (`"No PDF document opened"`) (`CA-11`),  
   **When** exibido nos dois temas,  
   **Then** o indicador de carregamento e o bloco vazio seguem integralmente a paleta e tipografia Neo-Brutalism sem resquícios roxos.

3. **Given** um PDF aberto em layout compacto (smartphone) (`CA-12`),  
   **When** o painel flutuante inferior de auto-scroll é exibido e manipulado,  
   **Then** ele é 100% opaco com borda sólida espessa e sombra deslocada dura; o botão Play/Pause distingue inequivocamente o estado pausado (acento primário) do estado ativo/rolando (acento contrastante); os chips `Slow`, `Medium`, `Fast`, os botões `-`/`+`, o slider `10..500 px/s`, a leitura `X px/s`, o indicador de padrão salvo (`"Padrão salvo: X px/s"` / `"Padrão geral do app"`), `"Salvar neste PDF"` e `"Restaurar"` funcionam integralmente; e tocar na página alterna a visibilidade do painel.

4. **Given** um PDF aberto em layout expandido (tablet ou dobrável) (`CA-13`),  
   **When** o painel lateral esquerdo é renderizado,  
   **Then** apresenta superfície opaca separada da viewport por divisória vertical sólida, acomodando Play/Pause e todo o seletor de velocidade sem cortes horizontais ou sobreposição, mantendo a rolagem automática fluida na viewport ao lado.

---

### User Story 4 - Diálogos, Menus Contextuais, Indicador de Backup e Snackbar Neo-Brutalism (Priority: P2)

Como usuário gerenciando backups locais, pastas favoritas ou erros de arquivo, eu quero que todos os diálogos modais, menus contextuais, barras de progresso e notificações `Snackbar` apresentem o mesmo acabamento Neo-Brutalism de alto contraste sem perder nenhuma opção ou regra de segurança.

**Why this priority**: Diálogos modais herdam facilmente superfícies arredondadas e elevações difusas do Material 3 padrão; unificá-los elimina quebras visuais nos fluxos críticos de backup e tratamento de falhas.

**Independent Test**: Abrir cada diálogo (`BackupActionsDialog`, `RestorePreviewDialog`, `FolderContentsDialog` com PDFs e vazio, `ErrorDialog`) e disparar `Snackbar` e `LinearProgressIndicator` em Claro e Escuro, verificando aparência Neo-Brutalism e execução real dos fluxos (`Exportar`, `Importar`, `Mesclar` vs `Substituir tudo`, abrir PDF da pasta, `Unpin`, `Choose Another File`, `Dismiss`).

**Acceptance Scenarios**:

1. **Given** acionamento dos diálogos de `Backup e Restauração`, `Confirmar restauração` e `Conteúdo da pasta favorita` nos temas Claro e Escuro (`CA-14`),  
   **When** exibidos na tela,  
   **Then** todos renderizam em contêineres opacos com borda sólida espessa, sombra deslocada dura sem blur, divisores nítidos e hierarquia clara de botões (primário em acento vibrante, secundário em superfície contrastante, destrutivo em acento vermelho), mantendo seleção entre `Mesclar (padrão)` e `Substituir tudo`, rolagem da lista de PDFs, estados de carregamento/vazio e todas as ações funcionais.

2. **Given** ocorrência de erro ao abrir PDF (`FileNotFound`, `PasswordRequired`, `CorruptedDocument`, `OutOfMemory`) ou falha/sucesso no fluxo de backup exibindo `Snackbar` (`CA-15`),  
   **When** `ErrorDialog` ou `Snackbar` aparece na tela,  
   **Then** apresenta destaque semântico Neo-Brutalism com contraste WCAG 2.2 AA legível, borda sólida e ações funcionais (`Choose Another File`, `Dismiss`).

---

### User Story 5 - Responsividade Multi-Tela, Proteção Contra Recorte, Acessibilidade e Regressão Zero (Priority: P2)

Como usuário que alterna entre smartphone (retrato/paisagem) e tablet, eu quero garantir que bordas espessas e sombras deslocadas nunca sejam cortadas, que todos os textos e controles cumpram WCAG 2.2 AA, que a fluidez de 60fps seja mantida e que todas as funcionalidades e testes do aplicativo permaneçam íntegros.

**Why this priority**: Sombras deslocadas duras (`4dp`) e bordas (`2.5dp`) frequentemente sofrem recorte (`clipping`) em carrosséis, listas e modais quando faltam folgas de padding, além de exigirem verificação rigorosa de contraste nos acentos Claro e Catppuccin Mocha.

**Independent Test**: Percorrer todas as telas e diálogos em viewport compacta (~360dp retrato e paisagem) e tablet expandido nos dois temas, executar a verificação automatizada de contrato/contraste WCAG AA, rodar `./gradlew testDebugUnitTest`, `./gradlew lintDebug` e `./gradlew assembleDebug` com 100% de aprovação.

**Acceptance Scenarios**:

1. **Given** execução em smartphone (~360dp retrato e paisagem) e tablet nos temas Claro e Escuro (`CA-16`),  
   **When** o usuário percorre a biblioteca, carrossel de recentes, grade de pastas, leitor, controles de velocidade e diálogos,  
   **Then** nenhuma borda ou sombra deslocada é recortada pelas extremidades da tela ou limites de container, nenhum texto se sobrepõe, todas as combinações de cor atingem contraste WCAG 2.2 AA, o scroll/auto-scroll mantém fluidez 60fps e 100% da suíte automatizada e build passam sem erros.

---

### Edge Cases

- **Troca de tema durante operação ativa**: Alternar Modo Claro ↔ Modo Escuro pelo sistema enquanto um PDF está em auto-scroll, o overlay está oculto/exibido, um diálogo de restauração está aberto ou a lista de recentes está rolada deve atualizar cores, bordas, sombras e grid instantaneamente sem fechar diálogos nem resetar estado (`RNF-03`).
- **Prevenção de clipping de sombra deslocada**: Cartões em `LazyRow`, `LazyColumn`, `LazyVerticalGrid`, `FAB`/`ExtendedFAB`, `FloatingAutoScrollOverlay` e `NeobrutalistDialog` não podem ter `.clip()` aplicado antes da sombra nem estar encostados em bordas de container sem margem mínima equivalente ao `shadowOffset` (`4dp`).
- **Títulos e caminhos longos em largura estreita (~360dp)**: Nomes longos de PDFs, caminhos de pastas, `"Padrão salvo: XXX px/s"` junto a `"Restaurar"` + `"Salvar neste PDF"` no `SpeedSelectorControl`, e título na `TopAppBar` do leitor devem truncar com reticências ou quebrar em linhas sem esmagar botões nem sobrepor badges (`RNF-05`).
- **Contraste em acentos claros e saturados**: Roxo `#7C5CFF`, laranja `#FF6B00`, vermelho `#FF4B4B` e amarelo `#FFD600` no Tema Claro não atingem 4,5:1 com texto branco `#FFFFFF` (ou amarelo direto sobre fundo branco); texto/ícones sobre acentos preenchidos devem usar `#000000` no Claro e `#11111B` no Escuro, e texto semântico de erro sobre fundo claro `#FFFFFF` deve usar tom de erro de alto contraste (`>= 4,5:1`).
- **Papel de parede Android dinâmico (Material You)**: Em Android 12+ (`API 31+`) com cores dinâmicas ativas no sistema, nenhuma tela ou componente Material 3 (`Slider`, `RadioButton`, `CircularProgressIndicator`, `LinearProgressIndicator`) pode exibir cores derivadas do wallpaper (`RN-01`).
- **Pastas sem `pathSummary` ou com `0`/`1`/`N` PDFs**: `FavoriteFolderCard` deve manter alinhamento limpo mesmo quando `pathSummary` estiver vazio, exibindo `"0 PDFs"`, `"1 PDF"` ou `"N PDFs"` no badge sem deformar o cartão.
- **Páginas PDF em Tema Escuro**: Mesmo quando a interface circundante está em Catppuccin Mocha (`#1E1E2E` / `#181825`), as páginas renderizadas do PDF devem permanecer intactas (sem inversão de cores, sem filtro escuro e sem grid aparecendo atrás ou através da página).

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001** *(ref. RF-01, RN-01)*: O sistema de tema DEVE desabilitar integralmente o `dynamicColor` do sistema operacional e aplicar de forma determinística duas paletas completas sincronizadas com o modo Claro/Escuro do dispositivo: **Tema Claro** (`#F8F8F8` fundo, `#FFFFFF` superfícies opacas, `#000000` texto/bordas/sombras, `#555555` subtexto, `#E2E2E2` grid, acentos `#FFD600`, `#7C5CFF`, `#FF6B00`, `#3399FF`, verde e vermelho semânticos) e **Tema Escuro Catppuccin Mocha** (`#1E1E2E` fundo, `#313244` superfícies opacas, `#CDD6F4` texto, `#A6ADC8` subtexto, `#11111B` bordas/sombras, `#28283D` grid, acentos `#F9E2AF`, `#CBA6F7`, `#FAB387`, `#89B4FA`, `#A6E3A1`, `#F38BA8`).
- **FR-002** *(ref. RF-01, RNF-03)*: Todos os tokens de `MaterialTheme.colorScheme` DEVEM ser mapeados para as paletas Neo-Brutalism (eliminando resíduos de `Purple40`/`Purple80`/`Pink40`/`Pink80`) e as barras de sistema (status bar / navigation bar) DEVEM acompanhar o tema ativo, reagindo imediatamente à troca Claro/Escuro do sistema sem perda de estado.
- **FR-003** *(ref. RF-02, RN-02)*: Todos os contêineres estruturais e controles interativos (cartões, botões, ações de barra, chips, badges, botões flutuantes, painel de auto-scroll, menu contextual e diálogos) DEVEM usar contornos sólidos espessos (`2dp`–`3dp`, referência `2.5dp` para superfícies principais e `2dp` para badges/chips/controles menores), sombras sólidas deslocadas sem blur (`2dp`–`4dp`) e raios de canto entre `8dp` e `16dp`, substituindo qualquer elevação tonal ou sombra difusa.
- **FR-004** *(ref. RF-02)*: A tipografia DEVE usar família `SansSerif` com pesos fortes (`Bold`/`ExtraBold`) para títulos, cabeçalhos, contadores e rótulos de ação, mantendo alto contraste legível em textos de apoio e metadados.
- **FR-005** *(ref. RF-02)*: Controles clicáveis principais (botões primários/secundários/destrutivos, ações da `TopAppBar`, chips de preset, botões `-`/`+`, botões de diálogo, `FAB`/`ExtendedFAB` e botões de ação no estado vazio) DEVEM aplicar compressão tátil ao serem pressionados, deslocando-se na direção da sombra e reduzindo visivelmente o offset da sombra até o retorno ao soltar.
- **FR-006** *(ref. RF-03, RN-04)*: A `HomeScreen` DEVE renderizar um fundo quadriculado discreto e uniforme (`24dp x 24dp`, traço `1dp`) na cor do tema ativo, fixo no contêiner raiz durante a rolagem, e todos os elementos sobrepostos na Home (barras, cartões, badges, botões, FAB, menus e diálogos) DEVEM usar superfícies 100% opacas (`alpha = 1f`) para impedir vazamento das linhas de grid.
- **FR-007** *(ref. RF-04)*: A `TopAppBar` da Home DEVE exibir marca/ícone em destaque Neo-Brutalism, título `"PDF Reader"` em peso forte, borda inferior sólida separando-a do conteúdo e as três ações existentes (`Backup e Restauração`, `Pin folder`, `Open PDF`) em botões com borda sólida e sombra curta; cabeçalhos `"Jump Back In"` e `"Favorite Folders"` DEVEM exibir contadores em badges Neo-Brutalism; o estado vazio `"Your Library is Empty"` DEVE ser apresentado em cartão opaco Neo-Brutalism com ícone enquadrado e botões táteis `"Open PDF"` (primário) e `"Pin Folder"` (secundário); e o botão flutuante (`FAB` compacto / `ExtendedFAB` `"Open PDF"` expandido) DEVE usar acento primário, borda espessa, sombra dura e compressão tátil.
- **FR-008** *(ref. RF-05)*: O `RecentDocumentCard` DEVE ter superfície 100% opaca, borda sólida espessa, sombra deslocada dura, ícone de PDF enquadrado com borda, nome do arquivo em negrito truncado em 1 linha (`Ellipsis`), botão de remoção `X` bem delimitado, texto `"Page X of Y • Z%"` legível e barra de progresso Neo-Brutalism com borda sólida.
- **FR-009** *(ref. RF-05)*: O `FavoriteFolderCard` DEVE adotar linguagem visual de fichário inspirada no ecossistema (aba superior em cor de acento determinística, caixa de ícone com borda e sombra, título em negrito, resumo de caminho quando presente, badge Neo-Brutalism com contagem `"1 PDF"` / `"N PDFs"` e menu suspenso opaco com borda sólida contendo `"Unpin folder"` com destaque semântico destrutivo).
- **FR-010** *(ref. RF-06, RN-03)*: A `TopAppBar` do leitor (`AdaptiveReaderScaffold`) DEVE usar superfície opaca Neo-Brutalism, borda inferior sólida, botões voltar e abrir PDF estilizados, título truncado em 1 linha com reticências e badge de alto contraste `"Page X of Y"`. A viewport de exibição do PDF (`PdfDocumentViewport` / `PdfPageItem`) DEVE manter fundo neutro confortável sem padrão quadriculado e preservar 100% das páginas PDF sem alteração de cor, filtro ou bordas invasivas; os estados de carregamento e leitor vazio (`"No PDF document opened"`) DEVEM seguir o tema Neo-Brutalism.
- **FR-011** *(ref. RF-07, RN-04, RN-05)*: O `FloatingAutoScrollOverlay` (layout compacto) DEVE ser um contêiner 100% opaco com borda sólida espessa e sombra deslocada dura (sem transparência nem elevação difusa), preservando o toggle por toque na página. Em telas expandidas, o painel lateral do leitor DEVE ter superfície opaca com divisória vertical sólida acomodando Play/Pause e `SpeedSelectorControl` sem cortes ou sobreposição. Em ambos os layouts, o botão Play/Pause DEVE diferenciar claramente os estados pausado e ativo com alto contraste.
- **FR-012** *(ref. RF-07, RN-05)*: O `SpeedSelectorControl` DEVE estilizar em Neo-Brutalism os chips de preset (`Slow`, `Medium`, `Fast`) com estado selecionado preenchido em cor de acento, botões `-` e `+` quadrados com borda/sombra, slider contínuo (`10f..500f`) e leitura `"X px/s"` em badge/texto de alto contraste, divisor sólido e seção de velocidade salva com destaque claro (`"Padrão salvo: X px/s"` vs `"Padrão geral do app"`), botão `"Restaurar"` com semântica destrutiva/alerta e botão `"Salvar neste PDF"` com borda e sombra dura, mantendo integridade de layout em ~360dp e no painel lateral expandido.
- **FR-013** *(ref. RF-08, RN-04, RN-05)*: `BackupActionsDialog`, `RestorePreviewDialog`, `FolderContentsDialog` e `ErrorDialog`, bem como `LinearProgressIndicator` (progresso de backup) e `SnackbarHost`, DEVEM utilizar contêineres opacos Neo-Brutalism com borda sólida espessa, sombra deslocada dura, divisores nítidos e hierarquia consistente de ações (primário em acento vibrante, secundário em superfície contrastante, destrutivo em vermelho semântico), com destaque evidente entre `"Mesclar (padrão)"` e `"Substituir tudo"` e destaque semântico de erro no `ErrorDialog`/`Snackbar`.
- **FR-014** *(ref. RF-09, RNF-05)*: Todos os layouts (compacto retrato/paisagem, médio e expandido) e listas/carrosséis/modais DEVEM possuir margens e `contentPadding` suficientes para que nenhuma borda espessa ou sombra deslocada seja recortada e nenhum texto se sobreponha.
- **FR-015** *(ref. RN-05, RNF-02, RNF-04)*: Todos os fluxos funcionais existentes, textos/rótulos atuais de UI, descrições de acessibilidade (`contentDescription`), atalhos de teclado físico (`Space`, `+`/`=`, `-`), persistência DataStore e contratos de ViewModel DEVEM ser preservados integralmente sem regressão, cumprindo contraste WCAG 2.2 AA (`>= 4,5:1` texto normal; `>= 3:1` texto grande e componentes gráficos).

### Key Entities *(include if feature involves data)*

- **ThemePalette (`NeobrutalismColors`)**: Conjunto imutável de tokens cromáticos para os modos Claro e Escuro Catppuccin Mocha, separando explicitamente superfícies opacas, fundo da viewport de leitura, bordas/sombras, texto sobre superfície, texto sobre acentos, linhas de grid e acentos semânticos.
- **SurfaceGeometry (`NeobrutalismMetrics`)**: Conjunto padronizado de métricas visuais (espessura de borda `2dp`–`2.5dp`, deslocamento de sombra dura `2dp`–`4dp`, raios `8dp`–`16dp`, passo de grid `24dp` com traço `1dp`, largura do painel lateral expandido).
- **TactilePressBehavior**: Comportamento de estado pressionado/habilitado que desloca o conteúdo na direção `(dx, dy)` da sombra e reduz o offset da sombra enquanto pressionado.
- **FolderAccentAssignment**: Atribuição determinística em memória de um token de acento (`accentYellow`, `accentPurple`, `accentOrange`, `accentBlue`) derivada da chave da pasta favorita, sem necessidade de persistência em disco.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos pares cor-de-texto/superfície e cor-de-texto/acento definidos para os modos Claro e Escuro (Catppuccin Mocha) atingem razão de contraste >= **4,5:1** para texto normal e >= **3,0:1** para componentes gráficos e texto grande (verificado por teste automatizado na JVM).
- **SC-002**: 0% de ocorrências de cores dinâmicas do sistema (`dynamicColor`) ou paleta roxa herdada (`Purple40`/`Purple80`/`Pink40`/`Pink80`) permanecem ativas nas telas, componentes e diálogos do aplicativo.
- **SC-003**: 100% dos cartões, botões principais, chips, FAB, painel flutuante e diálogos exibem borda sólida (`2dp`–`3dp`), sombra deslocada dura (`2dp`–`4dp`, `0dp` blur) e opacidade de 100% (`alpha = 1f`) sobre o fundo quadriculado.
- **SC-004**: 100% das páginas renderizadas do PDF mantêm pixel/cor originais sem filtros, inversão, borda brutal invasiva ou vazamento do grid `24dp x 24dp` atrás da folha, tanto em Tema Claro quanto em Tema Escuro.
- **SC-005**: 0 ocorrências de recorte (clipping) de bordas/sombras ou sobreposição de textos em larguras a partir de `360dp` até tablets expandidos em retrato e paisagem.
- **SC-006**: Alternância Claro ↔ Escuro do sistema atualiza 100% da UI imediatamente preservando estado de navegação, documento aberto, progresso, velocidade e diálogos abertos.
- **SC-007**: Rolagem da biblioteca e auto-scroll contínuo do PDF mantêm fluidez visual estável (`60fps`) sem introdução de trabalho bloqueante na main thread.
- **SC-008**: 100% da suíte de testes unitários existente + novos testes de contrato de tema passam (`./gradlew testDebugUnitTest`), com `lintDebug` e `assembleDebug` concluindo sem erros.

---

## Assumptions & Out of Scope

### Assumptions
- O tema segue automaticamente `isSystemInDarkTheme()` do Android na v1, sem persistência ou tela de escolha manual de tema.
- Os textos e rótulos atuais das telas e diálogos são mantidos no idioma original já presente em cada arquivo (sem tradução ou reescrita de copy nesta entrega).
- Os modelos de dados persistidos (`RecentDocument`, `FavoriteFolder`, `ReadingSession`, `BackupPayload`) permanecem inalterados; cores de pastas são derivadas deterministicamente em tempo de renderização.

### Out of Scope (v1)
- Seletor manual de tema dentro do aplicativo ou criação de nova tela de Configurações.
- Modo noturno/inversão de cores ou filtros sobre o conteúdo renderizado das páginas PDF.
- Novo ícone launcher do aplicativo ou splash screen customizada.
- Novas funcionalidades de produto (busca na biblioteca, tags, anotações, escolha manual de cor por pasta favorita, sincronização em nuvem).
- Tradução/padronização de idioma dos textos já existentes no aplicativo.
- Menu radial/em arco por gesto prolongado (`ArcActionMenu`).
