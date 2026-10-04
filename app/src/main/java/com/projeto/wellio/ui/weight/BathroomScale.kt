package com.projeto.wellio.ui.weight

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projeto.wellio.domain.weight.DEFAULT_WEIGHT_KG
import com.projeto.wellio.ui.format.formatKg
import com.projeto.wellio.ui.theme.NeedleRed
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Balança de banheiro vista de cima (SPEC §2.9), com agulha vermelha no visor.
 *
 * - Ao abrir: duas pegadas aparecem, uma de cada vez, afundando a balança; a fita gira
 *   com mola até o peso atual e as pegadas somem.
 * - Pesagem nova: a balança quica e a fita rola do peso antigo para o novo; o número
 *   acompanha a fita.
 * - [goalReached] passando a valer (depois da abertura): confete discreto.
 *
 * [footer] fica dentro da plataforma, abaixo do peso (o peso alvo, SPEC §6.1).
 */
@Composable
fun BathroomScale(
    currentKg: Double?,
    subtitle: String,
    detail: String?,
    goalReached: Boolean,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = {}
) {
    val colors = WellioTheme.colors.base
    val target = currentKg ?: DEFAULT_WEIGHT_KG
    val latestTarget by rememberUpdatedState(target)
    val hopHeight = with(LocalDensity.current) { 10.dp.toPx() }

    val shown = remember { Animatable((target - 6).toFloat()) }
    val press = remember { Animatable(1f) }
    val hop = remember { Animatable(0f) }
    val leftFoot = remember { Animatable(0f) }
    val rightFoot = remember { Animatable(0f) }
    val confetti = remember { Animatable(1f) }
    val confettiSeed = remember { mutableIntStateOf(0) }
    val introDone = remember { mutableStateOf(false) }
    // Animações que precisam terminar mesmo se o peso mudar no meio
    val scope = rememberCoroutineScope()

    suspend fun stepOn(foot: Animatable<Float, *>) {
        scope.launch { foot.animateTo(1f, tween(durationMillis = 180)) }
        press.animateTo(0.965f, tween(durationMillis = 110))
        press.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
    }

    // Abertura: pegada esquerda, pegada direita, fita girando até o peso
    LaunchedEffect(Unit) {
        delay(150)
        stepOn(leftFoot)
        stepOn(rightFoot)
        introDone.value = true
        scope.launch {
            delay(900)
            launch { leftFoot.animateTo(0f, tween(durationMillis = 500)) }
            rightFoot.animateTo(0f, tween(durationMillis = 500))
        }
        shown.animateTo(latestTarget.toFloat(), spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessVeryLow))
    }

    // Pesagem nova (ou apagada) depois da abertura: quica e rola até o novo peso
    LaunchedEffect(target) {
        if (!introDone.value) return@LaunchedEffect
        scope.launch {
            hop.animateTo(-hopHeight, tween(durationMillis = 120))
            hop.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMediumLow))
        }
        shown.animateTo(target.toFloat(), spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessVeryLow))
    }

    // Alvo alcançado agora (não ao abrir a tela já com ele alcançado)
    val wasReached = remember { mutableStateOf(goalReached) }
    LaunchedEffect(goalReached) {
        val justReached = goalReached && !wasReached.value && introDone.value
        wasReached.value = goalReached
        if (justReached) {
            confettiSeed.intValue++
            scope.launch {
                confetti.snapTo(0f)
                confetti.animateTo(1f, tween(durationMillis = 1800, easing = LinearEasing))
            }
        }
    }

    val confettiColors = listOf(NeedleRed, colors.accent, Color.White, WellioTheme.colors.water.accent, WellioTheme.colors.food.accent)

    Surface(
        shape = RoundedCornerShape(40.dp),
        color = colors.container,
        contentColor = colors.onContainer,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = press.value
                scaleY = press.value
                translationY = hop.value
            }
    ) {
        Column(
            Modifier
                .drawBehind {
                    drawPlatform(colors.onContainer)
                    drawFootprints(colors.onContainer, leftFoot.value, rightFoot.value)
                }
                .drawWithContent {
                    drawContent()
                    if (confetti.value < 1f) drawConfetti(confetti.value, confettiSeed.intValue, confettiColors)
                }
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = listOfNotNull(
                        "Peso atual: ${currentKg?.let(::formatKg) ?: "sem pesagens"}",
                        subtitle,
                        detail
                    ).joinToString(". ")
                }
            ) {
                ScaleTape(
                    valueKg = shown.value,
                    tick = colors.onContainer,
                    needle = NeedleRed,
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.28f))
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (currentKg == null) "—" else formatKg((shown.value * 10).roundToInt() / 10.0),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onContainer.copy(alpha = 0.8f))
                detail?.let {
                    Spacer(Modifier.height(10.dp))
                    Surface(shape = RoundedCornerShape(50), color = colors.accent, contentColor = colors.onAccent) {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            footer()
        }
    }
}

