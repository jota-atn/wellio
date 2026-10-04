package com.projeto.wellio.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.projeto.wellio.ui.module.ModuleId
import com.projeto.wellio.ui.module.OneTapAction
import com.projeto.wellio.ui.module.WellioModule
import com.projeto.wellio.ui.theme.ModuleColors
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.delay

/**
 * FAB "+" da Home (SPEC §5.1): abre um leque com o registro rápido de cada módulo. O "+"
 * gira para "×", a Home escurece atrás e as pílulas sobem uma a uma, com mola, a partir
 * do botão. Segurar uma pílula mostra, ao lado dela, as ações de 1 toque do módulo
 * ([oneTap]), que registram sem abrir nada. Tocar fora ou voltar fecha. Some no modo de
 * edição ([visible]).
 */
@Composable
fun RegisterFab(
    modules: List<WellioModule>,
    visible: Boolean,
    oneTap: Map<ModuleId, List<OneTapAction>>,
    onPick: (WellioModule) -> Unit,
    onOneTap: (OneTapAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    // Módulo com as ações de 1 toque abertas (segurou a pílula)
    var showingActions by remember { mutableStateOf<ModuleId?>(null) }
    LaunchedEffect(expanded) { if (!expanded) showingActions = null }
    LaunchedEffect(visible) { if (!visible) expanded = false }
    BackHandler(enabled = expanded) { expanded = false }

    val scrim by animateFloatAsState(if (expanded) 0.55f else 0f, tween(durationMillis = 220), label = "escurecer")
    Box(modifier.fillMaxSize()) {
        if (scrim > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrim))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { expanded = false }
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            modules.forEachIndexed { index, module ->
                // O item mais perto do botão sai primeiro
                FabItem(
                    module = module,
                    expanded = expanded,
                    order = modules.size - 1 - index,
                    actions = oneTap[module.id].orEmpty(),
                    showingActions = showingActions == module.id,
                    onClick = {
                        expanded = false
                        onPick(module)
                    },
                    onLongClick = { showingActions = if (showingActions == module.id) null else module.id },
                    onAction = { action ->
                        expanded = false
                        onOneTap(action)
                    }
                )
            }
            AnimatedVisibility(visible = visible, enter = scaleIn(spring(dampingRatio = 0.6f)), exit = scaleOut()) {
                val rotation by animateFloatAsState(
                    if (expanded) 135f else 0f,
                    spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
                    label = "girar"
                )
                val colors = WellioTheme.colors.base
                FloatingActionButton(
                    onClick = { expanded = !expanded },
                    containerColor = colors.container,
                    contentColor = colors.onContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.semantics { contentDescription = if (expanded) "Fechar registro" else "Registrar" }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.rotate(rotation))
                }
            }
        }
    }
}

/**
 * Pílula de um módulo no leque: ícone na cor do módulo e o nome. Segurar mostra as ações
 * de 1 toque à esquerda dela, cada uma saindo com mola a partir da pílula.
 */
@Composable
private fun FabItem(
    module: WellioModule,
    expanded: Boolean,
    order: Int,
    actions: List<OneTapAction>,
    showingActions: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAction: (OneTapAction) -> Unit
) {
    val shown = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(expanded) {
        if (expanded) {
            delay(order * 45L)
            shown.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
        } else {
            shown.animateTo(0f, tween(durationMillis = 120))
        }
    }
    if (shown.value <= 0.01f && !expanded) return
    val colors = module.colors(WellioTheme.colors)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showingActions) {
            actions.forEachIndexed { index, action ->
                // A mais perto da pílula sai primeiro
                OneTapChip(action, order = actions.size - 1 - index, colors = colors) { onAction(action) }
                Spacer(Modifier.width(8.dp))
            }
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = colors.container,
            contentColor = colors.onContainer,
            shadowElevation = 6.dp,
            modifier = Modifier
                .graphicsLayer {
                    val t = shown.value
                    alpha = t.coerceIn(0f, 1f)
                    scaleX = 0.5f + 0.5f * t
                    scaleY = 0.5f + 0.5f * t
                    translationY = (1 - t) * 40.dp.toPx()
                    transformOrigin = TransformOrigin(1f, 1f)
                }
                .clip(RoundedCornerShape(50))
                .combinedClickable(
                    enabled = expanded,
                    onClickLabel = "Registrar",
                    onLongClickLabel = if (actions.isEmpty()) null else "Ações rápidas",
                    onClick = onClick,
                    onLongClick = if (actions.isEmpty()) null else {
                        {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        }
                    }
                )
                .semantics { contentDescription = "Registrar ${module.registerLabel}" }
        ) {
            Row(Modifier.padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = colors.accent, contentColor = colors.onAccent, modifier = Modifier.size(32.dp)) {
                    Icon(painterResource(module.icon), contentDescription = null, modifier = Modifier.padding(7.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(module.registerLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Uma ação de 1 toque: ícone e o que registra ("Copo 200 ml", "Corrida · 40 min"). */
@Composable
private fun OneTapChip(action: OneTapAction, order: Int, colors: ModuleColors, onClick: () -> Unit) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(order * 40L)
        shown.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = colors.accent,
        contentColor = colors.onAccent,
        shadowElevation = 6.dp,
        modifier = Modifier
            .graphicsLayer {
                val t = shown.value
                alpha = t.coerceIn(0f, 1f)
                scaleX = 0.4f + 0.6f * t
                scaleY = 0.4f + 0.6f * t
                translationX = (1 - t) * 30.dp.toPx()
                transformOrigin = TransformOrigin(1f, 0.5f)
            }
            .semantics { contentDescription = "Registrar ${action.label}" }
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(action.icon), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(action.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}
