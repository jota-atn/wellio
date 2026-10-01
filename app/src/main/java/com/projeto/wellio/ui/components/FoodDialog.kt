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
import com.projeto.wellio.domain.model.Food
import com.projeto.wellio.ui.icons.WellioIcon
import com.projeto.wellio.ui.theme.WellioTheme

@Composable
fun FoodDialog(
    foods: List<Food>,
    onDismiss: () -> Unit,
    onConfirm: (Food, Double) -> Unit
) {
    val colors = WellioTheme.colors.food
    val categories = remember(foods) { foods.map { it.category }.distinct().sorted() }
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull()) }
    var searchText by remember { mutableStateOf("") }

    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var quantityText by remember { mutableStateOf("1") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Registrar Alimento", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.onContainer)
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
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = colors.accent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (searchText.isEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { category ->
                            val isSelected = category == selectedCategory
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategory = category
                                    selectedFood = null
                                },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.container,
                                    selectedLabelColor = colors.onContainer
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
                                foods.filter { it.name.contains(searchText, ignoreCase = true) }
                            } else {
                                foods.filter { it.category == selectedCategory }
                            }

                            items(filteredList) { food ->
                                Card(
                                    onClick = { selectedFood = food },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        WellioIcon(food.iconKey, size = 24.sp)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(food.name, fontWeight = FontWeight.Bold)
                                            Text("${food.kcalPerPortion.toInt()} kcal / ${food.portionDescription}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            WellioIcon(selectedFood!!.iconKey, size = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(selectedFood!!.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Base: ${selectedFood!!.portionDescription}", color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { quantityText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Quantidade de Porções") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Text("Ex: 1 para uma porção inteira, 0.5 para meia", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    val qtd = quantityText.toDoubleOrNull() ?: 0.0
                                    if (qtd > 0) {
                                        onConfirm(selectedFood!!, qtd)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                                enabled = quantityText.isNotEmpty()
                            ) {
                                Text("ADICIONAR AO PRATO")
                            }

                            TextButton(onClick = { selectedFood = null }) {
                                Text("Voltar para lista", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}