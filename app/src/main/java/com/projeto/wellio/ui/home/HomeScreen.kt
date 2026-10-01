package com.projeto.wellio.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.projeto.wellio.ui.components.DashboardCard
import com.projeto.wellio.ui.components.DashboardDetailsDialog
import com.projeto.wellio.ui.components.ExerciseDialog
import com.projeto.wellio.ui.components.FoodCard
import com.projeto.wellio.ui.components.FoodDialog
import com.projeto.wellio.ui.components.GoalSettingDialog
import com.projeto.wellio.ui.components.TrainCard
import com.projeto.wellio.ui.components.WaterCard
import com.projeto.wellio.model.GoalType

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    var goalBeingEdited by remember { mutableStateOf<GoalType?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    var showFoodDialog by remember { mutableStateOf(false) }
    var showExerciseDialog by remember { mutableStateOf(false) }

    fun dailyGoal(type: GoalType) = viewModel.goals[type]?.first ?: 0

    Scaffold(containerColor = Color(0xFFF5F5F7)) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                onAddMetaClick = { goalBeingEdited = it },
                onDetailsClick = { showDetails = true }
            )

            WaterCard(
                currentMl = viewModel.waterCurrentMl,
                goalMl = dailyGoal(GoalType.WATER),
                onAddWater = viewModel::addWater
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FoodCard(
                    modifier = Modifier.weight(1f),
                    ingestedKcal = viewModel.totalCaloriesConsumed,
                    goalKcal = dailyGoal(GoalType.FOOD),
                    onRegisterClick = { showFoodDialog = true }
                )
                TrainCard(
                    modifier = Modifier.weight(1f),
                    burnedKcal = viewModel.getTotalCaloriesBurned,
                    goalKcal = dailyGoal(GoalType.EXERCISE),
                    onRegisterClick = { showExerciseDialog = true }
                )
            }
        }
    }

    goalBeingEdited?.let { type ->
        val (daily, weekly) = viewModel.goals[type] ?: (0 to 0)
        GoalSettingDialog(
            type = type,
            currentDaily = daily,
            currentWeekly = weekly,
            onDismiss = { goalBeingEdited = null },
            onConfirm = { newDaily, newWeekly ->
                viewModel.updateGoal(type, newDaily, newWeekly)
                goalBeingEdited = null
            }
        )
    }

    if (showDetails) {
        DashboardDetailsDialog(
            onDismiss = { showDetails = false },
            getDataForType = { type ->
                val (daily, weekly) = viewModel.goals[type] ?: (0 to 0)
                Triple(viewModel.getCurrentProgress(type), daily, weekly)
            }
        )
    }

    if (showFoodDialog) {
        FoodDialog(
            onDismiss = { showFoodDialog = false },
            onConfirm = { food, quantity ->
                viewModel.addFood(food, quantity)
                showFoodDialog = false
            }
        )
    }

    if (showExerciseDialog) {
        ExerciseDialog(
            onDismiss = { showExerciseDialog = false },
            onConfirm = { exercise, minutes ->
                viewModel.addExercise(exercise, minutes)
                showExerciseDialog = false
            }
        )
    }
}
