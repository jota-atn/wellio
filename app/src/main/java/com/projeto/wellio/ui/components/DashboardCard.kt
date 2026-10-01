package com.projeto.wellio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projeto.wellio.model.GoalType

@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    onAddMetaClick: (GoalType) -> Unit = {},
    onDetailsClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF152259)),
        elevation = CardDefaults.cardElevation(0.dp)

    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(0.7f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DASHBOARD",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(60.dp)
                ) {
                    Box(modifier = Modifier.width(16.dp).fillMaxHeight(0.4f).clip(RoundedCornerShape(4.dp)).background(Color(0xFF536DFE)))
                    Box(modifier = Modifier.width(16.dp).fillMaxHeight(0.7f).clip(RoundedCornerShape(4.dp)).background(Color(0xFF536DFE)))
                    Box(modifier = Modifier.width(16.dp).fillMaxHeight(1.0f).clip(RoundedCornerShape(4.dp)).background(Color(0xFF8C9EFF)))
                    Box(modifier = Modifier.width(16.dp).fillMaxHeight(0.5f).clip(RoundedCornerShape(4.dp)).background(Color(0xFF536DFE)))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1.3f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "METAS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    MetaItem("ÁGUA", GoalType.WATER.color) { onAddMetaClick(GoalType.WATER) }
                    MetaItem("ALIMENTAÇÃO", GoalType.FOOD.color) { onAddMetaClick(GoalType.FOOD) }
                    MetaItem("EXERCÍCIOS", GoalType.EXERCISE.color) { onAddMetaClick(GoalType.EXERCISE) }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "DETALHES",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .clickable { onDetailsClick() }
                    )
                }
            }
        }
    }
}

@Composable
fun MetaItem(
    label: String, color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFFC5CAE9),
            fontSize = 11.sp
        )

        Surface(
            modifier = Modifier
                .size(18.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(4.dp),
            color = color
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Definir",
                tint = Color.White,
                modifier = Modifier.padding(2.dp)
            )
        }
    }
}

@Preview
@Composable
fun PreviewDashboard() {
    DashboardCard()
}