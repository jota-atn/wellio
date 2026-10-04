package com.projeto.wellio.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

/** [fromCard]: aberta tocando no card da Home (o card vira a tela); pela sidebar, desliza. */
@Serializable
data class WaterDestination(val fromCard: Boolean = false)

@Serializable
data class WeightDestination(val fromCard: Boolean = false)

/** Destino provisório das telas que ainda não existem. */
@Serializable
data class ComingSoonDestination(val title: String)
