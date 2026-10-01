package com.projeto.wellio.ui.components

import androidx.compose.ui.graphics.Color
import com.projeto.wellio.domain.model.GoalType

// Cores provisórias por meta; vão para os tokens do tema no futuro (SPEC §2.5)
val GoalType.color: Color
    get() = when (this) {
        GoalType.WATER -> Color(0xFF2979FF)
        GoalType.EXERCISE -> Color(0xFFB0BEC5)
        GoalType.FOOD -> Color(0xFF66BB6A)
    }
