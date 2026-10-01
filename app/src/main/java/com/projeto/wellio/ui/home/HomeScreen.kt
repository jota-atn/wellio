package com.projeto.wellio.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projeto.wellio.domain.model.ExerciseType
import com.projeto.wellio.domain.model.Food
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.ui.components.DashboardCard
import com.projeto.wellio.ui.components.DashboardDetailsDialog
import com.projeto.wellio.ui.components.ExerciseDialog
import com.projeto.wellio.ui.components.FoodCard
import com.projeto.wellio.ui.components.FoodDialog
import com.projeto.wellio.ui.components.GoalSettingDialog
import com.projeto.wellio.ui.components.TrainCard
import com.projeto.wellio.ui.components.WaterCard

@Composable
fun HomeRoute(
    onMenuClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onMenuClick = onMenuClick,
        onAddWater = viewModel::addWater,
        onAddFood = viewModel::addFood,
        onAddExercise = viewModel::addExercise,
        onUpdateGoal = viewModel::updateGoal
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onMenuClick: () -> Unit,
    onAddWater: (Int) -> Unit,
    onAddFood: (Food, Double) -> Unit,
    onAddExercise: (ExerciseType, Int) -> Unit,
    onUpdateGoal: (GoalType, Int, Int) -> Unit
) {
    var goalBeingEdited by remember { mutableStateOf<GoalType?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    var showFoodDialog by remember { mutableStateOf(false) }
    var showExerciseDialog by remember { mutableStateOf(false) }

    Scaffold(containerColor = Color(0xFFF5F5F7)) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Abrir menu")
            }

            DashboardCard(
                onAddMetaClick = { goalBeingEdited = it },
                onDetailsClick = { showDetails = true }
            )

            WaterCard(
                currentMl = state.today.waterMl,
                goalMl = state.goals.water.daily,
                onAddWater = onAddWater
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FoodCard(
                    modifier = Modifier.weight(1f),
                    ingestedKcal = state.today.kcalConsumed,
                    goalKcal = state.goals.food.daily,
                    onRegisterClick = { showFoodDialog = true }
                )
                TrainCard(
                    modifier = Modifier.weight(1f),
                    burnedKcal = state.today.kcalBurned,
                    goalKcal = state.goals.exercise.daily,
                    onRegisterClick = { showExerciseDialog = true }
                )
            }
        }
    }

    goalBeingEdited?.let { type ->
        val goal = state.goals[type]
        GoalSettingDialog(
            type = type,
            currentDaily = goal.daily,
            currentWeekly = goal.weekly,
            onDismiss = { goalBeingEdited = null },
            onConfirm = { newDaily, newWeekly ->
                onUpdateGoal(type, newDaily, newWeekly)
                goalBeingEdited = null
            }
        )
    }

    if (showDetails) {
        DashboardDetailsDialog(
            onDismiss = { showDetails = false },
            getDataForType = { type ->
                val goal = state.goals[type]
                Triple(state.today[type], goal.daily, goal.weekly)
            },
            getWeekTotal = { type -> state.week[type] }
        )
    }

    if (showFoodDialog) {
        FoodDialog(
            foods = state.foods,
            onDismiss = { showFoodDialog = false },
            onConfirm = { food, quantity ->
                onAddFood(food, quantity)
                showFoodDialog = false
            }
        )
    }

    if (showExerciseDialog) {
        ExerciseDialog(
            exercises = state.exerciseTypes,
            onDismiss = { showExerciseDialog = false },
            onConfirm = { exercise, minutes ->
                onAddExercise(exercise, minutes)
                showExerciseDialog = false
            }
        )
    }
}
