package com.projeto.wellio.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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

/**
 * Container que vira a tela, em transformação contínua (SPEC §2.9): card de um lado, tela do
 * outro, com a mesma [key]; [screen] marca o lado da tela. O retângulo cresce com mola; nenhum
 * dos dois conteúdos estica: o do card fica centralizado e some no começo, o da tela fica no
 * tamanho real, preso no topo, e vai sendo revelado enquanto aparece. Na volta, o inverso.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedContainer(key: String, shape: Shape, enabled: Boolean = true, screen: Boolean = false): Modifier {
    val shared = LocalSharedTransitionScope.current
    val animated = LocalNavAnimatedScope.current
    if (!enabled || !LocalCardTransitionEnabled.current || shared == null || animated == null) return this
    return with(shared) {
        this@sharedContainer.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animated,
            // Os fades se sobrepõem um pouco: nunca sobra o retângulo vazio
            enter = if (screen) fadeIn(tween(durationMillis = 260, delayMillis = 90)) else fadeIn(tween(durationMillis = 220, delayMillis = 100)),
            exit = if (screen) fadeOut(tween(durationMillis = 180)) else fadeOut(tween(durationMillis = 200)),
            boundsTransform = GrowSpring,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                contentScale = ContentScale.None,
                alignment = if (screen) Alignment.TopCenter else Alignment.Center
            ),
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
