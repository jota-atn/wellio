package com.projeto.wellio.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

/** [fromCard]: aberta tocando no card da Home (o card vira a tela); pela sidebar, desliza. */
@Serializable
data class WaterDestination(val fromCard: Boolean = false)

@Serializable
data class WeightDestination(val fromCard: Boolean = false)

@Serializable
data class ExerciseDestination(val fromCard: Boolean = false)

@Serializable
data object ProfileDestination

@Serializable
data object GoalsDestination

/** Tela do Dashboard (SPEC §5.4); [day] (ISO) abre num dia passado, vindo da semana em anéis. */
@Serializable
data class DashboardDestination(val fromCard: Boolean = false, val day: String? = null)

/** Destino provisório das telas que ainda não existem. */
@Serializable
data class ComingSoonDestination(val title: String)
