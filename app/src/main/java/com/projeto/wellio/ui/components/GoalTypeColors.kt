package com.projeto.wellio.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.ui.theme.ModuleColors
import com.projeto.wellio.ui.theme.WellioTheme

/** Cores do módulo correspondente a cada meta. */
val GoalType.colors: ModuleColors
    @Composable @ReadOnlyComposable
    get() = when (this) {
        GoalType.WATER -> WellioTheme.colors.water
        GoalType.FOOD -> WellioTheme.colors.food
        GoalType.EXERCISE -> WellioTheme.colors.exercise
    }
