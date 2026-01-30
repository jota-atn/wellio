package com.projeto.wellio.model

data class ExerciseBlueprint(
    val id: String,
    val name: String,
    val category: String,
    val mets: Double,
    val icon: String
)

data class RegisteredExercise(
    val blueprint: ExerciseBlueprint,
    val minutes: Int,
    val caloriesBurned: Int
)

