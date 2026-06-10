# CONTEXTO — Toca dos Peludos

> Documento de contexto e planejamento técnico do aplicativo **Toca dos Peludos**.
> Reúne o **estado atual**, as **normas de design**, os **blocos de tarefas** necessários
> para o funcionamento pleno e a **sequência recomendada** de execução.
>
> Aplicativo Android nativo (Java), `minSdk 24` / `targetSdk 36`, tema Material 3.
> Persistência atual: **SharedPreferences local** (protótipo, sem backend).

---

## 1. Estado atual do projeto

### 1.1. O que já existe e funciona
- **Autenticação local** ([MainActivity](app/src/main/java/com/example/tocadospeludos/MainActivity.java),
  [RegisterActivity](app/src/main/java/com/example/tocadospeludos/RegisterActivity.java),
  [RecoverActivity](app/src/main/java/com/example/tocadospeludos/RecoverActivity.java)).
- **Dois tipos de conta**: Adotante e ONG, escolhidos no cadastro; login roteia para o painel certo
  ([UserStorage](app/src/main/java/com/example/tocadospeludos/UserStorage.java)).
- **App do adotante** ([HomeActivity](app/src/main/java/com/example/tocadospeludos/HomeActivity.java)):
  feed de eventos dinâmico, seção "Adote um amigo" com candidatura, perfil com documentos e inscrições, aba Sobre/Suporte.
- **Documentos de adoção** ([DocumentActivity](app/src/main/java/com/example/tocadospeludos/DocumentActivity.java)):
  formulário completo (dados pessoais + anexos foto/PDF), com pré-preenchimento.
- **Eventos + QR Code** ([EventDetailActivity](app/src/main/java/com/example/tocadospeludos/EventDetailActivity.java),
  [QRCodeGenerator](app/src/main/java/com/example/tocadospeludos/QRCodeGenerator.java) via ZXing):
  inscrição gera QR salvo no perfil.
- **Painel da ONG** ([OngHomeActivity](app/src/main/java/com/example/tocadospeludos/OngHomeActivity.java)):
  CRUD parcial de eventos e animais (criar/excluir), candidaturas (aprovar/recusar), conta.
- **Camada de dados global** ([AppData](app/src/main/java/com/example/tocadospeludos/AppData.java))
  para eventos, animais e candidaturas; modelos [Event](app/src/main/java/com/example/tocadospeludos/Event.java)
  e [Animal](app/src/main/java/com/example/tocadospeludos/Animal.java).
- **Design system** aplicado (cores, tema, estilos e drawables — ver seção 2).

### 1.2. Limitações conhecidas (impedem o "funcionamento pleno")
- **Sem backend**: tudo é local; ONG e adotante só interagem no mesmo dispositivo. Não há sincronização nem multiusuário real.
- ~~**Segurança fraca**: senhas em texto puro~~ → **resolvido (Bloco 1)**: senhas em hash SHA-256 com salt. Ainda sem token de sessão (sessão local via `current_user`).
- ~~**Recuperação de senha é fictícia**~~ → **sinalizado (Bloco 1)**: continua simulação (sem backend), mas avisa o usuário e valida o e-mail.
- **Sem edição de eventos** (apenas criar e excluir). *(Animais já são editáveis — Bloco 4.)*
- ~~**Fotos não são exibidas**~~ → **resolvido (Bloco 4)**: fotos de animais renderizadas nos cards e no detalhe via `ImageUtils`. Anexos de documentos ainda não são abertos (Bloco 2).
- ~~**Sem feedback ao adotante** sobre o resultado da candidatura~~ → **resolvido (Bloco 4)**: tela "Minhas candidaturas" mostra o status.
- **Validações parciais**: e-mail, força de senha, confirmação, CNPJ e telefone já validados com `setError` (Bloco 1). **CPF e máscaras de entrada** (CPF/telefone/data) ainda pendentes (Bloco 2).
- **Datas de evento são texto livre** ("20 mai"); não há ordenação nem filtro de eventos passados.
- **Strings fixas no código/layout** (não centralizadas em `strings.xml`); sem i18n.
- **Sem testes** automatizados; sem diálogos de confirmação para ações destrutivas.

---

## 2. Normas de design

> O sistema visual já está implementado em `res/values/` e `res/drawable/`.
> **Toda nova tela deve reutilizar estes tokens e estilos — não criar cores/medidas avulsas.**

