package com.projeto.wellio.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.projeto.wellio.ui.icons.WellioIcon
import com.projeto.wellio.ui.module.Modules
import com.projeto.wellio.ui.module.WellioModule
import com.projeto.wellio.ui.theme.WellioTheme

private val menuItems = listOf("Estatísticas", "Conquistas", "Anotações", "Receitas", "Metas", "Configurações")

/**
 * Sidebar com a estrutura da SPEC §2.2; o cabeçalho de perfil e o painel rápido vêm depois.
 * A seção Registros sai dos módulos (cada um com ícone e cor).
 */
@Composable
fun WellioDrawerContent(onModuleClick: (WellioModule) -> Unit, onItemClick: (String) -> Unit) {
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

            Text(
                text = "Registros",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
            )
            Modules.all.forEach { module ->
                NavigationDrawerItem(
                    label = { Text(module.title) },
                    icon = {
                        Icon(
                            painterResource(module.icon),
                            contentDescription = null,
                            tint = module.colors(WellioTheme.colors).accent
                        )
                    },
                    selected = false,
                    onClick = { onModuleClick(module) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            menuItems.forEach { DrawerItem(it, onItemClick) }
        }
    }
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
