package com.projeto.wellio.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FormatTest {

    private val today = LocalDate.of(2026, 10, 1)

    @Test
    fun `ml usa separador de milhar brasileiro`() {
        assertEquals("1.400 ml", formatMl(1400))
    }

    @Test
    fun `horario em 24 horas no fuso escolhido`() {
        val zone = ZoneId.of("America/Sao_Paulo")

        assertEquals("08:10", formatTime(Instant.parse("2026-10-01T11:10:00Z"), zone))
    }

    @Test
    fun `hoje e ontem tem nome proprio`() {
        assertEquals("Hoje", dayLabel(today, today))
        assertEquals("Ontem", dayLabel(today.minusDays(1), today))
    }

    @Test
    fun `outros dias mostram dia da semana, dia e mes`() {
        assertEquals("Seg, 28 set", dayLabel(LocalDate.of(2026, 9, 28), today))
    }

    @Test
    fun `dias de outro ano mostram o ano`() {
        assertEquals("Qua, 31 dez 2025", dayLabel(LocalDate.of(2025, 12, 31), today))
    }

    @Test
    fun `peso com uma casa e virgula`() {
        assertEquals("72,4 kg", formatKg(72.4))
        assertEquals("80,0 kg", formatKg(80.0))
    }

    @Test
    fun `variacao de peso tem sinal`() {
        assertEquals("+0,3 kg", formatKgChange(0.3))
        assertEquals("−0,7 kg", formatKgChange(-0.7))
        assertEquals("0,0 kg", formatKgChange(0.0))
    }
}
