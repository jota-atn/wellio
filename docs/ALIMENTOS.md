# Wellio — Alimentos (base do app)

Base de alimentos para revisão (SPEC §7.0 e §7.5). Cada alimento aparece uma vez no app; o tipo ou
preparo é escolhido nos detalhes do registro.

- **Valores** por 100 g (ou 100 ml): TACO 4ª edição (NEPA/Unicamp). P = proteína, C = carboidrato, G = gordura.
- **Medidas caseiras**: POF 2008-2009 (IBGE), em gramas. Todo alimento aceita gramas também.
- ⚠️ = **extra**: a TACO não tem (ou veio sem valor); valor de referência, a revisar.
- Gerado por `tools/taco/gerar_base.py` a partir de `tools/taco/familias.py` e `tools/taco/pratos.py`.

**Para revisar:** marque o que tirar, adicionar, juntar ou renomear.

## Cereais e pães (19)

- **Arroz** — colher de sopa 25 g, colher de servir 45 g, escumadeira 85 g
  - branco: 128 kcal · P 2,5 · C 28,1 · G 0,2
  - integral: 124 kcal · P 2,6 · C 25,8 · G 1,0
- **Pão** — unidade 50 g, fatia 25 g
  - francês: 300 kcal · P 8,0 · C 58,6 · G 3,1
  - de forma integral: 253 kcal · P 9,4 · C 49,9 · G 3,7
  - sovado: 311 kcal · P 8,4 · C 61,5 · G 2,8
  - de milho: 292 kcal · P 8,3 · C 56,4 · G 3,1
- **Pão de queijo** — unidade 20 g, unidade pequena 10 g
  - —: 363 kcal · P 5,1 · C 34,2 · G 24,6
- **Torrada** — unidade 8 g
  - —: 377 kcal · P 10,5 · C 74,6 · G 3,3
- **Biscoito** — unidade 5 g
  - cream cracker: 432 kcal · P 10,1 · C 68,7 · G 14,4
  - maisena: 443 kcal · P 8,1 · C 75,2 · G 12,0
  - recheado: 472 kcal · P 6,4 · C 70,5 · G 19,6
  - wafer: 502 kcal · P 5,6 · C 67,5 · G 24,7
  - de polvilho: 438 kcal · P 1,3 · C 80,5 · G 12,2
- **Bolo** — fatia 60 g, pedaço 60 g
  - de chocolate: 410 kcal · P 6,2 · C 54,7 · G 18,5
  - de milho: 311 kcal · P 4,8 · C 45,1 · G 12,4
  - de coco: 333 kcal · P 5,7 · C 52,3 · G 11,3
  - de aipim: 324 kcal · P 4,4 · C 47,9 · G 12,7
- **Macarrão** — pegador 110 g, colher de servir 50 g, prato raso 320 g
  - cozido ⚠️: 158 kcal · P 5,8 · C 30,9 · G 0,9
  - à bolonhesa: 120 kcal · P 4,9 · C 22,5 · G 0,9
  - instantâneo: 436 kcal · P 8,8 · C 62,4 · G 17,2
- **Nhoque** — colher de servir 70 g, escumadeira 100 g
  - —: 181 kcal · P 5,9 · C 36,8 · G 1,9
- **Aveia** — colher de sopa 15 g
  - em flocos: 394 kcal · P 13,9 · C 66,6 · G 8,5
- **Granola** — colher de sopa 10 g
  - — ⚠️: 410 kcal · P 9,0 · C 66,0 · G 13,0
- **Cereal matinal** — colher de sopa 4 g, cumbuca 30 g
  - de milho: 365 kcal · P 7,2 · C 83,8 · G 1,0
  - açucarado: 377 kcal · P 4,7 · C 88,8 · G 0,7
- **Cuscuz** — fatia 135 g, colher de servir 24 g
  - nordestino: 113 kcal · P 2,2 · C 25,3 · G 0,7
  - paulista: 142 kcal · P 2,6 · C 22,5 · G 4,6
- **Tapioca** — unidade 50 g
  - com manteiga: 348 kcal · P 0,1 · C 63,6 · G 10,9
- **Pipoca** — pacote 20 g, xícara de chá 6 g
  - —: 448 kcal · P 9,9 · C 70,3 · G 15,9
- **Milho verde** — colher de sopa 24 g
  - enlatado: 98 kcal · P 3,2 · C 17,1 · G 2,4
