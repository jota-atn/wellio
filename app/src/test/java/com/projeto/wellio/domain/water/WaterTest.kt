package com.projeto.wellio.domain.water

import com.projeto.wellio.domain.model.WaterContainer
import com.projeto.wellio.domain.model.WaterEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class WaterTest {

    private fun container(name: String, ml: Int, quick: Boolean = true, position: Int = 0) =
        WaterContainer(id = name, name = name, amountMl = ml, isQuick = quick, position = position)

    private fun entry(ml: Int, at: String) =
        WaterEntry(id = at, date = LocalDate.of(2026, 10, 1), timestamp = Instant.parse("2026-10-01T${at}:00Z"), amountMl = ml)

    @Test
    fun `botoes rapidos seguem a ordem definida`() {
        val containers = listOf(container("Garrafa", 500, position = 2), container("Copo", 200, position = 0))

        assertEquals(listOf("Copo", "Garrafa"), quickButtons(containers).map { it.name })
    }

    @Test
    fun `recipientes que nao sao rapidos ficam fora do card`() {
        val containers = listOf(container("Copo", 200), container("Jarra", 1000, quick = false))

        assertEquals(listOf("Copo"), quickButtons(containers).map { it.name })
    }

    @Test
    fun `card mostra no maximo tres botoes`() {
        val containers = (0..4).map { container("R$it", 100, position = it) }

        assertEquals(3, quickButtons(containers).size)
    }

    @Test
    fun `ultimo registro e o de horario mais recente`() {
        val entries = listOf(entry(200, "08:00"), entry(500, "14:00"), entry(300, "10:00"))

        assertEquals(500, lastEntry(entries)?.amountMl)
    }

    @Test
    fun `sem registros nao ha o que desfazer`() {
        assertNull(lastEntry(emptyList()))
    }

    @Test
    fun `total soma os registros`() {
        assertEquals(1000, totalMl(listOf(entry(200, "08:00"), entry(800, "09:00"))))
    }

    @Test
    fun `quantidade aceita inteiros positivos`() {
        assertEquals(350, parseWaterAmount(" 350 "))
    }

    @Test
    fun `quantidade rejeita zero, texto e valores absurdos`() {
        assertNull(parseWaterAmount("0"))
        assertNull(parseWaterAmount("abc"))
        assertNull(parseWaterAmount("9000"))
    }

    @Test
    fun `recipiente precisa de nome`() {
        assertEquals(ContainerValidation.BlankName, validateContainer("  ", "200"))
    }

    @Test
    fun `recipiente precisa de quantidade valida`() {
        assertEquals(ContainerValidation.InvalidAmount, validateContainer("Copo", "0"))
    }

    @Test
    fun `recipiente valido tem nome sem espacos extras`() {
        assertEquals(ContainerValidation.Valid("Squeeze", 750), validateContainer(" Squeeze ", "750"))
    }
}
