package com.projeto.wellio.ui.weight

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Balança de banheiro vista de cima (SPEC §2.9). Ao abrir a tela ela "afunda" de leve,
 * como se alguém subisse, e a fita do visor gira com mola até o peso atual; o número
 * acompanha a fita. Uma pesagem nova faz a fita andar do peso antigo para o novo.
 */
@Composable
fun BathroomScale(
    currentKg: Double?,
    subtitle: String,
    detail: String?,
    modifier: Modifier = Modifier
) {
    val colors = WellioTheme.colors.base
    val target = currentKg ?: DEFAULT_WEIGHT_KG
    val shown = remember { Animatable((target - 6).toFloat()) }
    val press = remember { Animatable(1f) }

    LaunchedEffect(target) {
        launch {
            press.animateTo(0.95f, tween(durationMillis = 110))
            press.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
        }
        shown.animateTo(target.toFloat(), spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessVeryLow))
    }

    Surface(
        shape = RoundedCornerShape(40.dp),
        color = colors.container,
        contentColor = colors.onContainer,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = press.value
                scaleY = press.value
            }
            .clearAndSetSemantics {
                contentDescription = listOfNotNull(
                    "Peso atual: ${currentKg?.let(::formatKg) ?: "sem pesagens"}",
                    subtitle,
                    detail
                ).joinToString(". ")
            }
    ) {
        Column(
            Modifier
                .drawBehind { drawPlatform(colors.onContainer) }
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScaleTape(
                valueKg = shown.value,
                tick = colors.onContainer,
                needle = colors.accent,
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
    }
}

/** Borda interna da plataforma, reflexo do vidro e os quatro pezinhos nos cantos. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlatform(ink: Color) {
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
        // Agulha: triângulo no alto e linha até o meio
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
