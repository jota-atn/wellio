# Wellio — Especificação

> Documento vivo. Decisões tomadas na §14; pontos em aberto na §15.
> Itens em **Decisões em aberto** precisam de resposta antes da implementação da parte correspondente.

## 1. Visão geral

Wellio é um app Android pessoal para acompanhar hábitos de saúde do dia a dia: **água**, **alimentação** e **exercício**, com um **dashboard** de progresso, metas, conquistas, estatísticas e um espaço geral de **anotações**.

### Princípios

- **Uso pessoal, offline primeiro.** Tudo funciona sem internet e fica salvo no aparelho. Login e sincronização ficam para o futuro, mas o modelo de dados já nasce preparado para isso (ver §4.3).
- **Registrar tem que ser rápido.** Ações comuns (beber água, repetir uma refeição) devem levar 1–2 toques.
- **Nada se perde.** Todo registro é persistido, editável e removível.
- **Personalizável.** A tela principal é do usuário: ordem e tamanho dos cards são configuráveis.

### Fora do escopo (por enquanto)

- Sono e saúde mental (removidos).
- Login, conta e sincronização em nuvem (futuro, §11).
- Versão iOS/web.

## 2. Arquitetura

| Item | Escolha |
|---|---|
| UI | Jetpack Compose + Material 3, single-activity |
| Navegação | Navigation Compose + `ModalNavigationDrawer` (sidebar) |
| Estado | MVVM: `ViewModel` expondo `StateFlow`, alimentado por `Flow` do banco |
| Banco | Room (via KSP) |
| Preferências | DataStore (perfil, metas diárias, configurações, layout da Home) |
| Injeção | Manual, via `AppContainer` na `Application`. Migrar para Hilt só se crescer |
| Tarefas agendadas | WorkManager (lembretes periódicos) + AlarmManager (lembretes com hora exata) |
| Gráficos | Vico (Compose) |
| Imagens | Coil (foto de perfil) |
| Drag & drop | `sh.calvin.reorderable` (grid/lista reordenável em Compose) |

### 2.1 Estrutura de pacotes

```
com.projeto.wellio
├── WellioApp.kt              // Application + AppContainer
├── MainActivity.kt
├── navigation/               // rotas e NavHost
├── data/
│   ├── db/                   // AppDatabase, DAOs, entidades, seed
│   ├── prefs/                // DataStore (perfil, settings, layout)
│   └── repository/
├── domain/
│   ├── calories/             // cálculo de kcal (METs, porções, TMB)
│   ├── goals/                // cálculo de progresso de metas
│   └── achievements/         // definições e motor de conquistas
└── ui/
    ├── theme/
    ├── icons/                // WellioIcon (ver §2.3)
    ├── components/           // componentes reutilizáveis
    ├── home/                 // Home + cards
    ├── water/ food/ exercise/
    ├── profile/ goals/ achievements/ stats/ settings/
    └── notes/
```

O pacote declarado nos arquivos deve bater com a pasta (hoje `ui/components` declara `com.projeto.wellio.components`).

### 2.2 Navegação

```
Sidebar (drawer)
├── Cabeçalho: foto + nome + rank + informações  → modal de Perfil
├── Registros
│   ├── Água
│   ├── Alimentação
│   ├── Exercício
│   └── Peso
├── Estatísticas   (abas: Dia a dia | Visão geral)
├── Conquistas
├── Anotações
├── Receitas       (Anotações filtrada em receitas)
├── Metas
└── Configurações

Home → cards (atalhos para as mesmas telas)
     → FAB "+" (registro rápido)
```

**A Home é um atalho, não a única porta.** Toda funcionalidade é acessível pela sidebar (seção Registros e demais itens) ou pelo FAB, mesmo que nenhum card dela esteja na Home. Ocultar um card nunca tira acesso a nada.

**Toda funcionalidade tem tela própria** (Água, Peso, Exercício, Alimentação e, no M2, a Visão geral do Dashboard), com a mesma linguagem visual (§2.9). Os dialogs antigos de detalhes (Dashboard, Alimentação, Exercício) saem quando a tela correspondente chega.

O registro de alimento e de exercício abre num **bottom sheet em tela cheia** (busca, favoritos/recentes, código de barras, quantidade); arrastar para baixo fecha. Dialogs ficam só para interações pequenas (definir meta, confirmações). As telas de detalhe servem para histórico, edição e cadastro.

### 2.3 Ícones

Os emojis atuais são **provisórios**. A versão final usa ícones SVG (importados como Vector Drawables / `ImageVector`).

Para trocar sem retrabalho:
- Entidades guardam um **`iconKey: String`** (ex.: `"food.rice"`, `"exercise.run"`), nunca o emoji nem o recurso.
- `ui/icons/WellioIcon.kt` resolve `iconKey → ícone`. Durante a prototipação a resolução devolve o emoji; depois, o SVG. Chave desconhecida cai num ícone genérico da categoria.
- Ícones de UI (navegação, botões) usam o mesmo mecanismo.

### 2.4 Idioma

Só **pt-BR** por enquanto, mas todos os textos de interface ficam em `res/values/strings.xml` (hoje estão fixos no código), para permitir tradução futura. Nomes de alimentos e exercícios da base ficam no seed do banco, também só em pt-BR.

### 2.5 Tema e cores

- Paleta definida:

  | Módulo | Cor | Uso |
  |---|---|---|
  | Base | azul-marinho `#152259` | Dashboard, sidebar, FAB, botões gerais (cor primária do app) |
  | Água | azul | card, telas e registros de água |
  | Alimentação | verde | card, telas e registros de comida |
  | Exercício | coral `#C4471B` | card, telas e registros de treino |

- Cada módulo tem os tokens `container` (fundo do card), `onContainer` (texto sobre ele), `accent` (barras, anéis, botões), `onAccent` (texto sobre o destaque) e `track` (trilho das barras), em `ui/theme/Color.kt`, acessados por `WellioTheme.colors.<módulo>`. Pares de texto verificados para contraste AA nos dois temas.
- Cada módulo tem uma **cor de identidade** (Água = azul, Alimentação = verde, Exercício, Dashboard etc.), com variantes para **tema claro e escuro**.
- As cores ficam em tokens no tema (`ui/theme`), por exemplo `WellioColors.water.container` / `.onContainer` / `.accent`. Nenhum card usa `Color(0xFF...)` fixo.
- `GoalType` deixa de carregar `Color`: a cor é resolvida pelo tema a partir do tipo.
- Contraste mínimo WCAG AA para texto sobre os containers nos dois temas.

### 2.6 Domínio funcional

Toda regra de negócio fica em `domain/` como **funções puras**: recebem dados e devolvem resultados, sem acessar banco, DataStore, relógio ou Android.

- O "agora" e o "hoje" entram como **parâmetro** (`today: LocalDate`, `now: Instant`), nunca lidos com `LocalDate.now()` dentro do domínio.
- Entradas e saídas são `data class` imutáveis. Ex.: `closeDay(day: DayRecords, goals: Goals, freezes: Int): DayResult`, `evaluateAchievements(history: History, unlocked: Set<Unlock>): List<Unlock>`, `homeRows(availableHeightDp, fontScale): RowLayout`.
- ViewModels e repositórios só buscam os dados, chamam essas funções e gravam o resultado.

### 2.7 Testes

Testes **modulares e puros**: cada teste tem uma responsabilidade só, monta o mínimo de estado possível e verifica "esta entrada → esta saída".

| Tipo | O que cobre | Onde roda |
|---|---|---|
| **Unitários do domínio** (JUnit, Kotlin puro) | cálculo de kcal (METs, porções, gramas), meta calórica sugerida, fechamento do dia, metas diárias/semanais/de período, sequências e folgas, conquistas, XP e nível, cálculo de linhas da Home, parsing de números com vírgula | JVM, rápido |
| **Migração do banco** (Room `MigrationTestHelper`) | um teste por migração: cria dados na versão antiga, migra, valida o schema novo e confere que os dados sobreviveram | emulador |
| **UI (Compose)** | fluxos principais: registrar água e desfazer, registrar alimento por medida caseira, registrar treino, editar o layout da Home, onboarding; cada card nas versões inteiro/metade/mini com fonte em 100% e 130% | emulador |

Regras:
- Toda função nova em `domain/` entra **com testes no mesmo commit**. Sem meta de % de cobertura.
- Cada card novo entra com seus testes de UI nos tamanhos que oferece.
- Toda mudança de versão do banco entra com sua migração e o teste dela. Os schemas do Room são exportados em `app/schemas/` e versionados no git.
- Sem mocks no domínio: como as funções são puras, os testes passam dados de verdade.
- Dados de teste montados por pequenas funções construtoras (ex.: `waterEntry(ml = 300, at = "08:00")`), com valores padrão para tudo que não importa ao teste.
- Nomes de teste descrevem o comportamento: `` `folga cobre o dia perdido e mantém a sequência` ``.

