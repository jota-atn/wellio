package com.projeto.wellio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.projeto.wellio.domain.model.ExerciseType
import com.projeto.wellio.ui.icons.WellioIcon

@Composable
fun ExerciseDialog(
    exercises: List<ExerciseType>,
    onDismiss: () -> Unit,
    onConfirm: (ExerciseType, Int) -> Unit
) {
    val categories = remember(exercises) { exercises.map { it.category }.distinct().sorted() }
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull()) }

    var selectedExercise by remember { mutableStateOf<ExerciseType?>(null) }
    var minutesText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF5F5F7)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Registrar Treino", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2E7D32))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = category == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = category
                                selectedExercise = null
                            },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFC8E6C9),
                                selectedLabelColor = Color(0xFF1B5E20)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (selectedExercise == null) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filteredList = exercises.filter { it.category == selectedCategory }

                            items(filteredList) { exercise ->
                                Card(
                                    onClick = { selectedExercise = exercise },
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        WellioIcon(exercise.iconKey, size = 24.sp)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(exercise.name, fontWeight = FontWeight.Bold)
                                            Text("Intensidade: ${exercise.mets} METs", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            WellioIcon(selectedExercise!!.iconKey, size = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(selectedExercise!!.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                value = minutesText,
                                onValueChange = { minutesText = it.filter { char -> char.isDigit() } },
                                label = { Text("Duração (minutos)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    val mins = minutesText.toIntOrNull() ?: 0
                                    if (mins > 0) {
                                        onConfirm(selectedExercise!!, mins)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                enabled = minutesText.isNotEmpty()
                            ) {
                                Text("SALVAR TREINO")
                            }

                            TextButton(onClick = { selectedExercise = null }) {
                                Text("Voltar para lista", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}