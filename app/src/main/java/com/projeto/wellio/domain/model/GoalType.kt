package com.projeto.wellio.domain.model

enum class GoalType(val title: String, val unit: String) {
    WATER("Água", "ml"),
    EXERCISE("Exercícios", "kcal"),
    FOOD("Alimentação", "kcal")
}