**CI (GitHub Actions)**:
- A cada push e PR: build, lint e testes unitários.
- Em PRs para a `main`: também os testes de UI num emulador.

### 2.8 Acessibilidade

- **Fonte do sistema até 200%** é suportada. Nada é cortado: o conteúdo dos cards se adapta, e a Home rola quando precisa.
  - A altura base da linha cresce com a fonte (§5.1); com fonte muito grande cabem poucas linhas, e a regra de "mínimo de 3 linhas" deixa de valer.
  - Acima de ~150%, cards "metade" são exibidos como "inteiro" (empilhados), sem perder a configuração salva.
  - As versões "metade" e "mini" podem esconder detalhes secundários para caber, mas a informação principal (valor e meta) sempre aparece.
- **Animações**: com "Remover animações" ligado no Android, o app desliga confetes, a tremidinha do modo de edição e as transições; toasts aparecem sem deslizar.
- **TalkBack completo**:
  - todo card tem uma descrição-resumo falada (ex.: "Água: 1.400 de 2.000 mililitros, 70 por cento"), usando a descrição falada do resumo (§5.3);
  - todo botão e ícone tem descrição com contexto ("Apagar registro das 14h", não só "Apagar");
  - **reorganizar sem arrastar**: cards da Home e itens de listas oferecem as ações "Mover para cima", "Mover para baixo" e "Trocar tamanho" no menu de ações do TalkBack;
  - áreas de toque de no mínimo 48dp;
  - toasts de parabenização são anunciados pelo leitor de tela.
- **Cor nunca é a única informação**: estados como "acima da meta" também aparecem em texto (ex.: `+320 kcal`).
- Contraste mínimo WCAG AA nos dois temas (§2.5).

### 2.9 Linguagem visual das telas

Cada tela de funcionalidade tem **personalidade própria**, com um elemento ilustrativo animado no topo, em vez de só cards e listas:

- **Água**: uma gota grande que enche até a % do dia, com onda suave contínua; ao registrar, o nível sobe com um "chacoalhão" elástico. Ao registrar com a meta já batida (ou ao batê-la), a gota **transborda**: gotinhas pulam para fora e escorrem pela lateral.
- **Peso**: uma **balança de banheiro** vista de cima, com **agulha vermelha** no visor. Ao abrir a tela, duas pegadas aparecem uma de cada vez, cada uma afundando a balança, e a fita numerada gira com mola até o peso atual. Uma pesagem nova faz a balança quicar e a fita rolar do peso antigo para o novo. Bater o peso alvo solta um confete discreto. A área sob a curva de evolução "respira" de leve. Registro por **régua deslizante** de 0,1 kg, com vibração a cada 0,5 kg (mais forte a cada 1 kg), botões −/+ e opção de digitar.
- Exercício e Alimentação seguem a mesma ideia quando chegarem.
- **O card de cada funcionalidade na Home usa o mesmo elemento ilustrativo e os mesmos ícones da tela** (a gota no card de Água, a balança no card de Peso), em versão compacta (M4).

Com "Remover animações" ligado, os elementos ficam parados (§2.8).

**Transições entre telas** dependem de por onde se entra:
- **Pela sidebar**: deslize com mola solta ("gelatina"): passa do ponto, volta e assenta devagar.
- **Pelo card da Home**: o **card vira a tela** (transformação de container): o card cresce com mola até ocupar a tela, e os elementos ilustrativos voam do card para a tela (a gota do card vira a gota grande da Água; o visor do card vira o visor da balança do Peso). Voltar faz o caminho inverso: a tela encolhe de volta para dentro do card. O **gesto de voltar** dispara a mesma animação ao ser concluído; durante o arrasto a tela não acompanha o dedo (sem a prévia do voltar preditivo), porque a prévia comprime a mola e a volta fica rápida demais.

## 3. Regras gerais

- **Dia** = data local do aparelho (`LocalDate`), virando à **meia-noite**. Todo registro guarda `date` (dia a que pertence) e `timestamp` (momento exato).
- É possível registrar em **dias anteriores** (seletor de data nas telas de detalhe).
- **Semana** começa no dia configurado (padrão: segunda).
- Números decimais aceitam **vírgula e ponto** (`1,5` e `1.5`). Entrada inválida mostra erro no campo; nunca falha em silêncio.
- Toda remoção oferece **Desfazer** (snackbar) por alguns segundos.

## 4. Modelo de dados

### 4.1 Room

| Tabela | Campos principais |
|---|---|
| `water_entry` | id, date, timestamp, amountMl, containerName? |
| `water_container` | id, name, amountMl, isQuick, position |
| `food_item` | id, name, category, kcalPer100g, proteinPer100g?, carbsPer100g?, fatPer100g?, source (taco / extra / custom / barcode), sourceRef?, barcode?, iconKey, isFavorite |
| `food_portion` | id, foodItemId, name (ex.: "1 concha média"), grams, isDefault |
| `food_entry` | id, date, timestamp, foodItemId, meal?, portionId?, quantity, grams, kcalTotal, proteinG?, carbsG?, fatG? |
| `exercise_type` | id, name, category, mets, compendiumCode?, iconKey, isCustom |
| `exercise_entry` | id, date, timestamp, exerciseTypeId, minutes, kcal, kcalIsManual, source (manual / template / health_connect), externalId? |
| `workout_template` | id, name, iconKey |
| `workout_template_item` | id, templateId, exerciseTypeId, minutes, position |
| `weight_entry` | id, date, weightKg |
| `goal` | id, title, metric, period, target, startDate, endDate, status |
| `achievement_unlock` | achievementId, tier, unlockedAt, xp |
| `streak_freeze` | id, earnedAt, usedOn? (data coberta) |
| `note` | id, type, title, body, tags, pinned, color?, date?, reminderAt?, archived |
| `checklist_item` | id, noteId, text, done, position |

- `food_item`, `food_portion` e `exercise_type` são populados (seed) a partir das bases de dados (§7.5 e §8.1). Os cadastrados pelo usuário têm `source = custom` / `isCustom = true`.
- `food_entry.kcalTotal` e `exercise_entry.kcal` são **gravados no momento do registro**. Editar um alimento ou o peso depois não reescreve o histórico.
- Definições de conquistas ficam em código; o banco só guarda o desbloqueio.

### 4.2 DataStore

- **Perfil**: nome, fotoUri?, dataNascimento?, sexo?, alturaCm?, nivelAtividade?, biotipo?, objetivo?, pesoAlvoKg?, tituloExibido?, onboardingConcluido
- **Metas diárias e semanais**: água (ml), kcal consumidas, exercício (unidade: kcal ou minutos); para cada uma, semanal automática (×7) ou valor próprio
- **Configurações**: ver §9.3
- **Layout da Home e do painel rápido**: listas ordenadas de instâncias de card `{instanceId, cardType, size, config}`; `config` guarda o que é próprio da instância (ex.: lista do Checklist, linhas e nome do Combinado)

### 4.3 Fase de desenvolvimento

Até o fim do **M1**, o banco usa `fallbackToDestructiveMigration`: mudar o formato apaga os dados de teste. Ao fechar o M1, o schema vira a **versão 1 oficial**, o fallback sai, e a partir daí toda mudança de versão tem migração e teste (§2.7). O uso "pra valer" começa depois do M1.

### 4.4 Preparação para sync futuro

- IDs como **UUID** (`String`), não autoincremento.
- Todas as tabelas com `createdAt` e `updatedAt`.
- Remoção como **soft delete** (`deletedAt`), com limpeza periódica de itens apagados há mais de 30 dias.

## 5. Primeiro uso e Home

### 5.0 Onboarding

Na primeira abertura, um onboarding curto em etapas, com indicador de progresso:

1. **Boas-vindas**: o que o app faz.
2. **Você**: nome* e foto.
3. **Corpo**: data de nascimento, sexo, altura, peso atual*.
4. **Estilo de vida**: nível de atividade, biotipo, objetivo (perder / manter / ganhar peso), peso alvo.
5. **Metas diárias**: água, kcal consumidas (com a sugestão calculada a partir das etapas anteriores) e exercício (em kcal queimadas ou minutos ativos, à escolha).
6. **Lembretes**: oferece ligar o lembrete de água (a cada 2 h, das 8h às 22h). Se aceito, pede a permissão de notificação nesse momento.

\* Obrigatórios: **nome** e **peso**. Os outros campos podem ficar em branco e as etapas 4–6 podem ser puladas (ficam os valores padrão).