- **Polenta** — pedaço 150 g, colher de servir 60 g
  - —: 103 kcal · P 2,3 · C 23,3 · G 0,3
- **Farofa** — colher de sopa 15 g
  - pronta: 406 kcal · P 2,1 · C 80,3 · G 9,1
  - farinha de mandioca: 365 kcal · P 1,2 · C 89,2 · G 0,3
- **Pamonha** — unidade 160 g
  - —: 171 kcal · P 2,6 · C 30,7 · G 4,8
- **Canjica** — concha 120 g
  - com leite: 112 kcal · P 2,4 · C 23,6 · G 1,2

## Leguminosas (5)

- **Feijão** — concha 140 g, colher de sopa 17 g, escumadeira 35 g
  - carioca: 76 kcal · P 4,8 · C 13,6 · G 0,5
  - preto: 77 kcal · P 4,5 · C 14,0 · G 0,5
  - fradinho: 78 kcal · P 5,1 · C 13,5 · G 0,6
- **Lentilha** — concha 160 g, colher de sopa 18 g
  - cozida: 93 kcal · P 6,3 · C 16,3 · G 0,5
- **Grão-de-bico** — colher de sopa 22 g, concha 120 g
  - cozido ⚠️: 164 kcal · P 8,9 · C 27,4 · G 2,6
- **Ervilha** — colher de sopa 27 g
  - enlatada: 74 kcal · P 4,6 · C 13,4 · G 0,4
- **Tofu** — fatia 20 g
  - —: 64 kcal · P 6,6 · C 2,1 · G 4,0

## Carnes e ovos (16)

- **Bife** — bife 150 g
  - patinho: 219 kcal · P 35,9 · C 0,0 · G 7,3
  - alcatra: 241 kcal · P 31,9 · C 0,0 · G 11,6
  - contra-filé: 194 kcal · P 35,9 · C 0,0 · G 4,5
  - filé-mignon: 220 kcal · P 32,8 · C 0,0 · G 8,8
  - à milanesa: 352 kcal · P 20,6 · C 12,2 · G 24,0
- **Carne moída** — colher de sopa 25 g, colher de servir 60 g
  - refogada: 212 kcal · P 26,7 · C 0,0 · G 10,9
- **Carne de panela** — pedaço 35 g, colher de sopa 30 g
  - acém: 215 kcal · P 27,3 · C 0,0 · G 10,9
  - músculo: 194 kcal · P 31,2 · C 0,0 · G 6,7
  - lagarto: 222 kcal · P 32,9 · C 0,0 · G 9,1
  - coxão mole: 219 kcal · P 32,4 · C 0,0 · G 8,9
- **Picanha** — bife 150 g
  - com gordura: 289 kcal · P 26,4 · C 0,0 · G 19,5
  - sem gordura: 238 kcal · P 31,9 · C 0,0 · G 11,3
- **Costela bovina** — pedaço 40 g
  - assada: 373 kcal · P 28,8 · C 0,0 · G 27,7
- **Fígado** — bife 100 g
  - bovino grelhado: 225 kcal · P 29,9 · C 4,2 · G 9,0
- **Carne seca** — colher de sopa 18 g, pedaço 65 g
  - cozida: 313 kcal · P 26,9 · C 0,0 · G 21,9
- **Almôndega** — unidade 50 g
  - frita: 272 kcal · P 18,2 · C 14,3 · G 15,8
- **Hambúrguer (carne)** — unidade 56 g
  - grelhado: 210 kcal · P 13,2 · C 11,3 · G 12,4
  - frito: 258 kcal · P 20,0 · C 6,3 · G 17,0
- **Frango** — filé 100 g, unidade 100 g
  - peito grelhado: 159 kcal · P 32,0 · C 0,0 · G 2,5
  - peito cozido: 163 kcal · P 31,5 · C 0,0 · G 3,2
  - coxa assada: 215 kcal · P 28,5 · C 0,1 · G 10,4
  - sobrecoxa assada: 233 kcal · P 29,2 · C 0,0 · G 12,0
  - à milanesa: 221 kcal · P 28,5 · C 7,5 · G 7,8
- **Coração de frango** — espetinho 46 g
  - grelhado: 207 kcal · P 22,4 · C 0,6 · G 12,1
- **Carne de porco** — fatia 190 g, pedaço 95 g
  - lombo assado: 210 kcal · P 35,7 · C 0,0 · G 6,4
  - bisteca grelhada: 280 kcal · P 28,9 · C 0,0 · G 17,4
  - pernil assado: 262 kcal · P 32,1 · C 0,0 · G 13,9
  - costela assada: 402 kcal · P 30,2 · C 0,0 · G 30,3
