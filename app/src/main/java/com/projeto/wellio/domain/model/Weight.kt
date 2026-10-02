package com.projeto.wellio.domain.model

import java.time.LocalDate

/** Uma pesagem por dia. */
data class WeightEntry(
    val id: String,
    val date: LocalDate,
    val weightKg: Double
)
