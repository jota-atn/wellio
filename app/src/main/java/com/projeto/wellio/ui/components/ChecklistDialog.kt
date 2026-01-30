package com.projeto.wellio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.projeto.wellio.model.Task

@Composable
fun ChecklistDialog(
    tasks: List<Task>,
    onDismiss: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onAddTask: (String) -> Unit
) {
    var newTaskText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // O CARD DO DIALOG
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.85f)
                    .padding(vertical = 24.dp)
                    .imePadding()
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFF5F5F7),
                contentColor = Color.Black
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Checklist Diário",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C4DFF)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Gray)
                        }
                    }

                    HorizontalDivider(color = Color.LightGray)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (tasks.isEmpty()) {
                            Text(
                                "Nenhuma tarefa hoje :)",
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                items(tasks
                                ) { task ->
                                    TaskItem(
                                        task = task,
                                        onToggle = { onToggleTask(task) },
                                        onDelete = { onDeleteTask(task) }
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        color = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    if (newTaskText.isNotBlank()) {
                                        onAddTask(newTaskText)
                                        newTaskText = ""
                                    }
                                },
                                modifier = Modifier
                                    .background(Color(0xFF7C4DFF), shape = RoundedCornerShape(12.dp))
                                    .size(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            OutlinedTextField(
                                value = newTaskText,
                                onValueChange = { newTaskText = it },
                                placeholder = { Text("Nova tarefa...", color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color(0xFFF5F5F7),
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedBorderColor = Color(0xFF7C4DFF),
                                    unfocusedBorderColor = Color.LightGray
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (newTaskText.isNotBlank()) {
                                        onAddTask(newTaskText)
                                        newTaskText = ""
                                    }
                                })
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = task.isDone,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF7C4DFF),
                        checkmarkColor = Color.White,
                        uncheckedColor = Color.Gray
                    )
                )

                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    color = if (task.isDone) Color.Gray else Color.Black,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { onToggle() }
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Apagar",
                    tint = Color(0xFFFFCDD2),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}