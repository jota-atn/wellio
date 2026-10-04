package com.projeto.wellio.ui.icons

import androidx.annotation.DrawableRes
import com.projeto.wellio.R
import com.projeto.wellio.domain.exercise.ExerciseCategory
import com.projeto.wellio.domain.model.ExerciseType

/** Desenho de cada categoria de exercício (fonte em tools/icons/icones.py). */
@DrawableRes
fun exerciseCategoryIconRes(category: ExerciseCategory): Int = when (category) {
    ExerciseCategory.Cardio -> R.drawable.ic_exercise_cardio
    ExerciseCategory.Strength -> R.drawable.ic_exercise_strength
    ExerciseCategory.Fight -> R.drawable.ic_exercise_fight
    ExerciseCategory.Sports -> R.drawable.ic_exercise_sports
    ExerciseCategory.Water -> R.drawable.ic_exercise_water
    ExerciseCategory.Zen -> R.drawable.ic_exercise_zen
    ExerciseCategory.Other -> R.drawable.ic_exercise_other
}

/** Cada atividade usa o ícone da sua categoria (SPEC §8.0). */
@DrawableRes
fun exerciseIconRes(type: ExerciseType): Int = exerciseCategoryIconRes(ExerciseCategory.of(type.category))