- **Linguiça** — unidade 60 g, pedaço 10 g
  - de porco grelhada: 296 kcal · P 23,2 · C 0,0 · G 21,9
  - de frango grelhada: 244 kcal · P 18,2 · C 0,0 · G 18,4
  - de porco frita: 280 kcal · P 20,5 · C 0,0 · G 21,3
- **Frios** — fatia 15 g
  - presunto: 94 kcal · P 14,3 · C 2,1 · G 2,7
  - peito de peru ⚠️: 105 kcal · P 17,0 · C 3,5 · G 2,3
  - mortadela: 269 kcal · P 12,0 · C 5,8 · G 21,6
  - salame: 398 kcal · P 25,8 · C 2,9 · G 30,6
- **Bacon** — fatia 15 g
  - frito: 697 kcal · P 27,3 · C 0,0 · G 64,3
- **Ovo** — unidade 45 g
  - cozido: 146 kcal · P 13,3 · C 0,6 · G 9,5
  - frito: 240 kcal · P 15,6 · C 1,2 · G 18,6
  - mexido ⚠️: 166 kcal · P 11,1 · C 1,6 · G 12,2
  - omelete de queijo: 268 kcal · P 15,6 · C 0,4 · G 22,0

## Peixes e frutos do mar (6)

- **Peixe** — filé 120 g, posta 200 g
  - tilápia grelhada ⚠️: 128 kcal · P 26,2 · C 0,0 · G 2,7
  - merluza assada: 122 kcal · P 26,6 · C 0,0 · G 0,9
  - merluza frita: 192 kcal · P 26,9 · C 0,0 · G 8,5
  - pescada frita: 154 kcal · P 28,6 · C 0,0 · G 3,6
  - abadejo grelhado: 130 kcal · P 27,6 · C 0,0 · G 1,3
- **Salmão** — filé 120 g
  - grelhado: 243 kcal · P 26,1 · C 0,0 · G 14,5
- **Atum** — colher de sopa 16 g, lata 135 g
  - em conserva: 166 kcal · P 26,2 · C 0,0 · G 6,0
- **Sardinha** — unidade 42 g
  - em conserva: 285 kcal · P 15,9 · C 0,0 · G 24,0
  - assada: 164 kcal · P 32,2 · C 0,0 · G 3,0
- **Camarão** — colher de sopa 20 g, unidade 30 g
  - cozido: 90 kcal · P 19,0 · C 0,0 · G 1,0
  - frito: 231 kcal · P 18,4 · C 2,9 · G 15,6
- **Bacalhau** — pedaço 135 g, colher de sopa 20 g
  - refogado: 140 kcal · P 24,0 · C 1,2 · G 3,6

## Verduras e legumes (20)

- **Folhas** — folha 10 g, prato de sobremesa 30 g
  - alface: 11 kcal · P 1,3 · C 1,7 · G 0,2
  - rúcula: 13 kcal · P 1,8 · C 2,2 · G 0,1
  - agrião: 17 kcal · P 2,7 · C 2,3 · G 0,2
  - acelga: 21 kcal · P 1,4 · C 4,6 · G 0,1
- **Tomate** — unidade 100 g, fatia 15 g
  - —: 15 kcal · P 1,1 · C 3,1 · G 0,2
- **Pepino** — unidade 100 g, fatia 3 g
  - —: 10 kcal · P 0,9 · C 2,0 · G 0,0
- **Cenoura** — colher de sopa 12 g, unidade 120 g
  - crua: 34 kcal · P 1,3 · C 7,7 · G 0,2
  - cozida: 30 kcal · P 0,8 · C 6,7 · G 0,2
- **Beterraba** — colher de sopa 16 g, unidade 140 g
  - crua: 49 kcal · P 1,9 · C 11,1 · G 0,1
  - cozida: 32 kcal · P 1,3 · C 7,2 · G 0,1
- **Brócolis** — colher de sopa 10 g, ramo 60 g
  - cozido: 25 kcal · P 2,1 · C 4,4 · G 0,5
- **Couve-flor** — colher de sopa 25 g, pedaço 60 g
  - cozida: 19 kcal · P 1,2 · C 3,9 · G 0,3
- **Couve** — colher de sopa 20 g
  - refogada: 90 kcal · P 1,7 · C 8,7 · G 6,6
  - crua: 27 kcal · P 2,9 · C 4,3 · G 0,5
