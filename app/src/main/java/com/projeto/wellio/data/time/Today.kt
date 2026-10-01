package com.projeto.wellio.data.time

import com.projeto.wellio.domain.time.untilNextDay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime

/** Emite o dia atual e emite de novo quando ele vira, à meia-noite. */
fun todayFlow(clock: Clock): Flow<LocalDate> = flow {
    while (true) {
        val now = ZonedDateTime.now(clock)
        emit(now.toLocalDate())
        delay(untilNextDay(now).toMillis() + 1)
    }
}.distinctUntilChanged()
