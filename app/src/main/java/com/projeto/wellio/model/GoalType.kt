package com.projeto.wellio.model

import androidx.compose.ui.graphics.Color

enum class GoalType(val title: String, val unit: String, val color: Color) {
    WATER("Água", "ml", Color(0xFF2979FF)),
    EXERCISE("Exercícios", "kcal", Color(0xFFB0BEC5)),
    FOOD("Alimentação", "kcal", Color(0xFF66BB6A))
}
