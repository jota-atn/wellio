package com.projeto.wellio.ui.goals

import com.projeto.wellio.domain.goals.rings
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.model.Totals
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardTextsTest {

    private val goals = Goals.Default

    private fun phrase(totals: Totals, isToday: Boolean = true) = dayPhrase(totals, goals, rings(totals, goals), isToday)

    @Test
    fun `dia em branco provoca`() {
        assertEquals("Dia em branco. O primeiro gole é o mais difícil.", phrase(Totals()))
    }

    @Test
    fun `perto de fechar diz quanto falta no anel mais perto`() {
        assertEquals("Faltam 400 ml pra fechar a água. Bora?", phrase(Totals(waterMl = 1600, kcalConsumed = 200)))
    }

    @Test
    fun `tres fechados e lenda`() {
        assertEquals("Três anéis fechados. Lenda.", phrase(Totals(waterMl = 2000, kcalConsumed = 2000, kcalBurned = 500)))
    }

    @Test
    fun `conta os aneis fechados quando nenhum esta perto`() {
        assertEquals("Dois anéis fechados. Falta um pra lenda.", phrase(Totals(waterMl = 2000, kcalConsumed = 2000, kcalBurned = 100)))
    }

    @Test
    fun `dia passado fala no passado`() {
        assertEquals("Esse dia fechou com 50% das metas.", phrase(Totals(waterMl = 1000, kcalConsumed = 1000, kcalBurned = 250), isToday = false))
    }

    @Test
    fun `comparacao com ontem em pontos`() {
        assertEquals("+12% que ontem", comparedToYesterday(0.66f, 0.54f))
        assertEquals("−5% que ontem", comparedToYesterday(0.5f, 0.55f))
        assertEquals("igual a ontem", comparedToYesterday(0.5f, 0.5f))
    }
}
