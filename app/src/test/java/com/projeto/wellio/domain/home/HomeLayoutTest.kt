package com.projeto.wellio.domain.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutTest {

    private fun full(key: String) = HomeCard(key, CardSize.Full)
    private fun half(key: String) = HomeCard(key, CardSize.Half)

    @Test
    fun `inteiro ocupa a linha e dois metade dividem uma`() {
        val rows = homeRows(listOf(full("a"), half("b"), half("c")))

        assertEquals(listOf(listOf(full("a")), listOf(half("b"), half("c"))), rows)
    }

    @Test
    fun `metade sem vizinho fica sozinha e nada e reorganizado`() {
        val rows = homeRows(listOf(half("a"), full("b"), half("c")))

        assertEquals(listOf(listOf(half("a")), listOf(full("b")), listOf(half("c"))), rows)
    }

    @Test
    fun `layout padrao tem quatro linhas`() {
        assertEquals(4, homeRows(DefaultHomeLayout).size)
    }

    @Test
    fun `linhas por tela usam a altura base e preenchem a altura`() {
        val layout = rowLayout(availableHeightDp = 700f, fontScale = 1f, spacingDp = 12f)

        assertEquals(5, layout.count)
        assertEquals((700f - 4 * 12f) / 5, layout.rowHeightDp, 0.01f)
    }

    @Test
    fun `fonte maior deixa menos linhas`() {
        // Base de 166,4dp: 4 linhas (com os espaços) teriam 166dp, abaixo da base
        assertEquals(3, rowLayout(700f, fontScale = 1.3f, spacingDp = 12f).count)
    }

    @Test
    fun `tela baixa tem no minimo tres linhas sem encolher abaixo da base`() {
        val layout = rowLayout(availableHeightDp = 300f, fontScale = 1f, spacingDp = 12f)

        assertEquals(3, layout.count)
        assertEquals(128f, layout.rowHeightDp, 0.01f)
    }

    @Test
    fun `layout salvo volta igual`() {
        val keys = DefaultHomeLayout.map { it.key }.toSet()

        assertEquals(DefaultHomeLayout, decodeLayout(encodeLayout(DefaultHomeLayout), keys))
    }

    @Test
    fun `cards desconhecidos ou invalidos sao ignorados`() {
        assertEquals(listOf(full("water")), decodeLayout("water:F,sumiu:H,food:X,lixo", setOf("water", "food")))
    }

    @Test
    fun `mover leva o card para a nova posicao`() {
        val moved = moveCard(listOf(full("a"), full("b"), full("c")), from = 0, to = 2)

        assertEquals(listOf("b", "c", "a"), moved.map { it.key })
    }

    @Test
    fun `alternar tamanho troca inteiro e metade`() {
        assertEquals(CardSize.Half, toggleSize(listOf(full("a")), 0)[0].size)
        assertEquals(CardSize.Full, toggleSize(listOf(half("a")), 0)[0].size)
    }

    @Test
    fun `ocultar tira o card`() {
        assertEquals(listOf("b"), hideCard(listOf(full("a"), full("b")), 0).map { it.key })
    }

    @Test
    fun `adicionar vai para o fim e nao duplica`() {
        val cards = listOf(full("a"))

        assertEquals(listOf("a", "b"), addCard(cards, half("b")).map { it.key })
        assertEquals(cards, addCard(cards, half("a")))
    }

    @Test
    fun `linhas livres contam o que sobra na tela`() {
        assertEquals(1, freeRows(DefaultHomeLayout, rowsPerScreen = 5))
        assertEquals(0, freeRows(DefaultHomeLayout, rowsPerScreen = 3))
    }
}