Tudo que é preenchido aqui vai para o perfil e pode ser editado depois pelo modal de Perfil. O onboarding não aparece de novo depois de concluído.

### 5.1 Layout e catálogo de cards

Grid de **2 colunas** com cards, montado pelo usuário a partir de um **catálogo**. A Home começa com um layout padrão, e o usuário adiciona, remove, reordena e redimensiona cards como quiser.

#### Tamanhos

| Tamanho | Onde | Regra |
|---|---|---|
| **Inteiro** | Home | largura toda; versão completa do card. Obrigatório. |
| **Metade** | Home | meia largura; versão compacta. Obrigatório. **Não precisa ter tudo** que a versão inteira tem: mostra o essencial e pode perder ações e detalhes. |
| **Mini** | painel rápido da sidebar (§9.0) | versão de consulta rápida. Opcional: só os cards que fazem sentido em mini oferecem. |

Card novo só entra no catálogo se passar no critério: **mostra algo útil num relance e/ou permite uma ação rápida**. Card que seria só um link para uma tela não entra.

#### Altura das linhas e scroll

A Home é pensada para **caber na tela sem rolar**. O número de linhas que cabem é calculado para cada aparelho:

- **Altura base da linha** (`baseRow`): valor de referência (proposta: 128dp), multiplicado pela escala de fonte do sistema (fonte em 130% → linhas mais altas).
- **Linhas por tela**: `N = floor(alturaDisponível ÷ baseRow)`, com mínimo de 3. Altura disponível = altura da tela menos barras do sistema e cabeçalho da Home.
- **Altura real da linha** = `alturaDisponível ÷ N` (descontando os espaçamentos). Assim as `N` linhas preenchem a tela exatamente, e cada linha fica entre `baseRow` e pouco mais que isso: os cards se ajustam, mas nunca encolhem abaixo da base nem esticam demais.
- Exemplo: num celular com ~700dp úteis, `N = 5` e cada linha tem ~140dp; num com ~520dp, `N = 4` e ~130dp.
- Todo card tem **1 linha de altura**. Um card inteiro ocupa uma linha; dois cards metade dividem uma linha.
- **Scroll natural**: com até `N` linhas a Home não rola; com mais, passa a rolar sozinha. Não existe opção ou botão para isso.
- **Linhas vazias**: com menos de `N` linhas, o espaço livre mostra uma sugestão discreta ("+ Adicionar card") que pode ser **dispensada**. Dispensada, não volta (pode ser reativada em Configurações → Home). Ter poucos cards é uma escolha válida.
- O conteúdo de cada card é **responsivo** à altura real da linha: com mais espaço, mostra mais detalhes; com menos, esconde os secundários.
- Rotação, mudança de fonte ou janela dividida recalculam `N`.
- Tablets: a definir.

#### Regras do grid

- Os cards seguem exatamente a ordem definida pelo usuário. Um card "metade" sem vizinho fica na metade esquerda e **o espaço à direita fica vazio**; o app não reorganiza nada sozinho.
- Dois cards "metade" na mesma linha têm **a mesma altura**: o mais baixo estica para igualar o mais alto.
- Alguns cards podem aparecer **mais de uma vez** com configurações diferentes (Checklist, Combinado).

#### Catálogo

| Card | Na Home por padrão | Inteiro | Metade | Mini |
|---|---|---|---|---|
| Dashboard | ✅ inteiro | progresso das 3 metas + mini-gráfico dos últimos 7 dias (barra por dia = média do % atingido das 3 metas, limitado a 100% por meta; hoje destacado) + atalho para detalhes | 3 anéis de progresso + % do dia | — |
| Água | ✅ inteiro | gota pequena à esquerda (com onda e transbordar), total/meta, barra e %, recipientes rápidos com ícone e "Desfazer último" | gota + total/meta + o primeiro recipiente rápido | total/meta + botão rápido |
| Alimentação | ✅ metade | kcal/meta com barra, últimas refeições do dia, atalho para registrar | kcal/meta + atalho para registrar | kcal/meta |
| Exercício | ✅ metade | progresso na unidade da meta, o outro valor menor, treinos do dia, atalho para registrar | progresso na unidade da meta + atalho | progresso |
| Peso | ✅ metade | visor da balança (fita com agulha vermelha) e peso atual à esquerda; à direita, mini-curva de 30 dias, variação na semana e o alvo | visor pequeno + peso + seta de tendência | peso + seta |
| Saldo calórico | | consumidas, queimadas, saldo e meta, com barra | saldo vs. meta | saldo |
| Macros do dia | | proteína, carboidrato e gordura em barras (só alimentos com macros contam; mostra a % de kcal coberta) | 3 anéis | — |
| Notas fixadas | | até 3 notas fixadas com prévia | a primeira nota fixada | — |
| Checklist | | itens pendentes da lista escolhida, marcáveis | nº de pendentes + o primeiro item, marcável | os 3 primeiros pendentes, marcáveis |
| Próximo lembrete | | os próximos 3 lembretes | o próximo lembrete | o próximo lembrete |
| Progresso | | nível, barra de XP, título, sequência atual, folgas guardadas | nível + sequência | nível + 🔥 sequência |
| Combinado | | até 3 resumos em colunas com anel (§5.3) | resumos em linhas compactas | ícone + valor de cada resumo |

- **Checklist**: a lista exibida é escolhida no modo de edição; podem existir vários cards de Checklist com listas diferentes.
- **Painel rápido padrão**: Checklist (se houver lista), Próximo lembrete, Progresso.
- Cards do M8 (Planejado hoje, Jejum): a definir no M8.

Layout padrão da Home: Dashboard, Água, Alimentação + Exercício lado a lado, Peso (metade, com o espaço ao lado livre).

**Tocar em qualquer parte de um card abre a tela da funcionalidade**; os botões dentro dele (recipientes, desfazer, registrar) fazem só a própria ação. Não há botão "Detalhes".

Um **botão flutuante "+" (FAB)** fica fixo no canto da Home e abre um menu: Água, Alimento, Exercício, Anotação, Peso. Ele leva para os mesmos fluxos dos cards, então dá para registrar algo mesmo que o card esteja oculto ou fora da tela. O FAB some no modo de edição do layout.

- Abre **em leque com mola**: o "+" gira para "×", os itens sobem um a um em pílulas com ícone e cor do módulo, e a Home escurece atrás.
- Cada módulo entra no FAB pelo seu contrato; só aparece o que existe (Anotação entra no M5).
- **Ações de 1 toque**: segurar uma pílula do leque abre, ao lado dela, as ações diretas daquele módulo, que registram sem abrir nada: na Água, os recipientes rápidos (+200 Copo, +300 Caneca); no Exercício, os modelos de treino (M1c); na Alimentação, repetir a última refeição (M1b). Cada módulo declara as suas no contrato; módulo sem ações não reage ao segurar.
- **No máximo 4 itens visíveis** no leque. Com mais módulos, o leque rola **de 4 em 4** (rolagem que encaixa em grupos de 4), mantendo o mesmo visual.
- Tocar num item abre o **registro rápido por cima da Home**, sem trocar de tela: o mesmo sheet de registro da tela do módulo (Água com recipientes e horário, Peso com a régua); Alimento e Exercício usam os registros antigos até serem refeitos.

### 5.2 Cards arrastáveis

- **Segurar um card e soltar** (sem mexer) abre o **editor daquele card**, como o redimensionamento dos widgets do Android: os outros cards escurecem, o card ganha uma moldura com uma **alça** na lateral livre e o selo **⊖** (ocultar). Arrastar a alça estica ou encolhe a moldura junto com o dedo; passar de um terço do caminho vibra e, ao soltar (ou num puxão rápido na direção certa), o card encaixa no outro tamanho com mola; senão, volta. As mudanças valem na hora; tocar fora ou voltar fecha.
- **Um jeito só de mexer num card**, dentro e fora do modo de edição: ocultar pelo selo ⊖, tamanho pela alça.
- **Segurar e arrastar** (no mesmo gesto, logo depois da vibração) entra no **modo de edição** da Home já levando o card junto do dedo.
- Um **lápis no topo da Home**, ao lado do menu, também entra no modo de edição.
- No modo de edição:
  - todos os cards **tremem de leve** e ganham o selo **⊖** (ocultar), pequeno, por fora do canto de cima;
  - **tocar num card seleciona** ele: para de tremer e ganha a mesma moldura com a alça do editor de card; a troca de tamanho é animada e mostra a outra versão na hora;
  - a sidebar não abre pelo gesto de deslizar, para não atrapalhar o arrasto;
  - arrastar um card (de qualquer ponto) move sua posição; os outros se reorganizam com animação;
  - uma barra no topo tem **"+ Adicionar card"** e **Concluir**;
  - "+ Adicionar card" abre o **catálogo com prévia ao vivo**: um bottom sheet com cada card renderizado com os dados reais e um seletor Inteiro/Metade; tocar adiciona no fim da Home no tamanho escolhido. Cards já na Home aparecem marcados. Inclui a criação de um card Combinado (M4);
  - cards configuráveis (Checklist, Combinado) mostram um botão de configurar;
  - botão **Concluir** sai do modo (tocar fora ou voltar também sai).