- **Repolho** — colher de sopa 10 g
  - cru: 17 kcal · P 0,9 · C 3,9 · G 0,1
- **Abobrinha** — colher de sopa 30 g
  - refogada: 24 kcal · P 1,1 · C 4,2 · G 0,8
  - cozida: 15 kcal · P 1,1 · C 3,0 · G 0,2
- **Abóbora** — colher de sopa 36 g, pedaço 50 g
  - cabotiá cozida: 48 kcal · P 1,4 · C 10,8 · G 0,7
  - moranga refogada: 29 kcal · P 0,4 · C 6,0 · G 0,8
- **Chuchu** — colher de sopa 20 g
  - cozido: 19 kcal · P 0,4 · C 4,8 · G 0,0
- **Berinjela** — colher de sopa 25 g, fatia 20 g
  - cozida: 19 kcal · P 0,7 · C 4,5 · G 0,1
- **Espinafre** — colher de sopa 25 g
  - refogado: 67 kcal · P 2,7 · C 4,2 · G 5,4
- **Batata** — unidade 140 g, colher de sopa 30 g
  - cozida: 52 kcal · P 1,2 · C 11,9 · G 0,0
  - frita: 267 kcal · P 5,0 · C 35,6 · G 13,1
  - sauté: 68 kcal · P 1,3 · C 14,1 · G 0,9
  - purê ⚠️: 113 kcal · P 1,9 · C 16,9 · G 4,2
- **Batata-doce** — unidade 355 g, pedaço 70 g
  - cozida: 77 kcal · P 0,6 · C 18,4 · G 0,1
- **Mandioca** — pedaço 100 g, colher de servir 60 g
  - cozida: 125 kcal · P 0,6 · C 30,1 · G 0,3
  - frita: 300 kcal · P 1,4 · C 50,3 · G 11,2
- **Mandioquinha** — unidade pequena 45 g, colher de sopa 35 g
  - cozida: 80 kcal · P 0,9 · C 18,9 · G 0,2
- **Salada de legumes** — colher de sopa 38 g, colher de servir 70 g
  - no vapor: 35 kcal · P 2,0 · C 7,1 · G 0,3
  - com maionese: 96 kcal · P 1,1 · C 8,9 · G 7,0
- **Palmito** — unidade 100 g, colher de sopa 15 g
  - em conserva: 29 kcal · P 2,5 · C 5,5 · G 0,5

## Frutas (20)

- **Banana** — unidade 75 g, unidade pequena 56 g
  - prata: 98 kcal · P 1,3 · C 26,0 · G 0,1
  - nanica: 92 kcal · P 1,4 · C 23,8 · G 0,1
  - maçã: 87 kcal · P 1,8 · C 22,3 · G 0,1
  - da terra: 128 kcal · P 1,4 · C 33,7 · G 0,2
- **Maçã** — unidade 150 g
  - —: 56 kcal · P 0,3 · C 15,2 · G 0,0
- **Laranja** — unidade 180 g
  - —: 37 kcal · P 1,0 · C 8,9 · G 0,1
- **Mexerica** — unidade 135 g
  - poncã: 38 kcal · P 0,8 · C 9,6 · G 0,1
- **Mamão** — fatia 170 g, unidade 310 g
  - papaia: 40 kcal · P 0,5 · C 10,4 · G 0,1
  - formosa: 45 kcal · P 0,8 · C 11,6 · G 0,1
- **Manga** — unidade 140 g, fatia 22 g
  - palmer: 72 kcal · P 0,4 · C 19,4 · G 0,2
  - tommy: 51 kcal · P 0,9 · C 12,8 · G 0,2
- **Melancia** — fatia 200 g
  - —: 33 kcal · P 0,9 · C 8,1 · G 0,0
- **Melão** — fatia 90 g
  - —: 29 kcal · P 0,7 · C 7,5 · G 0,0
- **Abacaxi** — fatia 75 g, rodela 75 g
  - —: 48 kcal · P 0,9 · C 12,3 · G 0,1
- **Uva** — cacho 170 g, unidade 8 g
  - —: 53 kcal · P 0,7 · C 13,6 · G 0,2
- **Morango** — unidade 12 g, xícara de chá 120 g
  - —: 30 kcal · P 0,9 · C 6,8 · G 0,3
- **Pera** — unidade 130 g
  - —: 53 kcal · P 0,6 · C 14,0 · G 0,1
