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
}
