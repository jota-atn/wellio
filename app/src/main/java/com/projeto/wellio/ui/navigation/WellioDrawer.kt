package com.projeto.wellio.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projeto.wellio.ui.icons.WellioIcon

private val recordItems = listOf("Água", "Alimentação", "Exercício", "Peso")
private val menuItems = listOf("Estatísticas", "Conquistas", "Anotações", "Receitas", "Metas", "Configurações")

/** Sidebar com a estrutura da SPEC §2.2; o cabeçalho de perfil e o painel rápido vêm depois. */
@Composable
fun WellioDrawerContent(onItemClick: (String) -> Unit) {
    ModalDrawerSheet {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    WellioIcon("profile", size = MaterialTheme.typography.headlineLarge.fontSize)
                }
                Spacer(Modifier.height(8.dp))
                Text("Wellio", style = MaterialTheme.typography.titleMedium)
            }

            DrawerSection("Registros", recordItems, onItemClick)
            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            menuItems.forEach { DrawerItem(it, onItemClick) }
        }
    }
}

@Composable
private fun DrawerSection(title: String, items: List<String>, onItemClick: (String) -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
    )
    items.forEach { DrawerItem(it, onItemClick) }
}

@Composable
private fun DrawerItem(label: String, onItemClick: (String) -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = false,
        onClick = { onItemClick(label) },
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}
