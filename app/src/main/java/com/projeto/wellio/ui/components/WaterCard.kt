package com.projeto.wellio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.projeto.wellio.domain.goals.progress
import com.projeto.wellio.ui.theme.WellioTheme

@Composable
fun WaterCard(
    modifier: Modifier = Modifier,
    currentMl: Int = 0,
    goalMl: Int = 2000,
    onAddWater: (Int) -> Unit = {}
) {
    val colors = WellioTheme.colors.water
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.container),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "\uD83D\uDCA7", fontSize = 32.sp)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = "ÁGUA",
                    color = colors.onContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 0.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompactTextButton(text = "-500", onClick = { onAddWater(-500) })
                    CompactTextButton(text = "-300", onClick = { onAddWater(-300) })
                    CompactTextButton(text = "-200", onClick = { onAddWater(-200) })

                    Spacer(modifier = Modifier.width(8.dp))

                    CompactTextButton(text = "+200", onClick = { onAddWater(200) })
                    Spacer(modifier = Modifier.width(8.dp))
                    CompactTextButton(text = "+300", onClick = { onAddWater(300) })
                    Spacer(modifier = Modifier.width(8.dp))
                    CompactTextButton(text = "+500", onClick = { onAddWater(500) })
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 3. BARRA DE PROGRESSO
                val progress = progress(currentMl, goalMl)
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.track)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(colors.accent)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = if (progress > 0.5f) colors.onAccent else colors.onContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 4. DETALHES
                Text(
                    text = "DETALHES",
                    color = colors.onContainer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Ação futura */ }
                )
            }
        }
    }
}

@Composable
fun CompactTextButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .height(20.dp)
            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
    ) {
        Text(
            text = text,
            color = WellioTheme.colors.water.onContainer,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview
@Composable
fun PreviewWaterCard() {
    WaterCard(currentMl = 800, goalMl = 2000)
}