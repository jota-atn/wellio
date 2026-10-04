package com.projeto.wellio.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/*
 * Card que vira tela (SPEC §2.9): o card da Home cresce e se transforma na tela do módulo,
 * e os elementos ilustrativos (gota, visor da balança) voam de um para o outro. Sem as
 * camadas abaixo (testes, prévias do catálogo), os modificadores não fazem nada.
 */

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** A animação da tela atual no NavHost (entrando ou saindo). */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Desliga a transformação (ex.: prévias do catálogo, que não podem disputar a chave com o card da Home). */
val LocalCardTransitionEnabled = compositionLocalOf { true }

/** Mola do crescimento: rápida, com um leve quique ao chegar. */
@OptIn(ExperimentalSharedTransitionApi::class)
private val GrowSpring = BoundsTransform { _, _ -> spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow) }

/** Container que vira a tela (card de um lado, tela do outro, com a mesma [key]). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedContainer(key: String, shape: Shape, enabled: Boolean = true): Modifier {
    val shared = LocalSharedTransitionScope.current
    val animated = LocalNavAnimatedScope.current
    if (!enabled || !LocalCardTransitionEnabled.current || shared == null || animated == null) return this
    return with(shared) {
        this@sharedContainer.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animated,
            boundsTransform = GrowSpring,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
            clipInOverlayDuringTransition = OverlayClip(shape)
        )
    }
}

/** Elemento ilustrativo que voa entre o card e a tela (gota, visor da balança). */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedHero(key: String, enabled: Boolean = true): Modifier {
    val shared = LocalSharedTransitionScope.current
    val animated = LocalNavAnimatedScope.current
    if (!enabled || !LocalCardTransitionEnabled.current || shared == null || animated == null) return this
    return with(shared) {
        this@sharedHero.sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animated,
            boundsTransform = GrowSpring
        )
    }
}
