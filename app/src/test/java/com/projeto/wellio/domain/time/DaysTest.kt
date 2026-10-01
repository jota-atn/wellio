package com.projeto.wellio.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DaysTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")

    @Test
    fun `semana comeca na segunda por padrao`() {
        val quinta = LocalDate.of(2026, 10, 1)

        assertEquals(LocalDate.of(2026, 9, 28)..LocalDate.of(2026, 10, 4), weekOf(quinta))
    }

    @Test
    fun `segunda pertence a propria semana`() {
        val segunda = LocalDate.of(2026, 9, 28)

        assertEquals(segunda, weekOf(segunda).start)
    }

    @Test
    fun `semana pode comecar no domingo`() {
        val quinta = LocalDate.of(2026, 10, 1)

        assertEquals(LocalDate.of(2026, 9, 27), weekOf(quinta, DayOfWeek.SUNDAY).start)
    }

    @Test
    fun `tempo ate a meia noite`() {
        val now = ZonedDateTime.of(2026, 10, 1, 22, 30, 0, 0, saoPaulo)

        assertEquals(Duration.ofMinutes(90), untilNextDay(now))
    }
}