- Vibração curta (haptic) ao pegar e soltar um card.
- O layout é salvo no DataStore ao sair do modo de edição.
- Em Configurações: **Restaurar layout padrão** (M3; até lá não há como restaurar).
- Fora do modo de edição, cards se comportam normalmente (toques e botões funcionam).

### 5.3 Resumos e card Combinado

#### Resumo

Cada funcionalidade com card define um **resumo**: só os dados, sem layout.

| Campo | Exemplo (Água) |
|---|---|
| ícone (`iconKey`) e cor do módulo | gota, azul |
| valor principal | `1,4 L` |
| meta ou valor secundário (opcional) | `de 2 L` |
| progresso 0–1 (opcional) | `0,7` |
| descrição falada (TalkBack) | "Água: 1,4 de 2 litros, 70 por cento" |
| ação rápida (opcional) | `+300 ml` |

- Têm resumo: Água, Alimentação, Exercício, Peso, Saldo calórico, Macros do dia, Checklist (pendentes), Próximo lembrete, Progresso.
- O Dashboard usa os mesmos resumos para as 3 metas do dia.
- Os resumos são criados junto de cada funcionalidade (M1/M2), para estarem prontos quando o Combinado chegar (M4).

#### Card Combinado

Junta **até 3 resumos** escolhidos pelo usuário num card só, com visual caprichado e não uma lista de informações soltas. Pode haver vários combinados.

**Inteiro: colunas com anel.**

```
╭─ Meu resumo ─────────────────────────╮
│    ╭───╮     │    ╭───╮     │   ╭───╮  │
│   │ 💧 │    │   │ 🍎 │    │  │ 🔥 │ │
│    ╰───╯     │    ╰───╯     │   ╰───╯  │
│    1,4 L     │    1.250     │    320   │
│   de 2 L     │  de 2.000    │  de 500  │
╰───────────────────────────────────────╯
```
- Uma coluna por resumo, com a mesma largura e divisores finos entre elas.
- Cada coluna: **anel de progresso** na cor do módulo com o **ícone no centro**; embaixo, o valor em destaque e a meta em texto secundário.
- Resumo sem progresso (ex.: Peso sem peso alvo): anel completo em tom suave da cor do módulo, e a meta é trocada pelo valor secundário (ex.: `↓ 0,3 kg`).
- Com 2 resumos, 2 colunas; com 1, a coluna fica centralizada.
- O anel anima ao mudar o valor (respeitando "Remover animações").

**Metade: linhas compactas.**

```
╭─ Meu resumo ──────╮
│ (💧) 1,4 L  ▓▓▓░ │
│ (🍎) 1.250  ▓▓░░ │
│ (🔥) 320    ▓░░░ │
╰───────────────────╯
```
- Uma linha por resumo: ícone num círculo na cor do módulo, valor e mini-barra de progresso. Sem a meta.

**Mini**: ícone + valor de cada resumo, lado a lado.

**Comportamento:**
- **Nome opcional**: com nome, aparece como título; sem nome, as colunas ganham a altura do título.
- Tocar numa coluna ou linha abre a tela daquela funcionalidade. O Combinado não mostra botões de ação rápida, para manter o visual limpo.
- Criado e editado no modo de edição da Home: escolher os resumos, a ordem e o nome.
- TalkBack lê o card como a sequência das descrições faladas dos resumos.

## 6. Água

- Botões rápidos de adicionar (padrão **+200, +300, +500 ml**), configuráveis em Configurações.
- Quantidade personalizada (campo numérico).
- **Sem limite na meta**: dá para beber mais do que a meta; a barra fica cheia e mostra o excedente (ex.: `2.300 / 2.000 ml`).
- Os botões negativos (−200, −300, −500) saem. Para corrigir: **Desfazer último** no card, ou editar/remover registros na tela de detalhes.
- **Tela de detalhes**: lista dos registros do dia com horário, edição e remoção; botão **"Desfazer último"** (como no card); navegação entre dias; gráfico dos últimos 7/30 dias.
- **Meta diária editável na própria tela** (tocar na meta abre um sheet), com sugestão de 35 ml por kg do peso atual. O Dashboard continua editando também; os dois gravam no mesmo lugar.
- **Recipientes nomeados**: atalhos com nome, volume e ícone (ex.: "Copo 200 ml", "Garrafa 500 ml", "Squeeze 750 ml"), gerenciados **na própria tela de Água**, sem engrenagem:
  - uma fileira de botões de recipiente: **tocar registra** na hora, **segurar abre a edição** daquele recipiente, e o último botão **"+"** cria um novo;
  - **"Editar"** ao lado da fileira transforma ela na grade **"Meus recipientes"**, com lápis em cada um, "+ Novo" e apagar; "Pronto" volta ao normal;
  - cada recipiente tem um **ícone escolhido pelo usuário** num conjunto desenhado para o Wellio (copo, caneca, xícara, garrafa, squeeze, taça, garrafa térmica, galão); um recipiente novo já vem com um ícone sugerido pelo volume. Nada de emoji (§2.3);
  - os marcados como rápidos (até 3) também viram botões no card da Home.

## 6.1 Peso

- Tela própria, com a balança de banheiro no topo (§2.9).
- **Peso alvo** definido na própria tela, **dentro da balança**: com alvo, uma linha compacta com quanto falta, barrinha de progresso (desde o peso do momento em que o alvo foi definido) e data estimada, que abre a edição ao tocar; sem alvo, um botão "Definir alvo". Linha tracejada na curva. No M2 o peso alvo aparece também no perfil (mesmo dado).
- **Curva de evolução** por período (30 dias, 90 dias, tudo); segurar o dedo e arrastar mostra o peso e o dia de cada pesagem.
- **Resumo do período**: menor, maior e média.
- **Ritmo** médio em kg por semana (com pelo menos 7 dias entre a primeira e a última pesagem do período) e, com peso alvo, a **data estimada** para chegar lá, se o ritmo for na direção certa.

## 7. Alimentação

### 7.1 Registro

Abre em bottom sheet de tela cheia (§2.2).

- Busca por nome + filtro por categoria (como hoje).
- Seções **Favoritos** e **Recentes** no topo da busca.
- Ao escolher o alimento: escolher a **medida** (gramas ou uma medida caseira, ex.: "1 concha média") e a **quantidade** (aceita vírgula), com prévia da kcal (e macros, se houver) antes de confirmar.
- A última medida usada para cada alimento fica pré-selecionada na próxima vez.
- **Refeição** opcional: café da manhã, almoço, lanche, jantar, ceia. Sugerida automaticamente pelo horário.
- **Repetir refeição**: copiar todos os itens de uma refeição de outro dia (ex.: "mesmo café da manhã de ontem"), com revisão antes de confirmar. Os itens copiados recalculam kcal com os valores atuais do alimento.

### 7.2 Cadastro de alimentos próprios

- Campos: nome, categoria, ícone, e os valores nutricionais **por 100 g** ou **por porção** (o app converte para 100 g); proteína/carboidrato/gordura opcionais; medidas caseiras opcionais (nome + gramas).
- Alimentos próprios podem ser editados e removidos; os da base, só favoritados.
- Remover um alimento não apaga os registros antigos (a kcal já está gravada).

### 7.3 Acima da meta

Quando as kcal consumidas passam da faixa da meta (+10%), o card de Alimentação:
- muda a barra para uma cor de alerta neutra (âmbar, nunca vermelho);
- mostra o excedente (ex.: `+320 kcal`);
- exibe uma mensagem leve, sorteada (ver `TEXTOS.md`). Sem toast e sem notificação.

### 7.4 Tela de detalhes (diário)

- Registros do dia agrupados por refeição, com subtotal; edição de quantidade e remoção.
- Total do dia vs. meta; macros do dia se houver dados.
- Navegação entre dias.

### 7.5 Base de alimentos