### 2.1. Paleta de cores ([colors.xml](app/src/main/res/values/colors.xml))
| Token | Hex | Uso |
|---|---|---|
| `green_primary` / `dark_green` | `#2E7D32` | Cor primária da marca, botões, ícones ativos |
| `green_dark` | `#1B5E20` | Gradiente do header, status bar |
| `green_container` / `on_green_container` | `#DBEFDC` / `#15401A` | Chips e realces verdes |
| `amber` / `amber_container` | `#F49E0B` / `#FCEFD2` | Acento e chip "pendente" |
| `grey` | `#EEF2EC` | Fundo geral das telas |
| `surface` | `#FFFFFF` | Cards e campos |
| `outline` | `#DCE3DA` | Bordas sutis |
| `text_primary` / `text_secondary` | `#1A1C19` / `#586056` | Texto principal / secundário |
| `status_pending` / `status_done` / `danger` | `#9A6300` / `#1B5E20` / `#C62828` | Status e ações destrutivas |

### 2.2. Tema ([themes.xml](app/src/main/res/values/themes.xml))
- Base **Material 3 Light** (claro também no modo escuro, para manter identidade).
- `colorPrimary`, `colorPrimaryContainer`, `colorSecondary`, `colorSurface`, `colorOutline` mapeados à paleta.
- Status bar `green_dark`; fundo `grey`.

### 2.3. Tipografia
- `Text.Heading` — 26sp bold (títulos de tela).
- `Text.Section` — 18sp bold (títulos de seção/card).
- `Text.Body` — 14sp, `text_secondary` (texto corrido).
- Títulos de seção em telas: 20–22sp bold, `text_primary`.

### 2.4. Componentes (estilos reutilizáveis)
- **Botões**: `PrimaryButton` (verde, cantos 14dp, 52dp altura), `OutlineButton` (contorno verde, ações secundárias/Voltar), `DangerButton` (vermelho, ações destrutivas). Sempre `textAllCaps=false`.
- **Campos**: `FieldInput` — fundo `bg_field` (borda some/realça no foco), cantos 12dp, padding 14dp.
- **Cards**: `bg_card` — superfície branca, cantos 20dp, borda `outline`, elevação 2dp.
- **Header**: `bg_header` — gradiente verde com cantos inferiores 28dp.
- **Chips**: `bg_chip_done` (verde) / `bg_chip_pending` (âmbar) para status.

### 2.5. Convenções de espaçamento e layout
- Padding lateral das telas: **20dp**.
- Padding interno de cards: **18–24dp**; margem entre cards: **14dp**.
- Raios de canto: campos **12dp**, botões **14dp**, cards/chips **20dp**.
- Navegação por `BottomNavigationView` com `nav_item_color` (verde quando selecionado).
- Listas dinâmicas: sempre ter **estado vazio** (TextView de placeholder) controlado por visibilidade.

### 2.6. Regras de UX
- Toda ação destrutiva (excluir, deletar conta, recusar) deve ter **diálogo de confirmação** (a implementar).
- Toda operação deve dar **feedback** (Toast/Snackbar) de sucesso ou erro.
- Campos obrigatórios marcados com `*`; opcionais com `(opcional)`.
- Textos visíveis ao usuário em **português**, centralizados em `strings.xml` (meta).

---

## 3. Blocos de tarefas

> Marque `[x]` ao concluir. Prioridade: 🔴 crítica · 🟡 importante · 🟢 desejável.

### Bloco 0 — Fundação técnica e persistência 🟢 (ADIADO)
> **Decisão travada:** a estratégia atual é **persistência local com `SharedPreferences`**
> (via `UserStorage` e `AppData`). O backend remoto fica **adiado** — só será necessário para
> tornar o app **multiusuário/multidispositivo** (ONG e adotante interagindo de aparelhos diferentes).
> **Não introduzir backend nem dependências de rede sem o usuário pedir explicitamente.**
>
> Enquanto local, vale antecipar apenas a abstração que facilita a troca futura:
- [ ] Criar camada de **repositório** abstraindo `AppData`/`UserStorage` (interface + implementação local), para um backend futuro entrar sem reescrever as telas.
- [ ] *(Futuro, ao escalar)* Definir backend (Firebase Auth+Firestore/Storage ou API REST), modelar coleções `users`/`events`/`animals`/`applications`/`registrations`, migrar auth, upload de mídia e estados de rede.

