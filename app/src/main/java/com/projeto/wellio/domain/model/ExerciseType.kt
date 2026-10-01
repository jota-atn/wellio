package com.projeto.wellio.domain.model

data class ExerciseType(
    val id: String,
    val name: String,
    val category: String,
    val mets: Double,
    val iconKey: String
)
