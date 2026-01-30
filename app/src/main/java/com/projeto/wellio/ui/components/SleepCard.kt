package com.projeto.wellio.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SleepCard(
    modifier: Modifier = Modifier,
    bedTime: String = "22:00",
    wakeTime: String = "06:00",
    sleepDuration: String = "8h",
    onBedTimeClick: () -> Unit = {},
    onWakeTimeClick: () -> Unit = {},
    onDetailClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8DEFA)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "😴",
                fontSize = 40.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = "SONO",
                    color = Color(0xFF7E57C2),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy((-12).dp)
                    ) {
                        SleepInputRow(label = "DORMIR", time = bedTime, onClick = onBedTimeClick)
                        SleepInputRow(label = "ACORDAR", time = wakeTime, onClick = onWakeTimeClick)
                    }


                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "TEMPO DE SONO",
                            color = Color(0xFF9575CD),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = sleepDuration,
                            color = Color(0xFF4527A0),
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            lineHeight = 28.sp
                        )

                        Spacer(modifier = Modifier.height(0.dp))

                        Text(
                            text = "DETALHES",
                            color = Color(0xFF7E57C2),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onDetailClick() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SleepInputRow(label: String, time: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 0.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF7E57C2),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(75.dp)
        )

        Surface(
            onClick = onClick,
            color = Color(0xFFF3E5F5),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFD1C4E9))
        ) {
            Text(
                text = time,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = Color(0xFF5E35B1),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Preview
@Composable
fun PreviewSleepCardFixed() {
    SleepCard()
}