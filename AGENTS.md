# Regras e Diretrizes do Projeto: PDF Reader

Este arquivo define as regras de desenvolvimento e operação para agentes autônomos que atuam neste repositório.

---

## 1. Regra de Execução Autônoma do Spec Kit

- **Fluxo Contínuo Autorizado**: O agente **DEVE** seguir autonomamente o fluxo do Spec Kit (`/speckit-specify` → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement`) sempre que não houver bloqueios técnicos, falhas de compilação ou indefinições críticas de escopo.
- **Critério de Parada / Dúvida**: O agente só deve interromper o fluxo para questionar o usuário se:
  1. Houver um **bloqueio técnico** que impeça a compilação, execução de testes ou acesso ao sistema.
  2. Surgir uma **ambiguidade crítica de negócio/produto** sem padrão razoável evidente que afete substancialmente a experiência do usuário.
  3. Ocorrer violação injustificada dos princípios da Constituição do projeto.
- Na ausência desses bloqueios, proceda diretamente com o planejamento, quebra de tarefas e implementação.

---

## 2. Contexto Técnico do Projeto

- **Plataforma**: Aplicativo Android Nativo (Smartphones, Dobráveis e Tablets).
- **Linguagem**: Kotlin 2.4.0 (JVM target 17 via DSL `compilerOptions`).
- **UI Toolkit**: Jetpack Compose com Material 3 e Compose Adaptive (`WindowWidthSizeClass`).
- **Arquitetura**: Clean Architecture + Unidirectional Data Flow / MVI (`Intent` → `ViewModel` → `ViewState` / `SideEffect`).
- **Persistência**: AndroidX DataStore Preferences para preferências de rolagem, histórico recente e pastas favoritas.
- **Acesso a Arquivos**: Storage Access Framework (SAF) via `OpenDocument` e `OpenDocumentTree` com `takePersistableUriPermission`.
- **Renderização PDF**: `android.graphics.pdf.PdfRenderer` em background coroutine com reciclagem de bitmaps via `androidx.collection.LruCache`.

---

## 3. Diretrizes de Qualidade e Governança (Constituição)

1. **Performance em Primeiro Lugar**: Renderização suave a 60fps, processamento fora da main thread (`Dispatchers.IO`) e virtualização via `LazyColumn` / `LazyRow`.
2. **Motor Desacoplado**: Lógicas de cálculo de delta-tempo, velocidade de rolagem e modelos de dados devem ser módulos Kotlin puros testáveis na JVM sem dependência direta do Android framework.
3. **Test-Driven Development (TDD)**: Testes unitários para regras de negócio, limites de página e repositórios devem ser criados e validados antes da implementação.
4. **Design Responsivo**: Adaptação dinâmica entre telas compactas (celulares) e expandidas (tablets/navegação lateral).
5. **Tolerância a Falhas**: Tratamento elegante de arquivos/pastas movidos ou deletados fora do app, sem gerar crashes.

---

## 4. Comandos de Validação Rápida

```bash
# Executar todos os testes unitários na JVM
./gradlew testDebugUnitTest

# Compilar o APK de depuração
./gradlew assembleDebug

# Instalar no tablet/aparelho conectado
./gradlew installDebug
```
