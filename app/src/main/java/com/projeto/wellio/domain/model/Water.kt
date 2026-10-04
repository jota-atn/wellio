package com.projeto.wellio.domain.model

import com.projeto.wellio.domain.water.ContainerIcon
import java.time.Instant
import java.time.LocalDate

data class WaterEntry(
    val id: String,
    val date: LocalDate,
    val timestamp: Instant,
    val amountMl: Int,
    val containerName: String? = null
)

/** Recipiente nomeado ("Copo 200 ml"); os marcados como rápidos viram botões no card. */
data class WaterContainer(
    val id: String,
    val name: String,
    val amountMl: Int,
    val isQuick: Boolean,
    val position: Int,
    val iconKey: String = ContainerIcon.Copo.key
)