### Bloco 1 — Autenticação e conta completas ✅
- [x] **Hash de senha** — SHA-256 com salt por usuário em [PasswordUtils](app/src/main/java/com/example/tocadospeludos/PasswordUtils.java); `registerUser` grava hash e `login`/`changePassword` migram senhas legadas em texto puro automaticamente.
- [x] **Recuperação de senha real** — *requer backend/e-mail, portanto adiado*; [RecoverActivity](app/src/main/java/com/example/tocadospeludos/RecoverActivity.java) mantida como stub, agora validando o e-mail e **avisando explicitamente que é uma simulação** (diálogo).
- [x] Validação de formato: e-mail, senha forte (mín. 8, letras + números), confirmação de senha, telefone e CNPJ (14 dígitos) — todos com `setError` por campo no cadastro/login.
- [x] Edição de perfil (nome, telefone, senha; dados da ONG) via diálogos no perfil/conta (`updateProfile`/`changePassword` em [UserStorage](app/src/main/java/com/example/tocadospeludos/UserStorage.java)).
- [x] Diálogo de confirmação no **logout** e no **deletar conta** (adotante e ONG).
- [x] Sessão: login mantido via `current_user` no `SharedPreferences`; `logout`/`deleteAccount` encerram corretamente. *(Token de sessão real fica para o backend — adiado.)*

### Bloco 2 — Documentos do adotante 🟡
- [ ] Exibir/visualizar anexos enviados (abrir foto/PDF) no perfil.
- [ ] Máscaras de entrada: **CPF**, **telefone**, **data de nascimento** ([DocumentActivity](app/src/main/java/com/example/tocadospeludos/DocumentActivity.java)).
- [ ] Validar CPF (dígitos verificadores) e data.
- [ ] Copiar arquivos anexados para armazenamento do app (não depender da URI externa).
- [ ] Indicador de progresso de preenchimento (ex.: "7 de 9 concluídos").

### Bloco 3 — Eventos (ciclo completo) 🟡
- [ ] **Editar** evento (ONG) — hoje só criar/excluir.
- [ ] Data real (date picker) em vez de texto livre; **ordenar** por data e **ocultar eventos passados**.
- [ ] Confirmação ao excluir evento.
- [ ] **Check-in por QR**: tela na ONG para **ler/validar** o QR do adotante no evento (scanner ZXing).
- [ ] Listar participantes inscritos por evento (visão da ONG).
- [ ] Cancelar inscrição (adotante).

### Bloco 4 — Animais e adoção ✅
- [x] **Exibir foto** do animal nos cards (adotante e ONG) — via [ImageUtils](app/src/main/java/com/example/tocadospeludos/ImageUtils.java) (downsampling para evitar OOM).
- [x] **Tela de detalhe do animal** (adotante) com foto, descrição completa e botão candidatar-se — [AnimalDetailActivity](app/src/main/java/com/example/tocadospeludos/AnimalDetailActivity.java) (cards do feed abrem o detalhe ao toque).
- [x] **Editar** animal e alternar status manualmente (disponível/adotado) no painel da ONG (diálogo + botão "Marcar adotado"/"Reabrir"); excluir agora pede confirmação.
- [x] **"Minhas candidaturas"** (adotante): [MyApplicationsActivity](app/src/main/java/com/example/tocadospeludos/MyApplicationsActivity.java) com status pendente/aprovada/recusada e orientação por status (acessível pelo botão no Perfil).
- [x] Vincular candidatura aos **documentos** do adotante: botão "Ver dados do adotante" em cada candidatura mostra dados pessoais e quais anexos foram enviados.
- [x] Notificar adotante sobre o resultado: o status (e orientação) aparece em "Minhas candidaturas". *(Push real precisa de backend — adiado.)*
- [x] Busca/filtro de animais por nome, espécie ou ONG (campo de busca no feed). *(Filtro por "porte" exigiria novo campo no modelo — fica para depois.)*

### Bloco 5 — Gestão da ONG 🟡
- [ ] Confirmação ao excluir animal/evento e ao recusar candidatura.
- [ ] Dashboard com contadores (eventos ativos, animais disponíveis, candidaturas pendentes).
- [ ] Histórico de adoções concluídas.
- [ ] Perfil público da ONG (visível ao adotante a partir do animal/evento).

