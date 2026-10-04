package com.projeto.wellio.domain.home

import kotlin.math.floor

/** Um card na Home: qual ([key], ex.: "water", "dashboard") e em que tamanho. */
data class HomeCard(val key: String, val size: CardSize)

/** Chave do card do Dashboard, que não é de um módulo. */
const val DASHBOARD_KEY = "dashboard"

/** Layout padrão (SPEC §5.1): Dashboard, Água, Alimentação + Exercício, Peso. */
val DefaultHomeLayout = listOf(
    HomeCard(DASHBOARD_KEY, CardSize.Full),
    HomeCard("water", CardSize.Full),
    HomeCard("food", CardSize.Half),
    HomeCard("exercise", CardSize.Half),
    HomeCard("weight", CardSize.Half)
)

/**
 * Linhas da Home, na ordem exata do usuário (SPEC §5.1): um card inteiro ocupa a linha
 * sozinho; dois metade seguidos dividem uma linha; um metade sem vizinho fica sozinho à
 * esquerda (o espaço à direita fica vazio, nada é reorganizado).
 */
fun homeRows(cards: List<HomeCard>): List<List<HomeCard>> {
    val rows = mutableListOf<List<HomeCard>>()
    var pendingHalf: HomeCard? = null
    for (card in cards) {
        when (card.size) {
            CardSize.Full -> {
                pendingHalf?.let { rows += listOf(it) }
                pendingHalf = null
                rows += listOf(card)
            }
            CardSize.Half -> {
                val half = pendingHalf
                if (half == null) {
                    pendingHalf = card
                } else {
                    rows += listOf(half, card)
                    pendingHalf = null
                }
            }
        }
    }
    pendingHalf?.let { rows += listOf(it) }
    return rows
}

/** Quantas linhas cabem na tela ([count]) e a altura real de cada uma, em dp. */
data class RowLayout(val count: Int, val rowHeightDp: Float)

const val BASE_ROW_DP = 128f
const val MIN_ROWS = 3

/**
 * Linhas por tela (SPEC §5.1): `N = floor(disponível ÷ (128dp × escala da fonte))`, no
 * mínimo 3; as N linhas (com [spacingDp] entre elas) preenchem a altura disponível.
 */
fun rowLayout(availableHeightDp: Float, fontScale: Float, spacingDp: Float): RowLayout {
    val base = BASE_ROW_DP * fontScale
    val count = maxOf(MIN_ROWS, floor((availableHeightDp + spacingDp) / (base + spacingDp)).toInt())
    val height = (availableHeightDp - spacingDp * (count - 1)) / count
    return RowLayout(count, maxOf(height, base))
}

/** Layout salvo como texto: "dashboard:F,water:F,food:H". */
fun encodeLayout(cards: List<HomeCard>): String =
    cards.joinToString(",") { "${it.key}:${if (it.size == CardSize.Full) "F" else "H"}" }

/** Lê o layout salvo; itens inválidos ou de [knownKeys] desconhecidas são ignorados. */
fun decodeLayout(text: String, knownKeys: Set<String>): List<HomeCard> =
    text.split(',').mapNotNull { item ->
        val parts = item.split(':')
        if (parts.size != 2 || parts[0] !in knownKeys) return@mapNotNull null
        val size = when (parts[1]) {
            "F" -> CardSize.Full
            "H" -> CardSize.Half
            else -> return@mapNotNull null
        }
        HomeCard(parts[0], size)
    }

/** Move o card de [from] para a posição [to] (índices da lista), sem mudar os outros de ordem. */
fun moveCard(cards: List<HomeCard>, from: Int, to: Int): List<HomeCard> {
    if (from == to || from !in cards.indices || to !in cards.indices) return cards
    return cards.toMutableList().apply { add(to, removeAt(from)) }
}

/** Alterna o card de [index] entre inteiro e metade. */
fun toggleSize(cards: List<HomeCard>, index: Int): List<HomeCard> =
    cards.mapIndexed { i, card ->
        if (i != index) card else card.copy(size = if (card.size == CardSize.Full) CardSize.Half else CardSize.Full)
    }

/** Oculta (tira da Home) o card de [index]; ele continua no catálogo. */
fun hideCard(cards: List<HomeCard>, index: Int): List<HomeCard> = cards.filterIndexed { i, _ -> i != index }

/** Adiciona um card no fim da Home; um card que já está lá não entra de novo. */
fun addCard(cards: List<HomeCard>, card: HomeCard): List<HomeCard> =
    if (cards.any { it.key == card.key }) cards else cards + card

/** Linhas livres no fim da tela, onde aparece a sugestão de adicionar card (SPEC §5.1). */
fun freeRows(cards: List<HomeCard>, rowsPerScreen: Int): Int = maxOf(0, rowsPerScreen - homeRows(cards).size)
