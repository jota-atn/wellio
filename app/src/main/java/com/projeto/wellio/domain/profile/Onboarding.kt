package com.projeto.wellio.domain.profile

import com.projeto.wellio.domain.model.ActivityLevel
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.model.Profile
import com.projeto.wellio.domain.model.Sex
import com.projeto.wellio.domain.water.suggestedWaterGoal
import com.projeto.wellio.domain.weight.WeightGoal
import java.time.LocalDate

/** Etapas do onboarding no M2a (SPEC §5.0); a de lembretes chega no M3. */
enum class OnboardingStep {
    Welcome, You, Body, Lifestyle, Goals;

    /** Estilo de vida e metas podem ser pulados (ficam os valores de antes). */
    val skippable: Boolean get() = this == Lifestyle || this == Goals
}

/**
 * O que o onboarding vai preenchendo, salvo só no fim. Quem já usa o app ([returning])
 * começa com o que já existe; quem é novo, com as metas sugeridas pelas etapas anteriores.
 */
data class OnboardingDraft(
    val returning: Boolean,
    val name: String = "",
    val photoFile: String? = null,
    val birthDate: LocalDate? = null,
    val sex: Sex? = null,
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val activity: ActivityLevel? = null,
    val targetKg: Double? = null,
    val waterMl: Int = Goals.Default.water.daily,
    val kcal: Int = Goals.Default.food.daily,
    val exerciseUnit: ExerciseUnit = Goals.Default.exerciseUnit,
    val exercise: Int = Goals.Default.exercise.daily,
    /** As metas já foram preenchidas (pelas sugestões ou por quem já usa o app): não sobrescrever. */
    val goalsReady: Boolean = returning
) {
    /** Nome e peso são obrigatórios (SPEC §5.0). */
    fun canContinue(step: OnboardingStep): Boolean = when (step) {
        OnboardingStep.You -> name.isNotBlank()
        OnboardingStep.Body -> weightKg != null
        else -> true
    }

    /** Primeira etapa obrigatória sem preencher, para onde o Concluir leva; null se está tudo certo. */
    val firstIncomplete: OnboardingStep?
        get() = OnboardingStep.entries.firstOrNull { !canContinue(it) }

    val objective: Objective get() = objective(targetKg?.let { WeightGoal(it, weightKg ?: it) }, weightKg)

    fun suggestion(today: LocalDate): CalorieSuggestion = suggestedCalories(profile(), weightKg, objective, today)

    /** Sugestão de água pelo peso, se houver. */
    val waterSuggestion: Int? get() = weightKg?.let(::suggestedWaterGoal)

    /** Ao chegar nas metas pela primeira vez, quem é novo recebe as sugestões. */
    fun withSuggestedGoals(today: LocalDate): OnboardingDraft {
        if (goalsReady) return this
        val kcal = (suggestion(today) as? CalorieSuggestion.Ready)?.kcal ?: kcal
        return copy(waterMl = waterSuggestion ?: waterMl, kcal = kcal, goalsReady = true)
    }

    fun profile(): Profile = Profile(
        name = name.trim(),
        photoFile = photoFile,
        birthDate = birthDate,
        sex = sex,
        heightCm = heightCm,
        activity = activity,
        onboardingDone = true
    )
}