### Bloco 6 — Qualidade, validação e acessibilidade 🟡
- [ ] Centralizar **todas as strings** em [strings.xml](app/src/main/res/values/strings.xml).
- [ ] `contentDescription` em ícones/imagens; alvos de toque ≥ 48dp.
- [ ] Erros por campo (`setError`) em vez de só Toast genérico.
- [ ] Estados vazios/erro/carregamento padronizados.
- [ ] Tratar rotação/`savedInstanceState` em formulários.

### Bloco 7 — Design e polimento 🟢
- [ ] Ícone do app e splash com a marca (pata + verde).
- [ ] Animações/transições sutis entre telas.
- [ ] Migrar `<Button>`/`<EditText>` para componentes Material (`MaterialButton`, `TextInputLayout`) onde fizer sentido.
- [ ] Modo escuro real (paleta dedicada) — opcional.
- [ ] Imagens de placeholder para animais sem foto.

### Bloco 8 — Testes, build e release 🟢
- [ ] Testes unitários do repositório/validações.
- [ ] Testes de UI (Espresso) dos fluxos principais (cadastro, candidatura, criar evento).
- [ ] Configurar `release` (assinatura, `minify`, regras ProGuard).
- [ ] Política de privacidade e termos (LGPD — há dados pessoais sensíveis).
- [ ] Revisão de permissões e `dataExtractionRules`/`backup_rules`.

### Definição de pronto por bloco (critérios de aceitação)
- **Bloco 1**: senha nunca é gravada em texto puro; cadastro rejeita e-mail/CNPJ/senha inválidos com erro no campo; perfil editável persiste após reabrir o app; logout e deletar conta exigem confirmação.
- **Bloco 2**: cada anexo enviado pode ser aberto/visualizado; CPF/telefone/data têm máscara e CPF é validado; o app compila e os dados sobrevivem ao reinício.
- **Bloco 3**: ONG cria, **edita** e exclui eventos; data é escolhida por date picker; lista ordenada por data e sem eventos passados; ONG consegue validar (ler) o QR de um adotante.
- **Bloco 4**: foto do animal aparece no card/detalhe; adotante abre o detalhe e se candidata; adotante vê o status em "Minhas candidaturas"; aprovar marca o animal como adotado e some do feed.
- **Bloco 5**: painel mostra contadores corretos; ações destrutivas confirmam; histórico de adoções reflete aprovações.
- **Bloco 6**: nenhuma string fixa em layouts/código (tudo em `strings.xml`); erros aparecem por campo; toda lista tem estado vazio; formulários sobrevivem à rotação.
- **Bloco 7/8**: ícone/splash com a marca; fluxos principais cobertos por teste; build `release` assinado gera APK/AAB.

---

## 4. Sequência recomendada de execução (local-first)

> Como o backend está **adiado** (seção Bloco 0), a ordem prioriza o **máximo de valor possível
> mantendo persistência local** num único dispositivo. O multiusuário real entra só no fim, via Bloco 0.

1. **Bloco 1 — Conta** (hash de senha, validações de cadastro, editar perfil, diálogos de confirmação). *Recuperação real fica adiada (precisa de backend).*
2. **Bloco 4 — Animais e adoção** (foto no card/detalhe, tela de detalhe, "Minhas candidaturas", aprovar→adotado). É o coração do produto e 100% viável local.
3. **Bloco 3 — Eventos** (editar, date picker, ordenação, leitura/validação de QR no check-in).
4. **Bloco 2 — Documentos** (visualizar anexos, máscaras, validação) — pode correr em paralelo ao 3/4.
5. **Bloco 5 — Gestão da ONG** (dashboard, histórico, perfil público da ONG).
6. **Bloco 6 — Qualidade/validação/acessibilidade** (transversal; aplicar ao longo dos blocos acima).
7. **Bloco 7 — Polimento de design** e **Bloco 8 — Testes/release**.
8. **Bloco 0 — Backend** *(somente quando for escalar para multidispositivo)*.

### Marcos sugeridos
- **MVP local**: Blocos 1 + 4 (cadastro seguro + adoção ponta a ponta no mesmo aparelho).
- **Versão beta local**: + Blocos 2 e 3 (documentos visualizáveis/validados e eventos com check-in por QR).
- **Versão de lançamento**: + Blocos 5, 6, 7 e 8.
- **Multidispositivo**: + Bloco 0 (backend) — transforma o protótipo local em serviço real.

---

*Última análise baseada no código em `app/src/main/`. Atualize este documento conforme os blocos forem concluídos.*