- **TACO** (Tabela Brasileira de Composição de Alimentos, NEPA/Unicamp): todos os alimentos da tabela, com kcal e macros por 100 g. Nomes simplificados para busca (ex.: "Arroz, tipo 1, cozido" fica pesquisável por "arroz cozido").
- **Extras**: alimentos que a TACO não cobre (pizza, hambúrguer, açaí na tigela, refrigerante zero...), mantidos a partir da lista atual do `FoodRepository`, com valores revisados.
- **Medidas caseiras curadas**: os alimentos mais comuns ganham medidas com gramas (ex.: 1 concha média de feijão, 1 pão francês, 1 colher de sopa de arroz, 1 fatia de queijo). Todo alimento aceita gramas.
- **Marcas e industrializados**: pelo leitor de código de barras (§13.1).
- A tela "Sobre" cita a TACO como fonte.

## 8. Exercício

- Registro em bottom sheet de tela cheia (§2.2).
- Escolha por categoria + busca + favoritos/recentes.
- Duração em minutos; kcal estimada = `METs × peso (kg) × horas`, usando o **peso atual do perfil** (hoje está fixo em 70 kg).
- Prévia da kcal antes de salvar; o usuário pode **sobrescrever** a kcal manualmente (ex.: valor do relógio).
- Se o perfil não tiver peso (onboarding pulado), pedir o peso no primeiro registro.
- **Base de exercícios** (§8.1).
- **Cadastro de exercícios próprios**: nome, categoria, METs (com ajuda: leve ≈ 3, moderado ≈ 5, intenso ≈ 8) ou kcal/minuto, ícone.
- **Tela de detalhes**: treinos do dia, edição e remoção, navegação entre dias.

### 8.1 Base de exercícios

- Fonte: **Compendium of Physical Activities** (versão adulta de 2024), referência oficial dos valores de METs.
- Uma seleção curada de **~100 atividades**, traduzidas e agrupadas nas categorias do app (Cardio, Musculação, Lutas, Esportes, Aquáticos, Zen/Dança, Outros). Cada uma guarda o código do Compendium (`compendiumCode`).
- Substitui a lista atual do `ExerciseRepository`, revisando os METs e os ícones.
- A tela "Sobre" cita o Compendium como fonte.

## 9. Sidebar

### 9.0 Layout

Baseado no esboço em papel do usuário.

```
┌────────── sidebar (~60%) ──────────┬── painel rápido (~40%) ──┐
│            ( FOTO )                │ ┌──────────────────────┐ │
│                                    │ │ ☐ comprar aveia      │ │
│   Nome            Nv. 14 · Título  │ │ ☐ treino de perna    │ │
│ ┌────────────────────────────────┐ │ └──────────────────────┘ │
│ │ 72,4 kg · 1,78 m · 27 anos ... │ │ ┌──────────────────────┐ │
│ └────────────────────────────────┘ │ │ 🔥 12 dias seguidos  │ │
│                                    │ └──────────────────────┘ │
│   (itens do menu, §2.2)            │ ┌──────────────────────┐ │
│                                    │ │ ⏰ 18:00 academia     │ │
│                                    │ └──────────────────────┘ │
│                                    │   (Home embaçada atrás)  │
└────────────────────────────────────┴──────────────────────────┘
```

- **Foto**: grande, circular; tocar abre o modal de Perfil (§9.1).
- **Nome + rank**: nome do usuário e, ao lado, o **nível** e o **título** escolhido (§9.5).
- **Caixa de informações**: resumo do perfil (peso, altura, idade, objetivo); tocar abre o modal de Perfil.
- **Menu**: itens da navegação (§2.2).
- A sidebar é mais estreita que o padrão do Material 3 (~60% da largura), para sobrar espaço ao painel rápido.

#### Painel rápido

A Home fica **embaçada** atrás da sidebar, e à direita aparece uma coluna com os cards em **tamanho mini** (§5.1). O painel usa o mesmo catálogo da Home: só aparecem como opção os cards que têm versão mini.

- Quais mini-cards aparecem, e em que ordem, é configurável (como os cards da Home).
- Mini-cards vazios (ex.: sem jejum ativo) ficam ocultos.
- Tocar num mini-card abre a tela correspondente; tocar no espaço vazio do embaçado, deslizar para a esquerda ou voltar fecha a sidebar.
- Em telas estreitas (< 360dp de largura), o painel rápido não aparece.

### 9.1 Perfil

Editado num **modal (bottom sheet)** que abre ao tocar no cabeçalho da sidebar, sem tela própria. O gráfico de evolução do peso fica em Estatísticas (§9.4); o modal mostra o peso atual e um atalho "ver evolução".

- **Foto**: escolhida pelo Photo Picker e copiada para o armazenamento interno do app; recorte circular.
- **Nome**.
- **Peso atual**: cada alteração cria um `weight_entry`, gerando o histórico mostrado em Estatísticas.
- Opcionais: data de nascimento, sexo, altura, objetivo (perder / manter / ganhar peso), peso alvo e:
  - **nível de atividade**: sedentário, levemente ativo, moderado, muito ativo, atleta. Entra no cálculo da meta calórica;
  - **biotipo**: ectomorfo, mesomorfo, endomorfo. Só informativo, não entra em cálculos.
- Derivados exibidos: idade, IMC, e **meta calórica sugerida** pela fórmula de Mifflin-St Jeor × fator do nível de atividade (1,2 / 1,375 / 1,55 / 1,725 / 1,9), ajustada pelo objetivo (−500 kcal para perder, +300 para ganhar). O usuário pode aceitar a sugestão como meta diária. Sem sexo, idade ou altura, a sugestão não aparece.
- O cabeçalho da sidebar mostra foto, nome e um resumo (ex.: peso atual e sequência de dias).

### 9.2 Metas

Dois níveis:

1. **Metas diárias** (as que aparecem no Dashboard): água, kcal consumidas e exercício. Editadas pelos botões "+" do Dashboard ou nesta tela.
   - A meta de exercício pode ser em **kcal queimadas** ou **minutos ativos**, à escolha do usuário. A kcal de exercício é uma estimativa por METs; minutos são exatos. Trocar a unidade vale dali em diante, sem reescrever o histórico de metas batidas.
   - Cada uma tem uma **meta semanal**, que por padrão é a diária × 7. O usuário pode desligar o cálculo automático e definir um valor próprio; a diária continua independente.
2. **Metas de período**, criadas pelo usuário para motivação:
   - **Período**: semanal, mensal, anual ou datas personalizadas.
   - **Métrica**:

     | Métrica | Exemplo |
     |---|---|
     | Água total (L) | Beber 60 L em outubro |
     | Dias batendo a meta de água | 25 dias no mês |
     | Nº de treinos | 12 treinos no mês |
     | Minutos de exercício | 150 min por semana |
     | kcal queimadas | 50.000 kcal no ano |
     | Dias dentro da meta calórica (±10%) | 5 dias na semana |
     | Peso alvo | Chegar a 75 kg até dezembro |

   - **Recorrência** opcional: meta semanal/mensal que se renova sozinha.
   - Cada meta mostra progresso, dias restantes e **projeção** ("no ritmo atual você chega a 52 L").
   - Ao concluir, toast de parabenização (§9.6); metas vencidas ficam no histórico como concluídas ou não.


### 9.3 Configurações

- **Aparência**: tema (sistema / claro / escuro). Sem cores dinâmicas (Material You): o app usa sua própria paleta (§2.5).
- **Notificações** (cada uma liga/desliga e tem horário):
  - lembrete de água: intervalo (ex.: a cada 2 h) dentro de uma janela de horário (ex.: 8h–22h); não dispara se a meta do dia já foi batida;
  - lembrete de refeição (por refeição);
  - lembrete de treino (dias da semana + horário);
  - resumo do dia (à noite);
  - conquistas e metas concluídas.
- **Sons e vibração**: som ao registrar, vibração.
- **Registro**: botões rápidos de água, início da semana, unidades (ml/L, kg).
- **Home**: restaurar layout padrão.
- **Dados**: exportar e importar backup (JSON); exportar CSV (um arquivo por tipo de registro: água, alimentação, exercício, peso); apagar todos os dados (com confirmação).
  - Além disso, o **backup automático do Android** fica ativo (banco Room, DataStore e foto de perfil), configurado em `backup_rules.xml` / `data_extraction_rules.xml`. Ele restaura os dados ao reinstalar ou trocar de celular com a mesma conta Google.
  - A importação de JSON pergunta se deve **substituir** ou **mesclar** os dados atuais (mesclar usa os UUIDs para evitar duplicatas).
- **Sobre**: versão.

Pedir a permissão `POST_NOTIFICATIONS` (Android 13+) só quando o usuário ligar a primeira notificação.

### 9.4 Estatísticas

Uma visão mais ampla que o Dashboard. O Dashboard mostra **hoje e a semana**; Estatísticas cobre **qualquer período**. Histórico e estatísticas são uma tela só, com duas abas.

#### Aba "Dia a dia" (histórico)