- **Abacate** — colher de sopa 45 g, unidade 430 g
  - —: 96 kcal · P 1,2 · C 6,0 · G 8,4
- **Kiwi** — unidade 94 g
  - —: 51 kcal · P 1,3 · C 11,5 · G 0,6
- **Goiaba** — unidade 170 g
  - —: 54 kcal · P 1,1 · C 13,0 · G 0,4
- **Maracujá** — unidade 45 g
  - —: 68 kcal · P 2,0 · C 12,3 · G 2,1
- **Açaí** — copo 273 g, tigela 202 g
  - puro: 58 kcal · P 0,8 · C 6,2 · G 3,9
  - com guaraná: 110 kcal · P 0,7 · C 21,5 · G 3,7
- **Caqui** — unidade 110 g
  - —: 71 kcal · P 0,4 · C 19,3 · G 0,1
- **Pêssego** — unidade 60 g
  - —: 36 kcal · P 0,8 · C 9,3 · G 0,0
- **Ameixa** — unidade 42 g
  - —: 53 kcal · P 0,8 · C 13,9 · G 0,0

## Laticínios (7)

- **Leite** — copo americano 150 g, copo 240 g, xícara de chá 200 g
  - integral ⚠️: 61 kcal · P 3,2 · C 4,6 · G 3,3
  - desnatado ⚠️: 35 kcal · P 3,4 · C 5,0 · G 0,2
  - semidesnatado ⚠️: 45 kcal · P 3,3 · C 4,8 · G 1,5
- **Iogurte** — pote 200 g, copo 240 g
  - natural: 51 kcal · P 4,1 · C 1,9 · G 3,0
  - natural desnatado: 41 kcal · P 3,8 · C 5,8 · G 0,3
  - de morango: 70 kcal · P 2,7 · C 9,7 · G 2,3
- **Queijo** — fatia 20 g
  - minas frescal: 264 kcal · P 17,4 · C 3,2 · G 20,2
  - mussarela: 330 kcal · P 22,6 · C 3,0 · G 25,2
  - prato: 360 kcal · P 22,7 · C 1,9 · G 29,1
  - meia cura: 321 kcal · P 21,2 · C 3,6 · G 24,6
  - parmesão: 453 kcal · P 35,6 · C 1,7 · G 33,5
- **Requeijão** — colher de sopa 30 g, ponta de faca 6 g
  - cremoso: 257 kcal · P 9,6 · C 2,4 · G 23,4
- **Ricota** — fatia 35 g, colher de sopa 44 g
  - —: 140 kcal · P 12,6 · C 3,8 · G 8,1
- **Creme de leite** — colher de sopa 15 g
  - —: 221 kcal · P 1,5 · C 4,5 · G 22,5
- **Petit suisse** — pote 200 g
  - —: 121 kcal · P 5,8 · C 18,5 · G 2,8

## Bebidas (11)

- **Café** — xícara de café 50 g, xícara de chá 200 g
  - sem açúcar: 9 kcal · P 0,7 · C 1,5 · G 0,1
  - com leite ⚠️: 38 kcal · P 1,9 · C 2,8 · G 1,9
- **Chá** — xícara de chá 200 g, copo 240 g
  - preto: 2 kcal · P 0,0 · C 0,6 · G 0,0
  - mate: 3 kcal · P 0,0 · C 0,6 · G 0,1
- **Suco** — copo 240 g, copo grande 300 g
  - de laranja: 33 kcal · P 0,7 · C 7,6 · G 0,1
  - de uva: 58 kcal · P 0,0 · C 14,7 · G 0,0
  - de maracujá: 42 kcal · P 0,8 · C 9,6 · G 0,2
- **Refrigerante** — copo 240 g, lata 350 g
  - cola: 34 kcal · P 0,0 · C 8,7 · G 0,0
  - guaraná: 39 kcal · P 0,0 · C 10,0 · G 0,0
  - zero ⚠️: 1 kcal · P 0,0 · C 0,1 · G 0,0
- **Água de coco** — copo 240 g
  - —: 22 kcal · P 0,0 · C 5,3 · G 0,0
- **Achocolatado** — copo 240 g, caixinha (200 ml) 200 g
  - pronto: 83 kcal · P 2,1 · C 14,2 · G 2,2
- **Leite de soja** — copo 240 g
  - —: 39 kcal · P 2,4 · C 4,3 · G 1,6
