package com.projeto.wellio.domain.model

data class Food(
    val id: String,
    val name: String,
    val category: String,
    val kcalPerPortion: Double,
    val portionDescription: String,
    val iconKey: String
)