- Calendário mensal; cada dia pintado conforme as metas batidas.
- Tocar num dia mostra tudo o que foi feito nele: água, refeições, treinos, peso, entrada de diário. Dali dá para editar ou registrar algo naquele dia.

#### Aba "Visão geral"

- Intervalo: 7 dias, 30 dias, 3 meses, 12 meses, tudo.
- Por métrica (água, kcal consumidas, kcal queimadas, minutos de exercício, peso):
  - gráfico de evolução;
  - média, total, melhor dia (recorde);
  - % de dias com meta batida.
- **Calendário de consistência** (heatmap estilo GitHub): intensidade = quantas metas diárias foram batidas.
- Distribuição: exercícios por categoria; kcal por refeição e por categoria de alimento.
- Saldo calórico ao longo do tempo.
- Recordes pessoais: maior sequência, mais água num dia, treino mais longo, etc.

### 9.5 Conquistas e nível

> Nomes, mensagens de cada nível e títulos estão em [`TEXTOS.md`](TEXTOS.md).

#### Trilhas e níveis

A maioria das conquistas é uma **trilha** com **7 níveis**, para dar sensação de progressão:

| Nível | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
|---|---|---|---|---|---|---|---|
| Nome | Bronze | Prata | Ouro | Platina | Diamante | Mestre | Deus |
| XP | 10 | 25 | 50 | 100 | 200 | 400 | 800 |

- Cada nível tem uma **mensagem própria** ao ser desbloqueado.
- O nível máximo dá um **título** (ex.: "O Deus da Bebedeira" para a trilha Caudal), que pode ser exibido no cabeçalho da sidebar abaixo do nome.
- Também existem **marcos**: conquistas únicas, sem níveis, para momentos especiais e secretos.

#### Experiência e nível do usuário

- O XP vem principalmente das conquistas: cada nível de trilha e cada marco dá XP.
- O uso diário também dá um pouco de XP, para o nível andar aos poucos:

  | Ação | XP |
  |---|---|
  | Bater uma meta diária (água, kcal consumidas, kcal queimadas) | +5 cada |
  | Dia perfeito (as 3 metas no mesmo dia) | +10 de bônus |
  | Concluir uma meta de período | +20 |

  O máximo por dia vindo de metas diárias é 25 XP. XP de ações é calculado a partir dos registros; se um registro for apagado e a meta deixar de estar batida, o XP daquele dia é recalculado. O XP de conquistas nunca é retirado.
- O nível do usuário aparece no cabeçalho da sidebar, com uma barra de progresso até o próximo nível.
- Curva proposta: XP para chegar ao nível `n` = `100 × n^1,5` (arredondado). A curva fica em código para ser ajustada fácil.
- Subir de nível mostra um toast de parabenização (§9.6).

#### Folgas de sequência

Protegem as sequências de um dia perdido (doença, viagem, esquecimento):

- **Ganho**: 1 folga a cada 7 dias seguidos de Presença. Guarda no máximo **2**.
- **Uso**: automático. No fechamento de um dia (§9.6), se alguma sequência diária seria quebrada e há folga guardada, **uma folga cobre todas** as sequências quebradas naquele dia.
- Valem para as sequências **diárias**: Presença, Fonte Constante, Fiel da Balança. **Não** valem para Ritmo, que é semanal e já é flexível.
- Um dia coberto por folga **mantém** a sequência, mas **não conta** como dia cumprido: não soma em trilhas de total (Copo Cheio, Dia Perfeito...) nem dá XP.
- Dois dias perdidos seguidos com só uma folga: a folga cobre o primeiro e a sequência quebra no segundo.
- O usuário vê as folgas guardadas no card Progresso e no cabeçalho de Conquistas. Ao usar uma, aparece um toast no dia seguinte ("Ontem você perdeu a meta de água, mas uma folga salvou sua sequência de 23 dias").

#### Comportamento

- A tela lista as trilhas com o nível atual, a mensagem do último nível e o progresso até o próximo (ex.: `63 / 100 L`).
- Marcos secretos aparecem como "???" até serem desbloqueados.
- Ao desbloquear: toast de parabenização (§9.6) + notificação, se ativada.
- O motor reavalia as regras quando um registro é criado, editado ou removido, e também numa checagem diária. Uma conquista desbloqueada **nunca é perdida**, mesmo que o registro que a gerou seja apagado.

#### Trilhas (limites dos níveis 1 → 7)

**Água**
| Trilha | Métrica | Níveis |
|---|---|---|
| Caudal | litros totais | 10 · 50 · 100 · 250 · 500 · 1.000 · 2.500 |
| Fonte Constante | dias seguidos batendo a meta | 3 · 7 · 14 · 30 · 60 · 100 · 365 |
| Copo Cheio | dias com meta batida (total) | 1 · 10 · 30 · 75 · 150 · 300 · 500 |

Não haverá trilha de "mais água num dia": premiar volume diário alto pode incentivar excesso.

**Alimentação**
| Trilha | Métrica | Níveis |
|---|---|---|
| Diário de Bordo | dias com algum alimento registrado | 1 · 7 · 30 · 60 · 120 · 240 · 365 |
| Fiel da Balança | dias seguidos dentro da meta calórica (±10%) | 3 · 7 · 14 · 30 · 60 · 100 · 180 |
| Feira Livre | dias com fruta ou vegetal registrado | 5 · 15 · 30 · 60 · 120 · 240 · 365 |
| Despensa | alimentos próprios cadastrados | 1 · 5 · 10 · 25 · 50 · 75 · 100 |

**Exercício**
| Trilha | Métrica | Níveis |
|---|---|---|
| Rotina de Ferro | treinos registrados | 1 · 10 · 25 · 50 · 100 · 250 · 500 |
| Fornalha | kcal queimadas acumuladas | 1k · 5k · 10k · 25k · 50k · 100k · 250k |
| Tempo de Suor | minutos acumulados | 60 · 300 · 1.000 · 2.500 · 5.000 · 10.000 · 25.000 |
| Ritmo | semanas seguidas com 3+ treinos | 1 · 2 · 4 · 8 · 12 · 26 · 52 |

**Constância**
| Trilha | Métrica | Níveis |
|---|---|---|
| Presença | dias seguidos usando o app | 3 · 7 · 14 · 30 · 100 · 200 · 365 |
| Dia Perfeito | dias batendo as 3 metas diárias (total) | 1 · 7 · 30 · 60 · 100 · 200 · 365 |

**Metas e anotações**
| Trilha | Métrica | Níveis |
|---|---|---|
| Estrategista | metas de período concluídas | 1 · 3 · 5 · 10 · 20 · 35 · 50 |
| Escriba | anotações criadas | 1 · 10 · 25 · 50 · 100 · 250 · 500 |
| Riscando a Lista | itens de lista concluídos | 1 · 25 · 100 · 250 · 500 · 1.000 · 2.500 |
| Livro de Receitas | receitas salvas | 1 · 5 · 10 · 20 · 35 · 50 · 100 |

#### Marcos

| Marco | Regra | XP |
|---|---|---|
| Quem Sou Eu | completar o perfil (foto, nome, peso, altura) | 50 |
| Eclético | treinar nas 7 categorias de exercício | 100 |
| Semana Perfeita | bater as 3 metas diárias por 7 dias seguidos | 150 |
| Lista Zerada | concluir todos os itens de uma lista | 25 |
| Ano Vitorioso | concluir uma meta anual | 300 |
| Objetivo Alcançado | atingir o peso alvo | 300 |
| Madrugador (secreto) | registrar água antes das 6h | 25 |
| Coruja (secreto) | registrar um treino depois das 23h | 25 |

### 9.6 Parabenizações

Sempre que algo bom acontece, o app parabeniza **sem interromper**:

- Formato: um **toast**, um banner pequeno no topo da tela que some sozinho em ~3 s. Pode ser dispensado com um deslize. **Nunca** um modal ou algo que bloqueie a tela.
- Quando aparece:
  - bater uma meta diária (água, alimentação, exercício);
  - dia perfeito (as 3 metas);
  - bater a meta semanal;
  - concluir uma meta de período;
  - desbloquear um nível de conquista ou marco (mostra o nome, o nível e a mensagem);
  - subir de nível de usuário.
- Cada evento tem **várias mensagens** sorteadas, para não ficar repetitivo (ex.: "Meta de água batida! 💧", "Hidratação em dia, boa!").
- Vários eventos ao mesmo tempo viram **um toast só** (ex.: "Meta de água batida + Caudal · Prata").
- **Alimentação e Dia perfeito são avaliados ao fechar o dia**, porque a meta de comida é uma faixa (±10%) e pode deixar de valer até o jantar. O toast aparece na primeira abertura do dia seguinte ("Ontem: ..."). O XP e as conquistas que dependem deles também são creditados no fechamento. Água e exercício parabenizam na hora.
- Cada evento parabeniza **uma vez só**: se a meta deixar de estar batida (registro apagado) e for batida de novo no mesmo dia, não repete.
- Som e vibração curtos, respeitando as Configurações.
- Com o app fechado (ex.: treino importado pelo Health Connect), a parabenização vira uma notificação, se esse tipo de notificação estiver ligado.

