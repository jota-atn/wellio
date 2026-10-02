package com.projeto.wellio.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

@Serializable
data object WaterDestination

@Serializable
data object WeightDestination

/** Destino provisório das telas que ainda não existem. */
@Serializable
data class ComingSoonDestination(val title: String)
