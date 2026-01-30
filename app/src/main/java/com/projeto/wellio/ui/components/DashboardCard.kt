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

@Composable
fun DashboardCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF152259)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp) // Mantive compacto (16dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // --- COLUNA DA ESQUERDA (Dashboard + Gráfico) ---
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

                // Gráfico (Altura 30dp para ser compacto)
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

            // --- COLUNA DA DIREITA (Metas em Grid) ---
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

                // Grid de 2 Colunas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Top // Garante alinhamento no topo
                ) {
                    // Coluna 1 (3 Itens)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MetaItem("ÁGUA", Color(0xFF2979FF))
                        MetaItem("SONO", Color(0xFF536DFE))
                        MetaItem("EXERCÍCIOS", Color(0xFFB0BEC5))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Coluna 2 (2 Itens + Detalhes no final)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End // Alinha o texto detalhes à direita
                    ) {
                        MetaItem("ALIMENTAÇÃO", Color(0xFF66BB6A))
                        MetaItem("ESTUDO", Color(0xFFFFCA28))

                        // O "Detalhes" entra aqui, ocupando o espaço do 3º item
                        Spacer(modifier = Modifier.height(2.dp)) // Pequeno ajuste fino
                        Text(
                            text = "DETALHES",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .clickable { /* Ação */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetaItem(label: String, color: Color) {
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
                .clickable { /* Logica de definir meta */ },
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