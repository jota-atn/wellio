package com.projeto.wellio.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.projeto.wellio.model.GoalType

@Composable
fun GoalSettingDialog(
    type: GoalType,
    currentDaily: Int,
    currentWeekly: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var dailyText by remember { mutableStateOf("") }
    var weeklyText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Definir Meta: ${type.title}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = type.color
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = dailyText,
                    onValueChange = { dailyText = it.filter { c -> c.isDigit() } },
                    label = { Text("Meta Diária (${type.unit})") },
                    placeholder = { Text("Atual: $currentDaily") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = weeklyText,
                    onValueChange = { weeklyText = it.filter { c -> c.isDigit() } },
                    label = { Text("Meta Semanal (${type.unit})") },
                    placeholder = { Text("Atual: $currentWeekly") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            val newDaily = dailyText.toIntOrNull() ?: currentDaily
                            val newWeekly = weeklyText.toIntOrNull() ?: currentWeekly

                            onConfirm(newDaily, newWeekly)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = type.color)
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}