package com.projeto.wellio.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.projeto.wellio.ui.home.HomeRoute
import com.projeto.wellio.ui.water.WaterRoute
import com.projeto.wellio.ui.weight.WeightRoute
import kotlinx.coroutines.launch

@Composable
fun WellioNavHost() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            WellioDrawerContent(onItemClick = { title ->
                scope.launch { drawerState.close() }
                navController.navigate(destinationFor(title))
            })
        }
    ) {
        NavHost(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            navController = navController,
            startDestination = HomeDestination,
            enterTransition = enterFromRight,
            exitTransition = exitToLeft,
            popEnterTransition = popEnterFromLeft,
            popExitTransition = popExitToRight,
            // Gesto de voltar (voltar preditivo): mesma animação do voltar, acompanhando o dedo,
            // em vez do padrão da biblioteca, que encolhe a tela num cartão
            predictivePopEnterTransition = { _ -> popEnterFromLeft() },
            predictivePopExitTransition = { _ -> popExitToRight() }
        ) {
            composable<HomeDestination> {
                HomeRoute(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onOpenWater = { navController.navigate(WaterDestination) }
                )
            }
            composable<WaterDestination> {
                WaterRoute(onBack = { navController.popBackStack() })
            }
            composable<WeightDestination> {
                WeightRoute(onBack = { navController.popBackStack() })
            }
            composable<ComingSoonDestination> { entry ->
                ComingSoonScreen(
                    title = entry.toRoute<ComingSoonDestination>().title,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/** Item da sidebar → destino; o que ainda não existe cai em "Em breve". */
private fun destinationFor(title: String): Any = when (title) {
    "Água" -> WaterDestination
    "Peso" -> WeightDestination
    else -> ComingSoonDestination(title)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComingSoonScreen(title: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
            Text("Em breve")
        }
    }
}
