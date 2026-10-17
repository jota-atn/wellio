# Pratos prontos do app (SPEC §7.0): pratos comuns montados com os alimentos da base,
# que entram no prato de uma vez e podem ser ajustados item a item.
#
# (nome, refeição sugerida, [(alimento, variante, medida POF, quantidade[, como aparece])])
# "como aparece" substitui "alimento variante" quando juntar os dois fica estranho.
CAFE, ALMOCO, LANCHE, JANTAR = "Café da manhã", "Almoço", "Lanche", "Jantar"

PRATOS = [
    ("Prato feito", ALMOCO, [
        ("Arroz", "branco", "COLHER DE SOPA", 4),
        ("Feijão", "carioca", "CONCHA", 1),
        ("Bife", "patinho", "BIFE", 1, "patinho"),
        ("Folhas", "alface", "FOLHA", 3, "alface"),
        ("Tomate", "", "FATIA", 3),
    ]),
    ("Feijoada completa", ALMOCO, [
        ("Feijoada", "", "CONCHA", 2),
        ("Arroz", "branco", "COLHER DE SOPA", 4),
        ("Couve", "refogada", "COLHER DE SOPA", 2),
        ("Farofa", "pronta", "COLHER DE SOPA", 2),
        ("Laranja", "", "UNIDADE", 1),
    ]),
    ("Strogonoff de frango com arroz", ALMOCO, [
        ("Strogonoff", "de frango", "COLHER DE ARROZ/SERVIR", 2),
        ("Arroz", "branco", "COLHER DE SOPA", 4),
    ]),
    ("Macarronada à bolonhesa", ALMOCO, [
        ("Macarrão", "à bolonhesa", "PEGADOR", 2),
        ("Queijo", "parmesão", "FATIA", 1),
    ]),
    ("Frango com batata-doce e brócolis", ALMOCO, [
        ("Frango", "peito grelhado", "FILE", 1, "peito de frango grelhado"),
        ("Batata-doce", "cozida", "UNIDADE", 1),
        ("Brócolis", "cozido", "COLHER DE SOPA", 3),
    ]),
    ("Peixe com arroz e legumes", JANTAR, [
        ("Peixe", "tilápia grelhada", "FILE", 1, "tilápia grelhada"),
        ("Arroz", "branco", "COLHER DE SOPA", 3),
        ("Salada de legumes", "no vapor", "COLHER DE SOPA", 3),
    ]),
    ("Churrasco", ALMOCO, [
        ("Picanha", "com gordura", "BIFE", 1),
        ("Linguiça", "de porco grelhada", "UNIDADE", 1),
        ("Farofa", "pronta", "COLHER DE SOPA", 2),
        ("Arroz", "branco", "COLHER DE SOPA", 3),
    ]),
    ("Salada completa", ALMOCO, [
        ("Folhas", "alface", "FOLHA", 4, "alface"),
        ("Tomate", "", "FATIA", 3),
        ("Pepino", "", "FATIA", 3),
        ("Cenoura", "crua", "COLHER DE SOPA", 2),
        ("Azeite", "de oliva", "COLHER DE SOPA", 1),
    ]),
    ("Omelete com salada", JANTAR, [
        ("Ovo", "omelete de queijo", "UNIDADE", 1, "omelete de queijo"),
        ("Folhas", "alface", "FOLHA", 3, "alface"),
        ("Tomate", "", "FATIA", 2),
    ]),
    ("Pão com manteiga e café com leite", CAFE, [
        ("Pão", "francês", "UNIDADE", 1),
        ("Manteiga", "com sal", "PONTA DE FACA", 1),
        ("Café", "com leite", "XICARA DE CHA", 1),
    ]),
    ("Pão com ovo", CAFE, [
        ("Pão", "francês", "UNIDADE", 1),
        ("Ovo", "frito", "UNIDADE", 1),
    ]),
    ("Misto quente", LANCHE, [
        ("Pão", "de forma integral", "FATIA", 2),
        ("Queijo", "mussarela", "FATIA", 2),
        ("Frios", "presunto", "FATIA", 2, "presunto"),
    ]),
    ("Iogurte com granola e banana", CAFE, [
        ("Iogurte", "natural", "POTE", 1),
        ("Granola", "", "COLHER DE SOPA", 2),
        ("Banana", "prata", "UNIDADE PEQUENA", 1),
    ]),
    ("Açaí na tigela", LANCHE, [
        ("Açaí", "com guaraná", "TIGELA", 1),
        ("Granola", "", "COLHER DE SOPA", 2),
        ("Banana", "prata", "UNIDADE PEQUENA", 1),
    ]),
    ("Vitamina de banana", CAFE, [
        ("Leite", "integral", "COPO MEDIO", 1),
        ("Banana", "prata", "UNIDADE", 1),
        ("Aveia", "em flocos", "COLHER DE SOPA", 1),
    ]),
    ("Tapioca com queijo", CAFE, [
        ("Tapioca", "com manteiga", "UNIDADE", 1),
        ("Queijo", "minas frescal", "FATIA", 1),
    ]),
]
