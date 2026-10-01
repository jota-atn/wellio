# Wellio — Especificação

> Documento vivo. Itens marcados com **(sugestão)** não foram pedidos diretamente; são propostas para discutir.
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
├── Estatísticas   (abas: Dia a dia | Visão geral)
├── Conquistas
├── Anotações
├── Receitas       (Anotações filtrada em receitas)
├── Metas
└── Configurações

Home → Água (detalhes/histórico)
     → Alimentação (diário do dia, cadastro de alimento)
     → Exercício (treinos do dia, cadastro de exercício)
     → Dashboard (detalhes)
```

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

- Cada módulo tem uma **cor de identidade** (Água = azul, Alimentação = verde, Exercício, Dashboard etc.), com variantes para **tema claro e escuro**.
- As cores ficam em tokens no tema (`ui/theme`), por exemplo `WellioColors.water.container` / `.onContainer` / `.accent`. Nenhum card usa `Color(0xFF...)` fixo.
- `GoalType` deixa de carregar `Color`: a cor é resolvida pelo tema a partir do tipo.
- Contraste mínimo WCAG AA para texto sobre os containers nos dois temas.

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
| `water_entry` | id, date, timestamp, amountMl |
| `food_item` | id, name, category, kcalPerPortion, portionDescription, proteinG?, carbsG?, fatG?, barcode?, iconKey, isCustom, isFavorite |
| `food_entry` | id, date, timestamp, foodItemId, meal?, quantity, kcalTotal |
| `exercise_type` | id, name, category, mets, iconKey, isCustom |
| `exercise_entry` | id, date, timestamp, exerciseTypeId, minutes, kcal, kcalIsManual, source (manual / template / health_connect), externalId? |
| `workout_template` | id, name, iconKey |
| `workout_template_item` | id, templateId, exerciseTypeId, minutes, position |
| `weight_entry` | id, date, weightKg |
| `goal` | id, title, metric, period, target, startDate, endDate, status |
| `achievement_unlock` | achievementId, tier, unlockedAt, xp |
| `note` | id, type, title, body, tags, pinned, color?, date?, reminderAt?, archived |
| `checklist_item` | id, noteId, text, done, position |

- `food_item` e `exercise_type` são populados (seed) a partir dos atuais `FoodRepository` e `ExerciseRepository`. Os cadastrados pelo usuário têm `isCustom = true`.
- `food_entry.kcalTotal` e `exercise_entry.kcal` são **gravados no momento do registro**. Editar um alimento ou o peso depois não reescreve o histórico.
- Definições de conquistas ficam em código; o banco só guarda o desbloqueio.

### 4.2 DataStore

- **Perfil**: nome, fotoUri?, dataNascimento?, sexo?, alturaCm?, nivelAtividade?, biotipo?, objetivo?, pesoAlvoKg?, tituloExibido?, onboardingConcluido
- **Metas diárias e semanais**: água (ml), kcal consumidas, exercício (unidade: kcal ou minutos); para cada uma, semanal automática (×7) ou valor próprio
- **Configurações**: ver §9.3
- **Layout da Home**: lista ordenada de `{cardId, size, visible}`

### 4.3 Preparação para sync futuro

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

### 5.1 Layout

Grid de **2 colunas** com cards. Cada card ocupa **meia largura** ou **largura inteira**.

Cards disponíveis:

| Card | Tamanho padrão | Conteúdo |
|---|---|---|
| Dashboard | inteiro | progresso das 3 metas do dia + mini-gráfico dos últimos 7 dias (uma barra por dia = média do % atingido das 3 metas, limitado a 100% por meta; hoje destacado) + atalho para detalhes |
| Água | inteiro | botões rápidos, barra de progresso, total/meta |
| Alimentação | meio | kcal consumidas/meta, atalho para registrar |
| Exercício | meio | progresso na unidade da meta (kcal ou minutos) e o outro valor em destaque menor; atalho para registrar |
| Saldo calórico **(sugestão)** | meio | consumidas − queimadas vs. meta |
| Anotações | inteiro | **oculto por padrão**; notas fixadas e itens pendentes da lista fixada |
| Sequência **(sugestão)** | meio | dias seguidos batendo metas |

Layout padrão: Dashboard, Água, Alimentação + Exercício lado a lado.

Um **botão flutuante "+" (FAB)** fica fixo no canto da Home e abre um menu: Água, Alimento, Exercício, Anotação, Peso. Ele leva para os mesmos fluxos dos cards, então dá para registrar algo mesmo que o card esteja oculto ou fora da tela. O FAB some no modo de edição do layout.

### 5.2 Cards arrastáveis

- **Pressionar e segurar** um card entra no **modo de edição** da Home.
- No modo de edição:
  - arrastar um card move sua posição; os outros se reorganizam com animação;
  - cada card mostra controles para **alternar tamanho** (meio/inteiro) e **ocultar**;
  - um botão **"+ Adicionar card"** lista os cards ocultos;
  - botão **Concluir** sai do modo (tocar fora ou voltar também sai).
- Vibração curta (haptic) ao pegar e soltar um card.
- O layout é salvo no DataStore ao sair do modo de edição.
- Em Configurações: **Restaurar layout padrão**.
- Fora do modo de edição, cards se comportam normalmente (toques e botões funcionam).

## 6. Água

- Botões rápidos de adicionar (padrão **+200, +300, +500 ml**), configuráveis em Configurações.
- Quantidade personalizada (campo numérico).
- **Sem limite na meta**: dá para beber mais do que a meta; a barra fica cheia e mostra o excedente (ex.: `2.300 / 2.000 ml`).
- Os botões negativos (−200, −300, −500) saem. Para corrigir: **Desfazer último** no card, ou editar/remover registros na tela de detalhes.
- **Tela de detalhes**: lista dos registros do dia com horário, edição e remoção; navegação entre dias; gráfico dos últimos 7/30 dias.
- Tamanhos de recipiente **(sugestão)**: atalhos nomeados ("Copo 200 ml", "Garrafa 500 ml") configuráveis.

## 7. Alimentação

### 7.1 Registro

Abre em bottom sheet de tela cheia (§2.2).

- Busca por nome + filtro por categoria (como hoje).
- Seções **Favoritos** e **Recentes** no topo da busca.
- Ao escolher o alimento: quantidade em porções (aceita vírgula), com prévia da kcal calculada antes de confirmar.
- **Refeição** opcional: café da manhã, almoço, lanche, jantar, ceia. Sugerida automaticamente pelo horário.
- **Repetir refeição** **(sugestão)**: copiar todos os itens de uma refeição de outro dia.

### 7.2 Cadastro de alimentos próprios

- Campos: nome, categoria, descrição da porção, kcal por porção, ícone; proteína/carboidrato/gordura opcionais.
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

## 8. Exercício

- Registro em bottom sheet de tela cheia (§2.2).
- Escolha por categoria (como hoje) + busca + favoritos/recentes.
- Duração em minutos; kcal estimada = `METs × peso (kg) × horas`, usando o **peso atual do perfil** (hoje está fixo em 70 kg).
- Prévia da kcal antes de salvar; o usuário pode **sobrescrever** a kcal manualmente (ex.: valor do relógio).
- Se o perfil não tiver peso (onboarding pulado), pedir o peso no primeiro registro.
- **Cadastro de exercícios próprios**: nome, categoria, METs (com ajuda: leve ≈ 3, moderado ≈ 5, intenso ≈ 8) ou kcal/minuto, ícone.
- **Tela de detalhes**: treinos do dia, edição e remoção, navegação entre dias.

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

A Home fica **embaçada** atrás da sidebar, e à direita aparece uma coluna de **mini-cards** para consulta rápida:

| Mini-card | Conteúdo |
|---|---|
| Checklist | itens pendentes de uma lista fixada; dá para marcar ali mesmo |
| Próximo lembrete | título e horário do próximo lembrete |
| Sequência | dias seguidos usando o app / batendo metas |
| Jejum | tempo restante do jejum em andamento (M8) |
| Planejado hoje | treinos e refeições planejados (M8) |
| Nível | barra de XP até o próximo nível |

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
- **Dados**: exportar e importar backup (JSON); exportar CSV **(sugestão)**; apagar todos os dados (com confirmação).
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

**Aniversário (sugestão)**: no dia do aniversário (data de nascimento do perfil), algo especial e leve, por exemplo:
- saudação personalizada no topo da Home e confetes na primeira abertura do dia;
- marco secreto "Mais um ano" (+XP);
- o cabeçalho da sidebar ganha um enfeite durante o dia.

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
- **Receita → alimento** **(sugestão)**: botão "Salvar como alimento" cria um `food_item` próprio com a kcal por porção da receita, para registrar no diário.
- **Diário no histórico do dia** **(sugestão)**: a entrada de diário aparece na tela de detalhes daquele dia, junto de água/comida/treino.

## 11. Roadmap

| Fase | Entrega |
|---|---|
| **M0 — Base** | Atualizar dependências; corrigir pacotes; Room + DataStore + `AppContainer`; Navigation + drawer vazio; Home montada com os cards atuais; abstração `iconKey`; persistência de água/comida/treino |
| **M1 — Registros completos** | Telas de detalhe com editar/remover/desfazer; navegação entre dias; favoritos/recentes; cadastro de alimentos e exercícios próprios; **leitor de código de barras**; **modelos de treino**; peso do perfil no cálculo; correções da §12 |
| **M2 — Perfil e Metas** | Modal de perfil (foto, nome, peso); metas diárias, semanais e de período; Dashboard com dados reais; **widget na tela inicial** |
| **M3 — Configurações e integrações** | Tema; notificações e lembretes; sons; backup/exportação; **Health Connect** |
| **M4 — Home personalizável** | Grid com cards arrastáveis, tamanhos, ocultar/mostrar, restaurar padrão |
| **M5 — Anotações** | Notas, listas, receitas, lembretes, diário, tags, fixadas |
| **M6 — Estatísticas e Conquistas** | Gráficos, heatmap, recordes; motor de conquistas, trilhas, marcos, XP e nível |
| **M7 — Ícones** | Substituir emojis por SVGs (pode andar em paralelo a partir do M0) |
| **M8 — Planejamento e cozinha** | Planejamento da semana, lista de compras, jejum intermitente (§13.2) |
| **Futuro** | Login, sincronização em nuvem, itens da §13.3 |

### 11.1 Futuro: rodapé da sidebar

Seção no fim da sidebar, do esboço original:
- **Conheça o autor**: tela sobre quem fez o app.
- **Avalie!**: 5 estrelas que levam à avaliação na Play Store (quando o app for publicado).
- **Contato**: atalhos para GitHub, LinkedIn e e-mail.

## 12. Correções conhecidas no código atual

- [ ] `MainActivity` não mostra a Home; `HomeViewModel` não é usado.
- [ ] Nenhum dado é persistido.
- [ ] `addWater` limita o total à meta (`coerceIn(0, waterGoalMl)`).
- [ ] Meta de água duplicada em `waterGoalMl` e `_goals`.
- [ ] Peso fixo em 70 kg no cálculo de exercício.
- [ ] Quantidade de comida não aceita vírgula e falha em silêncio com entrada inválida.
- [ ] Barras do gráfico do Dashboard são fixas.
- [ ] Barra "semanal" do Dashboard usa o valor do dia.
- [ ] "DETALHES" do card de água sem ação.
- [ ] `FoodCard`/`TrainCard` com valores padrão fictícios (1500/1200, 2000/2500).
- [ ] Título do `ExerciseDialog` usa o verde da alimentação; cor de Exercício no `GoalType` é cinza-claro e some no fundo.
- [ ] Pacote `com.projeto.wellio.components` em pasta `ui/components`.
- [ ] Dependências defasadas (core-ktx 1.10, lifecycle 2.6, Compose BOM 2024.09).

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

## 14. Decisões tomadas

- **Home**: grid de 2 colunas, cards de meia largura ou largura inteira (§5).
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
- **Parabenizações**: toast discreto, nunca modal (§9.6). Aniversário fica como sugestão.
- **Sidebar**: layout do esboço com painel rápido à direita; rodapé (autor, avaliação, contato) para o futuro (§9.0, §11.1).
- **Histórico + Estatísticas**: um item só, com abas "Dia a dia" e "Visão geral" (§9.4).
- **Receitas**: atalho no menu para Anotações filtrada em receitas (§2.2).
- **Card de anotações na Home**: existe, mas começa oculto (§5.1).
- **Meta de alimentação**: avaliada ao fechar o dia; parabenização no dia seguinte (§9.6).
- **Acima da meta calórica**: barra âmbar + excedente + mensagem leve no card (§7.3).
- **Tom dos textos**: zoeiro e épico, sem ser brega; trilhas com nomes evocativos (Caudal, Fornalha...) (§9.5, `TEXTOS.md`).

## 15. Decisões em aberto

- **Textos** ([`TEXTOS.md`](TEXTOS.md)): revisão do primeiro rascunho completo.
