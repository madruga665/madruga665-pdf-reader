# madruga665-pdf-reader

## Sobre

Leitor de PDF nativo para Android, pensado para celulares, dobráveis e tablets. O foco é a leitura confortável com rolagem automática: o documento desce sozinho na velocidade escolhida, sem precisar tocar na tela.

O app é escrito em Kotlin com Jetpack Compose (Material 3) e adapta o layout ao tamanho da tela. Os arquivos são abertos pelo seletor do sistema (Storage Access Framework) e as preferências ficam salvas localmente no aparelho.

## Funcionalidades

| Funcionalidade | Status |
|---|---|
| Rolagem automática com seletor de velocidade (px/s) | Implementada |
| Documentos recentes, com retomada da última página lida | Implementada |
| Pastas favoritas para acesso rápido aos PDFs | Implementada |
| Velocidade de rolagem salva por PDF | Implementada |
| Backup e restauração local em arquivo JSON | Implementada |
| Tema Neo-Brutalism claro e escuro (Catppuccin Mocha), seguindo o tema do sistema | Implementada |

## Requisitos de ambiente

- JDK 21 para executar o Gradle (exigido por `gradle/gradle-daemon-jvm.properties`); o código é compilado para Java 17
- Android SDK 35 (`compileSdk` e `targetSdk` 35)
- Aparelho ou emulador com Android 8.0 ou superior (`minSdk` 26)
- Android Studio ou o Gradle wrapper incluído no repositório (`./gradlew`), sem instalação separada do Gradle

## Como compilar e testar

Execute os comandos na raiz do repositório.

Roda todos os testes unitários na JVM:

```bash
./gradlew testDebugUnitTest
```

Compila o APK de depuração:

```bash
./gradlew assembleDebug
```

Instala o APK de depuração no aparelho ou emulador conectado:

```bash
./gradlew installDebug
```

## Como usar

1. **Abrir um PDF:** na tela inicial, toque no ícone de pasta (**Open PDF**) e escolha o arquivo no seletor do sistema.
2. **Ativar ou pausar a rolagem automática:** no leitor, toque no botão de play/pause.
3. **Ajustar a velocidade:** use os botões **−** e **+** do seletor de velocidade (o valor aparece em px/s).
4. **Salvar a velocidade do documento:** toque em **Salvar neste PDF**; o PDF passa a abrir com essa velocidade. **Restaurar** volta ao padrão geral do app.
5. **Retomar um documento recente:** na tela inicial, toque no documento na seção **Jump Back In**.
6. **Abrir uma pasta favorita:** fixe uma pasta pelo ícone **Pin folder** e depois toque nela em **Favorite Folders** para ver e abrir seus PDFs.
7. **Exportar ou importar backup local:** toque no ícone **Backup e Restauração** e escolha **Exportar backup (.json)** ou **Importar backup (.json)**; na importação, selecione **Mesclar (padrão)** ou **Substituir tudo** e confirme em **Restaurar**.

## Documentação

- [AGENTS.md](AGENTS.md): contexto técnico, arquitetura e regras de desenvolvimento do projeto.
- [specs/](specs/): especificações, planos e tarefas de cada funcionalidade.

Todo pull request para a branch `main` dispara o workflow de integração contínua (`.github/workflows/ci.yml`), que roda `testDebugUnitTest` e `assembleDebug`.