- **Cerveja** — lata 350 g, copo de cerveja 290 g
  - —: 41 kcal · P 0,6 · C 3,3 · G 0,0
- **Vinho** — taça 150 g
  - tinto ⚠️: 85 kcal · P 0,1 · C 2,6 · G 0,0
- **Isotônico** — garrafa (500 ml) 500 g
  - —: 26 kcal · P 0,0 · C 6,4 · G 0,0
- **Whey protein** — colher de sopa 16 g
  - — ⚠️: 380 kcal · P 78,0 · C 8,0 · G 5,0

## Doces (11)

- **Chocolate** — barra 170 g, tablete 6 g
  - ao leite: 540 kcal · P 7,2 · C 59,6 · G 30,3
  - meio amargo: 475 kcal · P 4,9 · C 62,4 · G 29,9
- **Açúcar** — colher de chá 5 g, colher de sopa 24 g
  - refinado: 387 kcal · P 0,3 · C 99,5 · G 0,0
  - mascavo: 369 kcal · P 0,8 · C 94,5 · G 0,1
- **Achocolatado em pó** — colher de sopa 16 g
  - —: 401 kcal · P 4,2 · C 91,2 · G 2,2
- **Mel** — colher de sopa 15 g, colher de chá 3 g
  - —: 309 kcal · P 0,0 · C 84,0 · G 0,0
- **Doce de leite** — colher de sopa 40 g
  - —: 306 kcal · P 5,5 · C 59,5 · G 6,0
- **Goiabada** — fatia 60 g
  - —: 269 kcal · P 0,6 · C 74,1 · G 0,0
- **Paçoca** — unidade 30 g
  - —: 487 kcal · P 16,0 · C 52,4 · G 26,1
- **Brigadeiro** — unidade 15 g
  - — ⚠️: 410 kcal · P 3,8 · C 60,0 · G 17,0
- **Sorvete** — bola 80 g, copo 240 g
  - de creme ⚠️: 207 kcal · P 3,5 · C 23,6 · G 11,0
- **Pudim** — fatia 130 g, pedaço 130 g
  - de leite ⚠️: 250 kcal · P 5,6 · C 40,0 · G 7,5
- **Leite condensado** — colher de sopa 40 g
  - —: 313 kcal · P 7,7 · C 57,0 · G 6,7

## Gorduras e molhos (8)

- **Azeite** — colher de sopa 8 g, colher de chá 2 g
  - de oliva: 884 kcal · P 0,0 · C 0,0 · G 100,0
- **Óleo** — colher de sopa 8 g
  - de soja: 884 kcal · P 0,0 · C 0,0 · G 100,0
- **Manteiga** — ponta de faca 5 g, colher de chá 8 g
  - com sal: 726 kcal · P 0,4 · C 0,1 · G 82,4
- **Margarina** — ponta de faca 5 g, colher de chá 8 g
  - —: 596 kcal · P 0,0 · C 0,0 · G 67,4
- **Maionese** — colher de sopa 27 g
  - —: 302 kcal · P 0,6 · C 7,9 · G 30,5
- **Ketchup** — colher de sopa 27 g
  - — ⚠️: 112 kcal · P 1,2 · C 25,8 · G 0,3
- **Molho de tomate** — colher de sopa 20 g
  - —: 38 kcal · P 1,4 · C 7,7 · G 0,9
- **Shoyu** — colher de sopa 6 g
  - —: 61 kcal · P 3,3 · C 11,6 · G 0,3

## Lanches e salgados (9)

- **Pastel** — unidade 32 g
  - de carne: 388 kcal · P 10,1 · C 43,8 · G 20,1
  - de queijo: 422 kcal · P 8,7 · C 48,1 · G 22,7
- **Coxinha** — unidade 50 g
  - —: 283 kcal · P 9,6 · C 34,5 · G 11,8
- **Empada** — unidade 55 g
  - de frango: 358 kcal · P 6,9 · C 47,5 · G 15,6
- **Quibe** — unidade 50 g
  - frito: 254 kcal · P 14,9 · C 12,3 · G 15,8
  - assado: 136 kcal · P 14,6 · C 12,9 · G 2,7
- **Pizza** — fatia 100 g
  - mussarela ⚠️: 271 kcal · P 11,6 · C 30,0 · G 11,4
  - calabresa ⚠️: 283 kcal · P 11,8 · C 29,0 · G 13,2
- **Hambúrguer (sanduíche)** — unidade 125 g
  - — ⚠️: 254 kcal · P 12,5 · C 29,6 · G 9,4
