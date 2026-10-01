package com.projeto.wellio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.projeto.wellio.model.FoodBlueprint
import com.projeto.wellio.model.FoodRepository

@Composable
fun FoodDialog(
    onDismiss: () -> Unit,
    onConfirm: (FoodBlueprint, Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(FoodRepository.categories.first()) }
    var searchText by remember { mutableStateOf("") }

    var selectedFood by remember { mutableStateOf<FoodBlueprint?>(null) }
    var quantityText by remember { mutableStateOf("1") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFFF8E1)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Registrar Alimento", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFFF57C00))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        selectedFood = null
                    },
                    placeholder = { Text("Buscar (ex: Arroz)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFF57C00)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (searchText.isEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FoodRepository.categories) { category ->
                            val isSelected = category == selectedCategory
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategory = category
                                    selectedFood = null
                                },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFCC80),
                                    selectedLabelColor = Color(0xFFE65100)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (selectedFood == null) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val filteredList = if (searchText.isNotBlank()) {
                                FoodRepository.list.filter { it.name.contains(searchText, ignoreCase = true) }
                            } else {
                                FoodRepository.list.filter { it.category == selectedCategory }
                            }

                            items(filteredList) { food ->
                                Card(
                                    onClick = { selectedFood = food },
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(food.icon, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(food.name, fontWeight = FontWeight.Bold)
                                            Text("${food.kcal.toInt()} kcal / ${food.portionDescription}", fontSize = 12.sp, color = Color.Gray)
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
                            Text(selectedFood!!.icon, fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(selectedFood!!.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Base: ${selectedFood!!.portionDescription}", color = Color.Gray)

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { quantityText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Quantidade de Porções") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Text("Ex: 1 para uma porção inteira, 0.5 para meia", fontSize = 12.sp, color = Color.Gray)

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    val qtd = quantityText.toDoubleOrNull() ?: 0.0
                                    if (qtd > 0) {
                                        onConfirm(selectedFood!!, qtd)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
                                enabled = quantityText.isNotEmpty()
                            ) {
                                Text("ADICIONAR AO PRATO")
                            }

                            TextButton(onClick = { selectedFood = null }) {
                                Text("Voltar para lista", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}