# Como as atividades aparecem no app (SPEC §8.1): cada exercício uma vez só na lista;
# os que têm variações ganham até 3 níveis (Leve / Moderada / Intensa), cada um com o
# MET do Compendium (código + nome na lista aprovada, docs/EXERCICIOS.md).
#
# (categoria, nome no app, ícone, [(nível, código, nome na lista aprovada, dica)])
# nível None = atividade sem variações.
L, M, I = "Leve", "Moderada", "Intensa"

FAMILIAS = [
    # Cardio
    ("Cardio", "Caminhada", "walk", [
        (L, "17160", "Caminhada (passeio)", "passeio, ~4 km/h"),
        (M, "17200", "Caminhada rápida (5,6–6,3 km/h)", "~6 km/h"),
        (I, "17220", "Caminhada muito rápida (6,4–7 km/h)", "~7 km/h"),
    ]),
    ("Cardio", "Caminhada na esteira", "treadmill", [
        (L, "17355", "Esteira, caminhada (4,8–5,5 km/h)", "~5 km/h"),
        (M, "17358", "Esteira, caminhada rápida (5,6–6,3 km/h)", "~6 km/h"),
        (I, "17361", "Esteira, caminhada muito rápida (6,4–7,1 km/h)", "~7 km/h"),
    ]),
    ("Cardio", "Passear com o cachorro", "dog_walk", [(None, "17165", "Passear com o cachorro", None)]),
    ("Cardio", "Trilha", "hike", [
        (L, "17082", "Trilha (hiking)", "ritmo normal"),
        (I, "17012", "Trilha com mochila", "com mochila"),
    ]),
    ("Cardio", "Corrida", "run", [
        (L, "12030", "Corrida, 8 km/h", "~8 km/h"),
        (M, "12060", "Corrida, 11 km/h", "~11 km/h"),
        (I, "12110", "Corrida, 14,5 km/h", "~14 km/h"),
    ]),
    ("Cardio", "Corrida em trilha", "trail_run", [(None, "12140", "Corrida em trilha / cross-country", None)]),
    ("Cardio", "Subir escadas", "stairs", [
        (M, "17131", "Subir escadas", "andando"),
        (I, "12170", "Subir escadas correndo", "correndo"),
    ]),
    ("Cardio", "Simulador de escada", "stairs", [(None, "02065", "Simulador de escada", None)]),
    ("Cardio", "Bicicleta", "bike", [
        (L, "01015", "Bicicleta, ritmo leve", "passeio"),
        (M, "01016", "Bicicleta, ritmo moderado", "ritmo constante"),
        (I, "01017", "Bicicleta, ritmo intenso", "forte"),
    ]),
    ("Cardio", "Mountain bike", "mtb", [(None, "01009", "Mountain bike", None)]),
    ("Cardio", "Bike indoor", "stationary_bike", [
        (M, "01200", "Bicicleta ergométrica", "ergométrica"),
        (I, "01270", "Spinning / bike indoor (aula)", "spinning"),
    ]),
    ("Cardio", "Elíptico", "elliptical", [
        (M, "02048", "Elíptico, moderado", None),
        (I, "02049", "Elíptico, intenso", None),
    ]),
    ("Cardio", "Remo ergômetro", "rowing", [
        (M, "02071", "Remo ergômetro, moderado", None),
        (I, "02070", "Remo ergômetro, intenso", None),
    ]),
    ("Cardio", "Pular corda", "jump_rope", [
        (L, "15552", "Pular corda, ritmo lento", "ritmo lento"),
        (I, "02068", "Pular corda", "ritmo rápido"),
    ]),
    ("Cardio", "Aeróbica / step", "aerobics", [(None, "02000", "Aeróbica", None)]),
    ("Cardio", "HIIT", "hiit", [
        (M, "02210", "HIIT, moderado", None),
        (I, "02214", "HIIT intenso (burpees, Tabata)", "burpees, Tabata"),
    ]),
    # Musculação
    ("Musculação", "Musculação", "weights", [
        (L, "02054", "Musculação (8–15 repetições)", "8–15 repetições"),
        (M, "02052", "Musculação pesada (agachamento, terra)", "cargas pesadas"),
        (I, "02055", "Musculação em circuito / supersets", "circuito, supersets"),
    ]),
    ("Musculação", "Academia (musculação + aeróbico)", "gym", [(None, "02061", "Academia (musculação + aeróbico)", None)]),
    ("Musculação", "Aula de condicionamento físico", "gym", [(None, "02062", "Aula de condicionamento físico", None)]),
    ("Musculação", "CrossFit", "crossfit", [(None, "02040", "CrossFit / circuito com kettlebell", None)]),
    ("Musculação", "Treino em circuito", "circuit", [(None, "02035", "Treino em circuito, moderado", None)]),
    ("Musculação", "Calistenia", "calisthenics", [
        (L, "02056", "Exercícios com peso do corpo", "peso do corpo"),
        (M, "02022", "Calistenia, moderada", None),
        (I, "02020", "Calistenia intensa (barra, burpees, polichinelos)", "barra, burpees"),
    ]),
    ("Musculação", "Treino em casa", "home", [
        (L, "02064", "Exercícios em casa", None),
        (I, "02145", "Treino em vídeo, intenso", "vídeo intenso"),
    ]),
    ("Musculação", "Abdominais e prancha", "core", [(None, "02024", "Abdominais e prancha", None)]),
    # Lutas
    ("Lutas", "Boxe", "boxing", [
        (L, "15110", "Boxe, saco de pancada", "saco de pancada"),
        (M, "15120", "Boxe, sparring", "sparring"),
        (I, "15100", "Boxe no ringue", "luta no ringue"),
    ]),
    ("Lutas", "Kickboxing / Muay Thai", "kickboxing", [(None, "15457", "Kickboxing / Muay Thai", None)]),
    ("Lutas", "Artes marciais (jiu-jitsu, karatê)", "martial_arts", [
        (L, "15425", "Artes marciais, ritmo leve / iniciante", "iniciante"),
        (I, "15430", "Artes marciais (jiu-jitsu, karatê, kung fu)", "ritmo de treino"),
    ]),
    ("Lutas", "Judô", "judo", [(None, "15433", "Judô", None)]),
    ("Lutas", "Taekwondo", "taekwondo", [(None, "15432", "Taekwondo (luta)", None)]),
    ("Lutas", "Capoeira", "capoeira", [(None, "15430", "Capoeira", None)]),
    # Esportes
    ("Esportes", "Futebol", "soccer", [
        (M, "15610", "Futebol (pelada)", "pelada"),
        (I, "15605", "Futebol competitivo", "competitivo"),
    ]),
    ("Esportes", "Futsal", "futsal", [(None, "15195", "Futsal", None)]),
    ("Esportes", "Basquete", "basketball", [
        (L, "15070", "Basquete, arremessos", "arremessos"),
        (M, "15055", "Basquete", "treino"),
        (I, "15040", "Basquete (jogo)", "jogo"),
    ]),
    ("Esportes", "Vôlei", "volleyball", [
        (L, "15710", "Vôlei", "recreativo"),
        (I, "15711", "Vôlei competitivo", "competitivo"),
    ]),
    ("Esportes", "Vôlei de praia / futevôlei", "beach_volleyball", [(None, "15725", "Vôlei de praia / futevôlei", None)]),
    ("Esportes", "Tênis", "tennis", [
        (L, "15685", "Tênis (duplas)", "duplas"),
        (I, "15690", "Tênis (simples)", "simples"),
    ]),
    ("Esportes", "Beach tennis", "beach_tennis", [(None, "15685", "Beach tennis", None)]),
    ("Esportes", "Tênis de mesa", "table_tennis", [(None, "15660", "Tênis de mesa", None)]),
    ("Esportes", "Badminton", "badminton", [(None, "15030", "Badminton", None)]),
    ("Esportes", "Squash", "squash", [(None, "15652", "Squash", None)]),
    ("Esportes", "Handebol", "handball", [(None, "15330", "Handebol", None)]),
    ("Esportes", "Golfe", "golf", [(None, "15255", "Golfe", None)]),
    ("Esportes", "Skate", "skate", [(None, "15580", "Skate", None)]),
    ("Esportes", "Patins", "skates", [(None, "15590", "Patins", None)]),
    ("Esportes", "Escalada", "climbing", [
        (M, "15537", "Escalada, dificuldade moderada", "via moderada"),
        (I, "15534", "Escalada, boulder", "boulder"),
    ]),
    ("Esportes", "Cavalgada / hipismo", "horse", [(None, "15370", "Cavalgada / hipismo", None)]),
    ("Esportes", "Boliche", "bowling", [(None, "15092", "Boliche", None)]),
    # Aquáticos
    ("Aquáticos", "Natação", "swim", [
        (L, "18240", "Natação, crawl lento", "lento"),
        (M, "18290", "Natação, crawl moderado", "moderado"),
        (I, "18230", "Natação, crawl rápido", "rápido"),
    ]),
    ("Aquáticos", "Hidroginástica", "water_aerobics", [(None, "18355", "Hidroginástica", None)]),
    ("Aquáticos", "Corrida na água", "deep_running", [(None, "18366", "Corrida na água (deep running)", None)]),
    ("Aquáticos", "Surfe", "surf", [(None, "18220", "Surfe", None)]),
    ("Aquáticos", "Stand up paddle", "sup", [(None, "18224", "Stand up paddle", None)]),
    ("Aquáticos", "Caiaque", "kayak", [(None, "18100", "Caiaque", None)]),
    ("Aquáticos", "Mergulho", "dive", [(None, "18208", "Mergulho", None)]),
    # Zen/Dança
    ("Zen/Dança", "Yoga", "yoga", [
        (L, "02150", "Yoga (Hatha)", "Hatha"),
        (M, "02185", "Yoga (Vinyasa)", "Vinyasa"),
        (I, "02160", "Yoga (Power)", "Power"),
    ]),
    ("Zen/Dança", "Pilates", "pilates", [(None, "02105", "Pilates", None)]),
    ("Zen/Dança", "Alongamento", "stretch", [(None, "02101", "Alongamento", None)]),
    ("Zen/Dança", "Tai chi / qi gong", "tai_chi", [(None, "15670", "Tai chi / qi gong", None)]),
    ("Zen/Dança", "Zumba", "zumba", [(None, "02310", "Zumba", None)]),
    ("Zen/Dança", "Dança", "dance", [
        (L, "03040", "Dança de salão, lenta", "salão, lenta"),
        (M, "03025", "Dança latina (salsa, forró, samba)", "salsa, forró, samba"),
        (I, "03031", "Dança animada (balada, quadrilha)", "balada, quadrilha"),
    ]),
    ("Zen/Dança", "Ballet / jazz", "ballet", [(None, "03010", "Ballet / jazz (aula)", None)]),
    ("Zen/Dança", "Pole dance", "pole", [(None, "02108", "Pole dance", None)]),
    # Outros
    ("Outros", "Tarefas domésticas", "house", [
        (M, "05026", "Tarefas domésticas, moderadas", "moderadas"),
        (I, "05027", "Faxina pesada", "faxina pesada"),
    ]),
    ("Outros", "Carregar móveis ou caixas", "boxes", [(None, "05120", "Carregar móveis ou caixas", None)]),
    ("Outros", "Jardinagem", "garden", [(None, "08245", "Jardinagem", None)]),
    ("Outros", "Videogame ativo", "videogame", [(None, "22240", "Videogame ativo (Just Dance, Ring Fit)", None)]),
    ("Outros", "Sexo", "sex", [(None, "14020", "Sexo", None)]),
]