- **Cachorro-quente** — unidade 125 g
  - — ⚠️: 247 kcal · P 9,0 · C 27,0 · G 11,2
- **Batata chips** — porção 110 g
  - —: 543 kcal · P 5,6 · C 51,2 · G 36,6
- **Acarajé** — unidade 100 g
  - —: 289 kcal · P 8,3 · C 19,1 · G 19,9

## Pratos típicos (11)

- **Feijoada** — concha 225 g
  - —: 117 kcal · P 8,7 · C 11,6 · G 6,5
- **Strogonoff** — colher de servir 60 g, concha 120 g
  - de frango: 157 kcal · P 17,6 · C 2,6 · G 8,0
  - de carne: 173 kcal · P 15,0 · C 3,0 · G 10,8
- **Feijão tropeiro** — colher de sopa 17 g, colher de servir 35 g
  - —: 152 kcal · P 10,2 · C 19,6 · G 6,8
- **Baião de dois** — colher de servir 45 g
  - —: 136 kcal · P 6,2 · C 20,4 · G 3,2
- **Arroz carreteiro** — colher de servir 45 g
  - —: 154 kcal · P 10,8 · C 11,6 · G 7,1
- **Yakisoba** — prato raso 320 g, pegador 110 g
  - —: 113 kcal · P 7,5 · C 18,3 · G 2,6
- **Salpicão** — colher de servir 70 g
  - de frango: 148 kcal · P 13,9 · C 4,6 · G 7,8
- **Tabule** — colher de sopa 38 g
  - —: 57 kcal · P 2,0 · C 10,6 · G 1,2
- **Vaca atolada** — concha 140 g
  - —: 145 kcal · P 5,1 · C 10,1 · G 9,3
- **Bife à cavalo** — bife 150 g
  - —: 291 kcal · P 23,7 · C 0,0 · G 21,1
- **Vatapá** — colher de sopa 35 g
  - —: 255 kcal · P 6,0 · C 9,7 · G 23,2

## Castanhas e sementes (6)

- **Castanha-do-pará** — unidade 4 g
  - —: 643 kcal · P 14,5 · C 15,1 · G 63,5
- **Castanha de caju** — unidade 2 g, punhado 40 g
  - torrada: 570 kcal · P 18,5 · C 29,1 · G 46,3
- **Amendoim** — colher de sopa 17 g, punhado 30 g
  - torrado: 606 kcal · P 22,5 · C 18,7 · G 54,0
- **Amêndoa** — unidade 2 g, punhado 40 g
  - torrada: 581 kcal · P 18,6 · C 29,5 · G 47,3
- **Nozes** — unidade 5 g
  - —: 620 kcal · P 14,0 · C 18,4 · G 59,4
- **Linhaça** — colher de sopa 13 g
  - —: 495 kcal · P 14,1 · C 43,3 · G 32,3

## Pratos prontos do app (16)

Entram no prato de uma vez; cada item pode ser ajustado.

