package com.projeto.wellio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.projeto.wellio.ui.icons.WellioIcons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projeto.wellio.domain.goals.progress
import com.projeto.wellio.domain.model.WaterContainer
import com.projeto.wellio.ui.format.formatMl
import com.projeto.wellio.ui.icons.WellioIcon
import com.projeto.wellio.ui.theme.WellioTheme

@Composable
fun WaterCard(
    modifier: Modifier = Modifier,
    currentMl: Int,
    goalMl: Int,
    quickButtons: List<WaterContainer>,
    canUndo: Boolean,
    onQuickAdd: (WaterContainer) -> Unit,
    onUndo: () -> Unit,
    onDetailsClick: () -> Unit
) {
    val colors = WellioTheme.colors.water
    val fraction = progress(currentMl, goalMl)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.container, contentColor = colors.onContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WellioIcon("water", size = 28.sp)
                Spacer(Modifier.width(12.dp))
                Text("ÁGUA", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(
                    "${formatMl(currentMl)} / ${formatMl(goalMl)}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.semantics {
                        contentDescription = "${formatMl(currentMl)} de ${formatMl(goalMl)}, ${(fraction * 100).toInt()} por cento"
                    }
                )
            }

            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.track)
            ) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(colors.accent))
            }

            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                quickButtons.forEach { container ->
                    FilledTonalButton(
                        onClick = { onQuickAdd(container) },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .semantics { contentDescription = "Adicionar ${container.name}, ${formatMl(container.amountMl)}" }
                    ) {
                        Text("+${container.amountMl}", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(WellioIcons.Undo, contentDescription = "Desfazer último registro de água")
                }
            }

            Text(
                text = "DETALHES",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClickLabel = "Abrir detalhes de água") { onDetailsClick() }
                    .padding(vertical = 12.dp, horizontal = 4.dp)
            )
        }
    }
}

@Preview
@Composable
fun PreviewWaterCard() {
    WellioTheme {
        WaterCard(
            currentMl = 1400,
            goalMl = 2000,
            quickButtons = listOf(
                WaterContainer("copo", "Copo", 200, true, 0),
                WaterContainer("caneca", "Caneca", 300, true, 1),
                WaterContainer("garrafa", "Garrafa", 500, true, 2)
            ),
            canUndo = true,
            onQuickAdd = {},
            onUndo = {},
            onDetailsClick = {}
        )
    }
}
