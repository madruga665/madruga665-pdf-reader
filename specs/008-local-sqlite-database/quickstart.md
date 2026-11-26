# Quickstart & Roteiro de Validação: Banco de Dados Local SQLite (`008-local-sqlite-database`)

## 1. Preparação da Branch (`agy`)

Antes de implementar, confirme que a branch de trabalho parte de `origin/main` atualizado:

```bash
cd /home/madruga665/Projetos/madruga665-pdf-reader
git fetch origin
git checkout -b 008-local-sqlite-database origin/main
```

## 2. Build e Testes Automatizados na JVM (JDK 21)

O wrapper Gradle exige JDK 21 (`gradle/gradle-daemon-jvm.properties`) e compila para Java 17 (`app/build.gradle.kts`).

```bash
# Executar toda a suíte unitária (inclui testes SQLite reais com sqlite-jdbc)
./gradlew testDebugUnitTest

# Compilar APK de depuração
./gradlew assembleDebug

# Instalar em dispositivo/emulador conectado (quando aplicável ao QA)
./gradlew installDebug
```

## 3. Verificação Rápida de Higiene

```bash
# Confirmar ausência de menções a JSON na interface/mensagens de backup
rg -n -i "json" app/src/main/java/com/madruga665/pdfreader/ui app/src/main/java/com/madruga665/pdfreader/core/backup
```

## 4. Roteiro de Validação Funcional (Mapeamento `CA-01`..`CA-13`)

1. **Cadastro e ID Único (`CA-01`)**: Abrir um PDF inédito na Home → verificar criação em `pdf_documents` com `id` UUID v4; fechar e reabrir → confirmar mesmo `id` sem duplicação.
2. **Persistência de Leitura (`CA-02`)**: Alterar velocidade, clicar em "Salvar neste PDF", avançar para página 5, aplicar zoom, matar processo do app e reabrir o mesmo PDF → confirmar página 5, zoom e velocidade restaurados.
3. **Migração DataStore (`CA-03`)**: Partir de instalação prévia com dados no DataStore Preferences, atualizar APK e abrir duas vezes → confirmar todos os recentes, velocidades e pastas migrados sem duplicação.
4. **Indisponibilidade sem Crash (`CA-04`)**: Com um PDF salvo a `160 px/s` na página 5, renomear/mover/apagar o arquivo fora do app e voltar à Home → confirmar estado indisponível com configurações preservadas e zero crashes.
5. **Restaurar PDF (`CA-05`, `CA-06`, `CA-07`)**:
   - Acionar **"Restaurar PDF"**, selecionar arquivo com menos páginas → confirmar mesmo `id`, velocidade `160 px/s` mantida e página ajustada para `pageCount - 1`.
   - Testar cancelamento do seletor e seleção de arquivo inválido → confirmar registro intacto e mensagem clara.
   - Tentar revincular apontando para URI de outro PDF já cadastrado → confirmar diálogo de conflito explícito sem duplicação.
6. **Backup & Restauração `.db` (`CA-08`, `CA-09`, `CA-10`, `CA-11`, `CA-13`)**:
   - Exportar backup → verificar extensão `.db` e resumo das contagens.
   - Tentar importar arquivo `.json`, `.txt` e `.db` corrompido → confirmar rejeição sem alterar banco local.
   - Importar `.db` válido via **Substituir** e via **Mesclar** → confirmar contagens da prévia e estado resultante correto.
   - Conferir que nenhum diálogo ou mensagem menciona JSON.
7. **Recuperação Pós-Backup (`CA-12`)**: Restaurar `.db` com URI inacessível no aparelho, acionar **"Restaurar PDF"** no card importado e abrir mantendo as configurações trazidas pelo backup.