**Aniversário**: no dia do aniversário (data de nascimento do perfil), algo especial e leve:
- saudação personalizada no topo da Home e confetes na primeira abertura do dia (sem confetes com "Remover animações");
- marco secreto "Mais um Ano" (+100 XP), que pode ser ganho de novo a cada ano;
- o cabeçalho da sidebar ganha um enfeite durante o dia.

Sem data de nascimento no perfil, nada acontece.

## 10. Anotações

Um espaço geral, acessível pela sidebar (e pelo card de anotações fixadas na Home).

### 10.1 Tipos

| Tipo | Conteúdo |
|---|---|
| Nota | título + texto com formatação básica |
| Lista (to-do) | título + itens com checkbox, reordenáveis por arrastar |
| Receita | título, porções, ingredientes (lista), modo de preparo, kcal por porção opcional |
| Lembrete | título + data/hora (dispara notificação), repetição opcional (diária/semanal) |
| Diário | **uma entrada por dia**, com formatação básica; abrir o diário de um dia que já tem entrada continua a mesma |

O tipo define o editor, mas todas compartilham a mesma tela de listagem.

**Formatação**: negrito, itálico, títulos, listas com marcadores e checkboxes no meio do texto, aplicados por uma barra de ferramentas acima do teclado. O conteúdo é guardado como **Markdown** (`note.body`), o que facilita exportar e sincronizar depois.

### 10.2 Funcionalidades

- Listagem com busca, filtro por tipo e por **tags**.
- **Fixar** notas no topo (e no card da Home).
- Cor opcional por nota.
- Arquivar e remover (com desfazer).
- Itens de lista concluídos vão para o final, com opção de ocultar.
- Qualquer nota pode ganhar um lembrete.

## 11. Roadmap

| Fase | Entrega |
|---|---|
| **M0 — Base** | Atualizar dependências; corrigir pacotes; Room + DataStore + `AppContainer`; Navigation + drawer vazio; Home montada com os cards atuais; abstração `iconKey`; persistência de água/comida/treino |
| **M1a — Água, Exercício e Peso** | Telas de Água, Exercício e Peso (simples); editar (toque abre bottom sheet), apagar (deslizar ou lixeira) e desfazer; navegação entre dias (setas, deslizar e calendário, nunca depois de hoje); recipientes nomeados gerenciados na tela de Água; base de exercícios do Compendium curada (lista revisada pelo usuário antes de entrar); peso mais recente no cálculo de kcal |
| **M1.5 — Home e módulos** | Contrato de módulo (tela, item da sidebar, cards, resumo e ação do FAB declarados num lugar só); resumos de Água e Peso; cards inteiro/metade de Água (gota) e Peso (visor + mini-curva); grid com linhas calculadas por aparelho; modo de edição (selos, arrastar, alternar tamanho, ocultar, catálogo com prévia ao vivo); layout salvo; FAB em leque; Dashboard com dados reais (3 metas do dia + 7 dias) e versão metade. Cards antigos de Alimentação e Exercício só adaptados aos tamanhos até serem refeitos. Antecipa parte do M4 para que cada módulo novo chegue completo (tela + card + resumo + FAB) |
| **M1b — Alimentação** | Tela de Alimentação com refeições (sugeridas pelo horário: café até 10h, almoço 10–15h, lanche 15–18h, jantar 18–22h, ceia depois das 22h); TACO + extras; medidas caseiras; favoritos/recentes; cadastro de alimentos próprios; repetir refeição; acima da meta |
| **M1c — Extras do registro** | Leitor de código de barras (Open Food Facts); modelos de treino |
| **M1 — Registros completos (visão geral)** | Telas de detalhe com editar/remover/desfazer; navegação entre dias; favoritos/recentes; bases TACO + extras e Compendium curado; medidas caseiras; resumos de cada funcionalidade; cadastro de alimentos e exercícios próprios; **leitor de código de barras**; **modelos de treino**; peso do perfil no cálculo; correções da §12 |
| **M2 — Perfil e Metas** | Modal de perfil (foto, nome, peso); metas diárias, semanais e de período; Dashboard com dados reais; **widget na tela inicial** |
| **M3 — Configurações e integrações** | Tema; notificações e lembretes; sons; backup/exportação; **Health Connect** |
| **M4 — Home personalizável** | Catálogo de cards (inteiro/metade/mini), grid arrastável, painel rápido configurável, card Combinado, restaurar padrão |
| **M5 — Anotações** | Notas, listas, receitas, lembretes, diário, tags, fixadas |
| **M6 — Estatísticas e Conquistas** | Gráficos, heatmap, recordes; motor de conquistas, trilhas, marcos, XP, nível e folgas de sequência |
| **M7 — Ícones** | Substituir emojis por SVGs (pode andar em paralelo a partir do M0) |
| **M8 — Planejamento e cozinha** | Planejamento da semana, lista de compras, jejum intermitente (§13.2) |
| **Futuro** | Login, sincronização em nuvem, itens da §13.3 |

### 11.1 Futuro: rodapé da sidebar

Seção no fim da sidebar, do esboço original:
- **Conheça o autor**: tela sobre quem fez o app.
- **Avalie!**: 5 estrelas que levam à avaliação na Play Store (quando o app for publicado).
- **Contato**: atalhos para GitHub, LinkedIn e e-mail.

## 12. Correções conhecidas no código atual

- [x] `MainActivity` não mostra a Home; `HomeViewModel` não é usado.
- [x] Nenhum dado é persistido.
- [x] `addWater` limita o total à meta (`coerceIn(0, waterGoalMl)`).
- [x] Meta de água duplicada em `waterGoalMl` e `_goals`.
- [ ] Peso fixo em 70 kg no cálculo de exercício.
- [ ] Quantidade de comida não aceita vírgula e falha em silêncio com entrada inválida.
- [ ] Barras do gráfico do Dashboard são fixas.
- [x] Barra "semanal" do Dashboard usa o valor do dia.
- [x] "DETALHES" do card de água sem ação.
- [ ] `FoodCard`/`TrainCard` com valores padrão fictícios (1500/1200, 2000/2500).
- [ ] Título do `ExerciseDialog` usa o verde da alimentação; cor de Exercício no `GoalType` é cinza-claro e some no fundo.
- [x] Pacote `com.projeto.wellio.components` em pasta `ui/components`.
- [x] Dependências defasadas (core-ktx 1.10, lifecycle 2.6, Compose BOM 2024.09).

## 13. Integrações e extras

### 13.1 Aprovados (entram no núcleo)

- **Widget na tela inicial** (Glance), no M2: progresso de água do dia e botão "+300 ml"; versão maior com as 3 metas.
- **Health Connect**, no M3: importar treinos de relógio e celular; exportar água e alimentação.
  - A importação é **automática**: sincroniza periodicamente (WorkManager) e ao abrir o app. Uma notificação discreta avisa quantos treinos entraram.
  - Treinos importados ficam marcados com a origem (`source = health_connect`, `externalId`) e não são duplicados; podem ser editados ou apagados. Um treino apagado não volta na próxima sincronização.
  - **kcal**: usa a medida pelo dispositivo quando existir; sem ela, calcula por METs com o peso do perfil.
  - O tipo de exercício do Health Connect é mapeado para um `exercise_type` da base; sem correspondência, cai em "Outros".
  - Passos ficam para depois (§13.3).
- **Leitor de código de barras**, no M1: busca na base Open Food Facts (precisa de internet só nesse momento) e pré-preenche o cadastro do alimento, que fica salvo localmente.
- **Modelos de treino**, no M1: salvar um conjunto de exercícios com durações (ex.: "Treino A: musculação 50 min + esteira 15 min") e registrar tudo com um toque, podendo ajustar antes de confirmar.

### 13.2 Planejamento e cozinha (aprovado, M8)

Depende de Anotações (receitas) e dos registros de alimentação e exercício.

- **Planejamento da semana**
  - Calendário semanal com **treinos** (exercício avulso ou modelo de treino) e **refeições** (alimentos, receitas ou uma refeição copiada) planejados por dia e horário.
  - Na Home e nas telas de registro, o que está planejado para hoje aparece como sugestão: um toque registra o item como feito (com ajuste antes de confirmar).
  - Itens planejados e não feitos ficam marcados como perdidos no fim do dia; não geram registro.
  - Copiar a semana anterior para a atual.
  - Lembretes opcionais por item planejado.
