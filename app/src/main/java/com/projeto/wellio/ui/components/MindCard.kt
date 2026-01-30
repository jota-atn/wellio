package com.projeto.wellio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MindCard(
    modifier: Modifier = Modifier,
    selectedMood: String? = null,
    totalStudyTime: String = "0h",
    goalStudyTime: String = "20h",
    onMoodSelected: (String) -> Unit = {},
    onChecklistClick: () -> Unit = {},
    onStudyClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF7C4DFF)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🧠", fontSize = 40.sp)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("MENTAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val emojis = listOf("😡", "😠", "😐", "🙂", "😄")

                        emojis.forEach { emoji ->
                            val isSelected = emoji == selectedMood

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.3f) else Color.Transparent
                                    )
                                    .clickable { onMoodSelected(emoji)}
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "HORAS DE ESTUDO: $totalStudyTime/$goalStudyTime",
                    color = Color.White,
                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier
                        .clickable { onChecklistClick() }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "CHECKLIST",
                        color = Color.White,
                        fontSize = 14.sp,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "📝",
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(0.dp))

                Text(
                    "ESTUDAR",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { onStudyClick() }
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewMindCard() {
    MindCard(selectedMood = "\uD83D\uDE04")
}