/** Borda interna da plataforma, reflexo do vidro e os quatro pezinhos nos cantos. */
private fun DrawScope.drawPlatform(ink: Color) {
    val inset = 10.dp.toPx()
    drawRoundRect(
        ink.copy(alpha = 0.12f),
        topLeft = Offset(inset, inset),
        size = size.copy(width = size.width - 2 * inset, height = size.height - 2 * inset),
        cornerRadius = CornerRadius(32.dp.toPx()),
        style = Stroke(1.5.dp.toPx())
    )
    val glare = Path().apply {
        moveTo(size.width * 0.08f, size.height * 0.55f)
        lineTo(size.width * 0.38f, 0f)
        lineTo(size.width * 0.52f, 0f)
        lineTo(size.width * 0.12f, size.height * 0.75f)
        close()
    }
    drawPath(glare, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent)))
    val foot = 5.dp.toPx()
    val corner = 26.dp.toPx()
    listOf(
        Offset(corner, corner), Offset(size.width - corner, corner),
        Offset(corner, size.height - corner), Offset(size.width - corner, size.height - corner)
    ).forEach { drawCircle(ink.copy(alpha = 0.18f), foot, it) }
}

/** Duas pegadas (sola e dedos) na metade de baixo da plataforma; [left]/[right] de 0 a 1. */
private fun DrawScope.drawFootprints(ink: Color, left: Float, right: Float) {
    val footHeight = size.height * 0.36f
    listOf(
        Triple(Offset(size.width * 0.3f, size.height * 0.6f), -8f, left),
        Triple(Offset(size.width * 0.7f, size.height * 0.58f), 8f, right)
    ).forEachIndexed { index, (center, angle, visible) ->
        if (visible <= 0f) return@forEachIndexed
        val color = ink.copy(alpha = 0.2f * visible)
        val h = footHeight * (0.85f + 0.15f * visible)
        val w = h * 0.42f
        // Pé esquerdo tem o dedão à direita; o direito, espelhado
        val inner = if (index == 0) 1f else -1f
        rotate(angle, center) {
            translate(center.x, center.y) {
                drawOval(color, Offset(-w / 2, -h * 0.26f), Size(w, h * 0.42f))
                drawOval(color, Offset(-w * 0.3f - inner * w * 0.1f, -h * 0.02f), Size(w * 0.6f, h * 0.3f))
                drawOval(color, Offset(-w * 0.36f, h * 0.14f), Size(w * 0.72f, h * 0.34f))
                listOf(
                    Triple(0.3f, -0.36f, 0.13f),
                    Triple(0.04f, -0.4f, 0.09f),
                    Triple(-0.15f, -0.38f, 0.08f),
                    Triple(-0.31f, -0.34f, 0.07f),
                    Triple(-0.44f, -0.27f, 0.06f)
                ).forEach { (x, y, r) ->
                    drawCircle(color, w * r * 1.4f, Offset(inner * w * x, h * y))
                }
            }
        }
    }
}

/** Confete que estoura do visor e cai; [t] de 0 a 1. */
private fun DrawScope.drawConfetti(t: Float, seed: Int, palette: List<Color>) {
    if (t <= 0f) return
    val random = Random(seed)
    val origin = Offset(size.width / 2, size.height * 0.22f)
    repeat(28) {
        val vx = (random.nextFloat() * 2 - 1) * size.width * 0.7f
        val vy = -(0.3f + random.nextFloat() * 0.5f) * size.height
        val gravity = size.height * 1.6f
        val spin = random.nextFloat() * 720f - 360f
        val color = palette[random.nextInt(palette.size)]
        val center = Offset(origin.x + vx * t, origin.y + vy * t + gravity * t * t / 2)
        val piece = Size(7.dp.toPx(), 4.dp.toPx())
        rotate(spin * t, center) {
            drawRect(
                color.copy(alpha = 1f - t * t),
                topLeft = Offset(center.x - piece.width / 2, center.y - piece.height / 2),
                size = piece
            )
        }
    }
}

/** Fita numerada do visor, centrada em [valueKg]: marca a cada 0,1 kg e número a cada 1 kg. */
@Composable
private fun ScaleTape(valueKg: Float, tick: Color, needle: Color, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = tick, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Canvas(
        modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                // Esmaece as pontas da fita, como num visor curvo
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.Transparent, 0.25f to Color.Black, 0.75f to Color.Black, 1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        val spacing = 9.dp.toPx()
        val centerX = size.width / 2
        val halfTenths = centerX / spacing
        val first = floor(valueKg * 10 - halfTenths).toInt()
        val last = ceil(valueKg * 10 + halfTenths).toInt()
        for (tenth in first..last) {
            if (tenth < 0) continue
            val x = centerX + (tenth - valueKg * 10) * spacing
            val major = tenth % 10 == 0
            val length = size.height * when {
                major -> 0.38f
                tenth % 5 == 0 -> 0.26f
                else -> 0.16f
            }
            drawLine(
                tick.copy(alpha = if (major) 0.9f else 0.45f),
                Offset(x, 0f),
                Offset(x, length),
                strokeWidth = (if (major) 2.dp else 1.dp).toPx(),
                cap = StrokeCap.Round
            )
            if (major) {
                val label = measurer.measure("${tenth / 10}", labelStyle)
                drawText(label, topLeft = Offset(x - label.size.width / 2, size.height - label.size.height - 6.dp.toPx()))
            }
        }
        // Agulha vermelha: triângulo no alto e linha até o meio
        val needlePath = Path().apply {
            moveTo(centerX - 7.dp.toPx(), 0f)
            lineTo(centerX + 7.dp.toPx(), 0f)
            lineTo(centerX, 9.dp.toPx())
            close()
        }
        drawPath(needlePath, needle)
        drawLine(needle, Offset(centerX, 0f), Offset(centerX, size.height * 0.5f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
    }
}
