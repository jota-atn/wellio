package com.projeto.wellio.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class NavigationTest {

    private val today = LocalDate.of(2026, 10, 1)

    @Test
    fun `hoje e dias passados podem ser abertos`() {
        assertTrue(canOpen(today, today))
        assertTrue(canOpen(today.minusDays(30), today))
    }

    @Test
    fun `dias futuros nao podem ser abertos`() {
        assertFalse(canOpen(today.plusDays(1), today))
    }

    @Test
    fun `nao ha dia seguinte a partir de hoje`() {
        assertNull(nextDay(today, today))
    }

    @Test
    fun `dia seguinte a ontem e hoje`() {
        assertEquals(today, nextDay(today.minusDays(1), today))
    }

    @Test
    fun `registro em dia passado usa o dia e o horario escolhidos`() {
        val zone = ZoneId.of("America/Sao_Paulo")

        assertEquals(
            Instant.parse("2026-09-30T11:30:00Z"),
            instantAt(LocalDate.of(2026, 9, 30), LocalTime.of(8, 30), zone)
        )
    }
}
