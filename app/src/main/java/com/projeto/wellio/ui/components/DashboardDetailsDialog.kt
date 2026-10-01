package com.projeto.wellio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.projeto.wellio.domain.goals.progress
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.ui.theme.WellioTheme

@Composable
fun DashboardDetailsDialog(
    onDismiss: () -> Unit,
    getDataForType: (GoalType) -> Triple<Int, Int, Int>,
    getWeekTotal: (GoalType) -> Int
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            color = WellioTheme.colors.base.container
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Visão Geral", color = WellioTheme.colors.base.onContainer, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = WellioTheme.colors.base.onContainer)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    GoalType.entries.forEach { type ->
                        val (current, dailyGoal, weeklyGoal) = getDataForType(type)
                        DetailItem(type, current, dailyGoal, getWeekTotal(type), weeklyGoal)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(type: GoalType, current: Int, dailyGoal: Int, weekTotal: Int, weeklyGoal: Int) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(type.colors.accent, shape = RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(type.title, color = WellioTheme.colors.base.onContainer, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        ProgressRow(label = "Diário", current = current, max = dailyGoal, color = type.colors.accent, unit = type.unit)

        Spacer(modifier = Modifier.height(4.dp))

        ProgressRow(label = "Semanal", current = weekTotal, max = weeklyGoal, color = type.colors.accent.copy(alpha = 0.6f), unit = type.unit)
    }
}

@Composable
fun ProgressRow(label: String, current: Int, max: Int, color: Color, unit: String) {
    val progress = progress(current, max)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = WellioTheme.colors.base.onContainer.copy(alpha = 0.75f), fontSize = 10.sp, modifier = Modifier.width(50.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(WellioTheme.colors.base.track)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            "${(progress * 100).toInt()}%",
            color = WellioTheme.colors.base.onContainer,
            fontSize = 10.sp,
            modifier = Modifier.width(30.dp)
        )
    }
}