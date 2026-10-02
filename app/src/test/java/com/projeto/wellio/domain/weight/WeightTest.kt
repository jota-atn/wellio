package com.projeto.wellio.domain.weight

import com.projeto.wellio.domain.model.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WeightTest {

    private fun weighing(day: Int, kg: Double) = WeightEntry("d$day", LocalDate.of(2026, 9, day), kg)

    @Test
    fun `peso aceita virgula`() {
        assertEquals(72.4, parseWeight("72,4")!!, 0.0)
    }

    @Test
    fun `peso aceita ponto`() {
        assertEquals(72.4, parseWeight("72.4")!!, 0.0)
    }

    @Test
    fun `peso arredonda a uma casa`() {
        assertEquals(72.5, parseWeight("72,46")!!, 0.0)
    }

    @Test
    fun `peso rejeita texto e valores fora da faixa`() {
        assertNull(parseWeight("abc"))
        assertNull(parseWeight("5"))
        assertNull(parseWeight("500"))
    }

    @Test
    fun `peso mais recente e o da data mais nova`() {
        val entries = listOf(weighing(10, 74.0), weighing(28, 72.4), weighing(20, 73.1))

        assertEquals(72.4, latestWeight(entries)!!.weightKg, 0.0)
    }

    @Test
    fun `sem pesagens nao ha peso mais recente`() {
        assertNull(latestWeight(emptyList()))
    }

    @Test
    fun `variacao e em relacao a pesagem anterior`() {
        val result = withChanges(listOf(weighing(10, 74.0), weighing(20, 73.1), weighing(28, 72.4)))

        assertEquals(listOf(-0.7, -0.9, null), result.map { it.second })
    }
}
