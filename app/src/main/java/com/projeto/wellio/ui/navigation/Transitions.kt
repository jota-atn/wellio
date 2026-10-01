package com.projeto.wellio.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

/*
 * Transições com "bounce": a tela desliza da lateral, passa um pouco do ponto
 * e assenta (mola pouco amortecida). Com "Remover animações" ligado no Android,
 * o Compose zera a duração das animações sozinho (SPEC §2.8).
 */
private val bouncySlide = spring<IntOffset>(
    dampingRatio = 0.72f,
    stiffness = Spring.StiffnessMediumLow
)
private val quickFade = spring<Float>(stiffness = Spring.StiffnessMedium)

private typealias Scope = AnimatedContentTransitionScope<NavBackStackEntry>

/** Entrar numa tela: a nova vem da direita. */
val enterFromRight: Scope.() -> EnterTransition = {
    slideInHorizontally(bouncySlide) { width -> width } + fadeIn(quickFade)
}

/** A tela de baixo recua um pouco para a esquerda enquanto a nova entra. */
val exitToLeft: Scope.() -> ExitTransition = {
    slideOutHorizontally(bouncySlide) { width -> -width / 4 } + fadeOut(quickFade)
}

/** Voltar: a tela de baixo retorna da esquerda... */
val popEnterFromLeft: Scope.() -> EnterTransition = {
    slideInHorizontally(bouncySlide) { width -> -width / 4 } + fadeIn(quickFade)
}

/** ...e a que estava aberta sai para a direita. */
val popExitToRight: Scope.() -> ExitTransition = {
    slideOutHorizontally(bouncySlide) { width -> width } + fadeOut(quickFade)
}
