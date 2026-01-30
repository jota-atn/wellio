package com.projeto.wellio.model

data class FoodBlueprint(
    val id: String,
    val name: String,
    val category: String,
    val kcal: Double,
    val portionDescription: String = "100g",
    val icon: String
)

data class EatenFood(
    val blueprint: FoodBlueprint,
    val quantity: Double,
    val totalCalories: Int
)