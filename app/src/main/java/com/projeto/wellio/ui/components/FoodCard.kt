package com.projeto.wellio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projeto.wellio.ui.theme.WellioTheme

@Composable
fun FoodCard(
    modifier: Modifier = Modifier,
    ingestedKcal: Int = 1500,
    goalKcal: Int = 1200,
    onRegisterClick: () -> Unit = {}
) {
    val colors = WellioTheme.colors.food
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.container),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🍎", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ALIMENTAÇÃO",
                    color = colors.onContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("CALORIAS INGERIDAS", fontSize = 12.sp, color = colors.onContainer)
            Text(
                text = "$ingestedKcal/$goalKcal",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "CADASTRAR",
                color = colors.onContainer,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRegisterClick() }
            )
        }
    }
}

@Preview
@Composable
fun PreviewFoodCard() {
    FoodCard()
}