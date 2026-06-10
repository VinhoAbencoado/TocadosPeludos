# CLAUDE.md — Toca dos Peludos

Guia para qualquer sessão de IA trabalhar neste repositório com resultado consistente.
**Antes de planejar qualquer tarefa nova, leia [CONTEXTO.md](CONTEXTO.md)** — ele contém o
estado do projeto, as **normas de design** (§2) e o **roadmap em blocos + sequência** (§3 e §4).

## O que é
App Android nativo **Toca dos Peludos**: conecta **adotantes** e **ONGs** (eventos pet, adoção de animais).
Há dois tipos de conta (adotante / ONG) com painéis distintos.

## Stack e restrições
- **Linguagem:** Java (não Kotlin). **Build:** Gradle Kotlin DSL.
- `minSdk 24`, `targetSdk 36`, tema **Material 3**.
- **Única dependência externa não-AndroidX:** ZXing (`com.google.zxing:core`) para QR Code.
  Não adicionar novas bibliotecas sem o usuário pedir.
- **Persistência: `SharedPreferences` local** (sem backend — ver Bloco 0 do CONTEXTO; backend está **adiado**).
  Não introduzir rede/backend sem solicitação explícita.

## Como compilar/rodar (Windows)
`JAVA_HOME` normalmente não está no ambiente; use o JBR do Android Studio:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug --console=plain
```
Para checagem rápida só de Java: `.\gradlew.bat :app:compileDebugJavaWithJavac --console=plain`.
Flags interativas do Gradle/git não são suportadas neste ambiente.

## Arquitetura e armazenamento
- [UserStorage](app/src/main/java/com/example/tocadospeludos/UserStorage.java) — prefs `user_db`: usuários
  (nome, **senha em texto puro — a corrigir**, `accountType` `adopter`/`ong`, `cnpj`), documentos e inscrições por usuário.
- [AppData](app/src/main/java/com/example/tocadospeludos/AppData.java) — prefs `app_data`: dados **globais**
  (eventos, animais, candidaturas) como arrays JSON. Semeia eventos de exemplo na 1ª execução.
- Modelos [Event](app/src/main/java/com/example/tocadospeludos/Event.java) e
  [Animal](app/src/main/java/com/example/tocadospeludos/Animal.java) (Serializable, com `toJson`/`fromJson`).
- **Não renomear chaves JSON existentes** (`idDocument`, `proofOfResidence`, `declaration`, `authorization`,
  `eventId`, `accountType`, `status`...) — quebraria dados já gravados.
- **Roteamento por tipo de conta:** login/sessão em [MainActivity](app/src/main/java/com/example/tocadospeludos/MainActivity.java)
  manda ONG → [OngHomeActivity](app/src/main/java/com/example/tocadospeludos/OngHomeActivity.java),
  adotante → [HomeActivity](app/src/main/java/com/example/tocadospeludos/HomeActivity.java).
  Ambas as Homes têm **guarda cruzada** (redirecionam se o tipo não bate). Mantenha esse padrão.
- Toda nova `Activity` deve ser registrada no [AndroidManifest.xml](app/src/main/AndroidManifest.xml).

## Convenções de UI/código
- **Reutilizar os estilos/tokens do design system** (CONTEXTO §2): `PrimaryButton`, `OutlineButton`,
  `DangerButton`, `FieldInput`, `bg_card`, `bg_header`, `bg_chip_done/pending`, cores nomeadas.
  Não criar cores/medidas avulsas.
- Listas dinâmicas são montadas **programaticamente** em código (cards via `LinearLayout` + helper `dp(int)`),
  com um container `LinearLayout` vazio no XML e um TextView de **estado vazio**. Siga esse padrão.
- **Textos visíveis ao usuário em português.** (Centralizar em `strings.xml` é meta do Bloco 6.)
- Anexos (foto/PDF) usam `ActivityResultContracts.OpenDocument` + `takePersistableUriPermission`.
- Ações destrutivas devem confirmar; toda operação dá feedback via `Toast`.

## Fluxo de trabalho esperado
1. Ler CONTEXTO.md e escolher tarefas seguindo a **sequência local-first** (§4).
2. Implementar respeitando as normas de design e os padrões acima.
3. **Compilar** (`assembleDebug`) e corrigir erros antes de concluir.
4. Atualizar os checkboxes do bloco correspondente no CONTEXTO.md ao concluir tarefas.