- **Lista de compras**
  - Novo tipo de anotação: "Lista de compras" (uma lista com quantidades).
  - Gerada a partir das receitas escolhidas (e das receitas planejadas na semana), somando ingredientes iguais.
  - Para somar, os ingredientes das receitas passam a ter quantidade e unidade opcionais (ex.: `200 g arroz`); sem quantidade, o ingrediente entra só pelo nome.
  - Itens podem ser marcados durante a compra; itens marcados vão para o final.
- **Jejum intermitente**
  - Protocolos prontos (12:12, 14:10, 16:8, 18:6, 20:4) ou personalizado.
  - Timer iniciado manualmente ou pela última refeição registrada; mostra o tempo de jejum e o fim previsto.
  - Notificação ao terminar a janela de jejum.
  - Histórico de jejuns concluídos; entra em Estatísticas e pode ganhar uma trilha de conquistas.

Modelo de dados adicional: `plan_item` (id, date, time?, kind: exercise/template/food/recipe, refId, quantity?, status: planned/done/missed, entryId?), `fast_session` (id, startAt, endAt?, targetHours, status) e `recipe_ingredient` (id, noteId, name, quantity?, unit?, position).

### 13.3 Para depois

1. **Ações rápidas** no ícone do app (segurar o ícone → "Beber água", "Registrar treino") e um tile nas configurações rápidas do Android.
2. **Treino de força detalhado**: séries, repetições e carga por exercício, com evolução de carga e recordes pessoais.
3. **Cronômetro de treino e descanso**: timer entre séries, e o treino é registrado com a duração cronometrada.
4. **Passos como métrica** (via Health Connect), com meta diária e card na Home.
5. **Fotos de progresso**: fotos do corpo com data e comparação lado a lado, privadas no app.
6. **Medidas corporais** (cintura, braço, % de gordura) junto do histórico de peso.
7. **Resumo semanal**: tela ou notificação no domingo comparando com a semana anterior.
8. **Bloqueio por biometria** para abrir o app.
9. **Tile para Wear OS** com água e progresso do dia.
10. **Modo canhoto (espelhamento dos cards)**: opção em Configurações → Home que espelha o layout horizontalmente: o card "metade" sem vizinho passa para a direita, os elementos internos dos cards trocam de lado (ex.: a gota e o visor da balança à direita, botões de ação à esquerda) e o FAB vai para o canto esquerdo, para ficar ao alcance do polegar esquerdo.

## 14. Decisões tomadas

- **Home**: grid de 2 colunas; **todo card tem versão inteira e metade**, à escolha do usuário; espaço vazio é mantido e cards da mesma linha têm a mesma altura (§5.1).
- **Água**: botões negativos substituídos por "Desfazer último" + edição na tela de detalhes (§6).
- **Macros**: opcionais no cadastro, exibidas quando existem, sem metas próprias (§7).
- **Perfil**: modal a partir do cabeçalho da sidebar; histórico de peso em Estatísticas (§9.1).
- **Meta semanal**: diária × 7 por padrão, com opção de definir à parte (§9.2).
- **Conquistas**: trilhas com 7 níveis, mensagem por nível, título no nível máximo, XP e nível do usuário (§9.5).
- **Integrações**: widget, Health Connect, código de barras e modelos de treino entram no núcleo (§13.1).
- **Níveis**: 7, de Bronze a Deus; XP de conquistas + metas diárias (§9.5).
- **Extras aprovados**: planejamento da semana, lista de compras e jejum intermitente (§13.2, M8).
- **Backup**: backup automático do Android + exportar/importar JSON manual (§9.3).
- **Virada do dia**: meia-noite (§3).
- **Mini-gráfico do Dashboard**: % médio das 3 metas por dia, últimos 7 dias (§5.1).
- **Cores**: paleta própria por módulo, com tema claro e escuro; sem Material You (§2.5).
- **Registro de alimento/exercício**: bottom sheet em tela cheia (§2.2).
- **Primeiro uso**: onboarding com dados de perfil, corpo, objetivo e metas (§5.0).
- **FAB "+"** na Home com menu de registro (§5.1).
- **Idioma**: pt-BR, com textos em `strings.xml` (§2.4).
- **Onboarding**: nome e peso obrigatórios; resto pulável; lembrete de água oferecido no fim (§5.0).
- **Tipo de corpo**: nível de atividade (usado no cálculo) + biotipo (informativo) (§9.1).
- **Meta de exercício**: em kcal ou minutos, à escolha do usuário (§9.2).
- **Anotações**: formatação básica com barra de ferramentas, guardada em Markdown; diário com uma entrada por dia (§10).
- **Health Connect**: importação automática com aviso; kcal do dispositivo quando houver (§13.1).
- **Parabenizações**: toast discreto, nunca modal (§9.6).
- **Aprovados das sugestões**: recipientes nomeados (§6), repetir refeição (§7.1), exportar CSV (§9.3), aniversário (§9.6).
- **Recusados**: receita → alimento; diário no histórico do dia.
- **Combinado sem ação rápida**: tocar abre a funcionalidade (§5.3).
- **Sidebar**: layout do esboço com painel rápido à direita; rodapé (autor, avaliação, contato) para o futuro (§9.0, §11.1).
- **Histórico + Estatísticas**: um item só, com abas "Dia a dia" e "Visão geral" (§9.4).
- **Receitas**: atalho no menu para Anotações filtrada em receitas (§2.2).
- **Card de anotações na Home**: existe, mas começa oculto (§5.1).
- **Meta de alimentação**: avaliada ao fechar o dia; parabenização no dia seguinte (§9.6).
- **Acima da meta calórica**: barra âmbar + excedente + mensagem leve no card (§7.3).
- **Catálogo de cards**: Home montada pelo usuário a partir de um catálogo; tamanhos inteiro, metade (pode perder funções) e mini (painel rápido) (§5.1).
- **Acesso**: toda funcionalidade acessível sem card, pela seção Registros da sidebar e pelo FAB (§2.2).
- **Organização**: cards separados (Notas fixadas, Checklist, Próximo lembrete), sem agregador próprio; Checklist escolhe a lista (§5.1).
- **Progresso**: nível e sequência num card só (§5.1).
- **Card Combinado**: até 3 resumos; inteiro em colunas com anel, metade em linhas compactas; nome opcional; chega no M4 (§5.3).
- **Folgas**: 1 a cada 7 dias de presença, máx. 2, compartilhadas entre as sequências diárias; não valem para Ritmo (§9.5).
- **Bases de dados**: TACO completa + extras, com gramas e medidas caseiras; Compendium curado (~100) (§7.5, §8.1).
- **Home sem scroll**: número de linhas calculado por aparelho (`N`), linhas de altura fixa que preenchem a tela; scroll só aparece naturalmente com mais de `N` linhas; todo card tem 1 linha de altura (§5.1).
- **Linhas vazias da Home**: sugestão discreta e dispensável de adicionar card (§5.1).
- **Domínio funcional e testes**: regras como funções puras; testes unitários do domínio e de UI; CI no GitHub Actions; testes junto do código (§2.6, §2.7).
- **Acessibilidade**: fonte até 200% com adaptação; respeitar "Remover animações"; TalkBack completo, com reorganização sem arrastar (§2.8).
- **Testes de migração do banco**: um por migração, com schemas versionados (§2.7).
- **M1 dividido em M1a/M1b/M1c**; uma tela por módulo; navegação entre dias por setas, deslizar e calendário (§11).
- **Metas editáveis na tela de cada módulo e no Dashboard**, gravando no mesmo lugar (§6, §6.1).
- **M1.5 antes de Exercício**: a estrutura da Home (contrato de módulo, catálogo, grid, modo de edição, FAB) vem antes, para cada módulo novo chegar completo (§11).
- **Telas próprias para tudo** e personalidade visual por tela (gota na Água, curva + régua no Peso); transição "gelatina"; recipientes na tela de Água com fileira + modo edição e ícones desenhados (§2.2, §2.9, §6).
- **Tom dos textos**: zoeiro e épico, sem ser brega; trilhas com nomes evocativos (Caudal, Fornalha...) (§9.5, `TEXTOS.md`).

## 15. Decisões em aberto

- **Textos** ([`TEXTOS.md`](TEXTOS.md)): revisão do primeiro rascunho; faltam textos de folga usada, linhas vazias da Home, notificações e onboarding.
- **Cards do M8** (Planejado hoje, Jejum).
- **Calibrar na implementação**: altura base da linha (128dp) e curva de XP.
- **Layout para tablets**.