- **Prato feito** (Almoço, ~574 kcal): 4 colheres de sopa de arroz branco; 1 concha de feijão carioca; 1 bife de patinho; 3 folhas de alface; 3 fatias de tomate
- **Feijoada completa** (Almoço, ~878 kcal): 2 conchas de feijoada; 4 colheres de sopa de arroz branco; 2 colheres de sopa de couve refogada; 2 colheres de sopa de farofa pronta; 1 unidade de laranja
- **Strogonoff de frango com arroz** (Almoço, ~316 kcal): 2 colheres de servir de strogonoff de frango; 4 colheres de sopa de arroz branco
- **Macarronada à bolonhesa** (Almoço, ~354 kcal): 2 pegadores de macarrão à bolonhesa; 1 fatia de queijo parmesão
- **Frango com batata-doce e brócolis** (Almoço, ~439 kcal): 1 filé de peito de frango grelhado; 1 unidade de batata-doce cozida; 3 colheres de sopa de brócolis cozido
- **Peixe com arroz e legumes** (Jantar, ~290 kcal): 1 filé de tilápia grelhada; 3 colheres de sopa de arroz branco; 3 colheres de sopa de salada de legumes no vapor
- **Churrasco** (Almoço, ~829 kcal): 1 bife de picanha com gordura; 1 unidade de linguiça de porco grelhada; 2 colheres de sopa de farofa pronta; 3 colheres de sopa de arroz branco
- **Salada completa** (Almoço, ~91 kcal): 4 folhas de alface; 3 fatias de tomate; 3 fatias de pepino; 2 colheres de sopa de cenoura crua; 1 colher de sopa de azeite de oliva
- **Omelete com salada** (Jantar, ~128 kcal): 1 unidade de omelete de queijo; 3 folhas de alface; 2 fatias de tomate
- **Pão com manteiga e café com leite** (Café da manhã, ~262 kcal): 1 unidade de pão francês; 1 ponta de faca de manteiga com sal; 1 xícara de chá de café com leite
- **Pão com ovo** (Café da manhã, ~258 kcal): 1 unidade de pão francês; 1 unidade de ovo frito
- **Misto quente** (Lanche, ~287 kcal): 2 fatias de pão de forma integral; 2 fatias de queijo mussarela; 2 fatias de presunto
- **Iogurte com granola e banana** (Café da manhã, ~240 kcal): 1 pote de iogurte natural; 2 colheres de sopa de granola; 1 unidade pequena de banana prata
- **Açaí na tigela** (Lanche, ~360 kcal): 1 tigela de açaí com guaraná; 2 colheres de sopa de granola; 1 unidade pequena de banana prata
- **Vitamina de banana** (Café da manhã, ~279 kcal): 1 copo de leite integral; 1 unidade de banana prata; 1 colher de sopa de aveia em flocos
- **Tapioca com queijo** (Café da manhã, ~227 kcal): 1 unidade de tapioca com manteiga; 1 fatia de queijo minas frescal

## Extras ⚠️ e de onde vieram os valores

- Leite integral: 61 kcal · P 3,2 · C 4,6 · G 3,3 — rótulo médio de leite integral UHT
- Leite desnatado: 35 kcal · P 3,4 · C 5,0 · G 0,2 — rótulo médio de leite desnatado UHT
- Leite semidesnatado: 45 kcal · P 3,3 · C 4,8 · G 1,5 — rótulo médio de leite semidesnatado UHT
- Macarrão cozido: 158 kcal · P 5,8 · C 30,9 · G 0,9 — USDA, massa cozida sem sal
- Grão-de-bico cozido: 164 kcal · P 8,9 · C 27,4 · G 2,6 — USDA, grão-de-bico cozido
- Peixe tilápia grelhada: 128 kcal · P 26,2 · C 0,0 · G 2,7 — USDA, tilápia cozida em calor seco
- Batata purê: 113 kcal · P 1,9 · C 16,9 · G 4,2 — USDA, purê de batata com leite e manteiga
- Pizza mussarela: 271 kcal · P 11,6 · C 30,0 · G 11,4 — referência média de pizza de mussarela
- Pizza calabresa: 283 kcal · P 11,8 · C 29,0 · G 13,2 — referência média de pizza de calabresa
- Hambúrguer (sanduíche): 254 kcal · P 12,5 · C 29,6 · G 9,4 — referência média de hambúrguer de lanchonete
- Cachorro-quente: 247 kcal · P 9,0 · C 27,0 · G 11,2 — referência média de cachorro-quente simples
- Refrigerante zero: 1 kcal · P 0,0 · C 0,1 · G 0,0 — rótulo de refrigerante zero/diet
- Whey protein: 380 kcal · P 78,0 · C 8,0 · G 5,0 — rótulo médio de whey protein concentrado
- Granola: 410 kcal · P 9,0 · C 66,0 · G 13,0 — rótulo médio de granola tradicional
- Brigadeiro: 410 kcal · P 3,8 · C 60,0 · G 17,0 — referência de brigadeiro caseiro
- Sorvete de creme: 207 kcal · P 3,5 · C 23,6 · G 11,0 — USDA, sorvete de baunilha
- Pudim de leite: 250 kcal · P 5,6 · C 40,0 · G 7,5 — referência de pudim de leite condensado
- Ketchup: 112 kcal · P 1,2 · C 25,8 · G 0,3 — USDA, ketchup
- Vinho tinto: 85 kcal · P 0,1 · C 2,6 · G 0,0 — USDA, vinho tinto
- Café com leite: 38 kcal · P 1,9 · C 2,8 · G 1,9 — café coado com leite integral, meio a meio
- Ovo mexido: 166 kcal · P 11,1 · C 1,6 · G 12,2 — USDA, ovo mexido
- Frios peito de peru: 105 kcal · P 17,0 · C 3,5 · G 2,3 — rótulo médio de peito de peru defumado fatiado
