package com.projeto.wellio.ui.water

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.model.WaterContainer
import com.projeto.wellio.ui.format.formatMl
import com.projeto.wellio.ui.icons.containerIconRes
import com.projeto.wellio.ui.theme.WellioTheme

/**
 * Recipientes na tela de Água (SPEC §6). Normal: fileira em que tocar registra,
 * segurar edita e "+" cria. Editando: grade "Meus recipientes" com lápis em cada um.
 */
@Composable
fun ContainersSection(
    containers: List<WaterContainer>,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onDrink: (WaterContainer) -> Unit,
    onEdit: (WaterContainer) -> Unit,
    onNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WellioTheme.colors.water
    Column(modifier) {
        Row(Modifier.padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (editing) "Meus recipientes" else "Beber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onEditingChange(!editing) }) {
                Text(if (editing) "Pronto" else "Editar", color = colors.accent)
            }
        }

        AnimatedContent(
            targetState = editing,
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                    fadeOut(spring(stiffness = Spring.StiffnessMedium))
            },
            label = "recipientes"
        ) { isEditing ->
            if (isEditing) {
                ContainersGrid(containers, onEdit, onNew)
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(containers, key = { it.id }) { container ->
                        ContainerButton(container, onClick = { onDrink(container) }, onLongClick = { onEdit(container) })
                    }
                    item { NewContainerButton(onNew) }
                }
            }
        }
    }
}

@Composable
private fun ContainerButton(container: WaterContainer, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = WellioTheme.colors.water
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.container,
        contentColor = colors.onContainer,
        modifier = Modifier
            .size(width = 88.dp, height = 104.dp)
            .combinedClickable(
                onClickLabel = "Registrar",
                onLongClickLabel = "Editar recipiente",
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "${container.name}, ${formatMl(container.amountMl)}"
            }
    ) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(painterResource(containerIconRes(container.iconKey)), contentDescription = null, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                container.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(formatMl(container.amountMl), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun NewContainerButton(onClick: () -> Unit) {
    val colors = WellioTheme.colors.water
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, colors.track),
        modifier = Modifier
            .size(width = 64.dp, height = 104.dp)
            .semantics { contentDescription = "Novo recipiente" }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContainersGrid(containers: List<WaterContainer>, onEdit: (WaterContainer) -> Unit, onNew: () -> Unit) {
    val colors = WellioTheme.colors.water
    FlowRow(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2
    ) {
        containers.forEach { container ->
            Surface(
                onClick = { onEdit(container) },
                shape = RoundedCornerShape(18.dp),
                color = colors.container,
                contentColor = colors.onContainer,
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Editar ${container.name}, ${formatMl(container.amountMl)}"
                    }
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(containerIconRes(container.iconKey)), contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(container.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatMl(container.amountMl), style = MaterialTheme.typography.labelMedium)
                    }
                    Surface(shape = CircleShape, color = colors.accent, contentColor = colors.onAccent, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(5.dp))
                    }
                }
            }
        }
        OutlinedCard(
            onClick = onNew,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, colors.track),
            modifier = Modifier.weight(1f)
        ) {
            Row(Modifier.padding(12.dp).height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent)
                Spacer(Modifier.width(8.dp))
                Text("Novo", style = MaterialTheme.typography.labelLarge, color = colors.accent)
            }
        }
    }
}
