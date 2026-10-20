package com.projeto.wellio.ui.onboarding

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.projeto.wellio.WellioApp
import com.projeto.wellio.data.prefs.GoalsStore
import com.projeto.wellio.data.prefs.ProfilePhotoStore
import com.projeto.wellio.data.prefs.ProfileStore
import com.projeto.wellio.data.repository.WeightRepository
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.profile.OnboardingDraft
import com.projeto.wellio.domain.weight.WeightGoal
import com.projeto.wellio.domain.weight.latestWeight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock
import java.time.LocalDate

/**
 * Onboarding (SPEC §5.0): monta o rascunho com o que já existe e só grava no Concluir.
 * Quem já tem pesagens ou metas salvas conta como quem já usa o app.
 */
class OnboardingViewModel(
    private val profileStore: ProfileStore,
    private val photoStore: ProfilePhotoStore,
    private val goalsStore: GoalsStore,
    private val weightRepository: WeightRepository,
    private val clock: Clock
) : ViewModel() {

    val today: LocalDate get() = LocalDate.now(clock)

    private val _draft = MutableStateFlow<OnboardingDraft?>(null)
    /** null enquanto carrega. */
    val draft: StateFlow<OnboardingDraft?> = _draft

    private var initialWeightKg: Double? = null
    private var initialTarget: WeightGoal? = null

    init {
        viewModelScope.launch {
            val profile = profileStore.profile.first()
            val weight = latestWeight(weightRepository.entries.first())?.weightKg
            val target = goalsStore.weightGoal.first()
            val goals = goalsStore.goals.first()
            initialWeightKg = weight
            initialTarget = target
            _draft.value = OnboardingDraft(
                returning = weight != null || goalsStore.hasSavedGoals(),
                name = profile.name,
                photoFile = profile.photoFile,
                birthDate = profile.birthDate,
                sex = profile.sex,
                heightCm = profile.heightCm,
                weightKg = weight,
                activity = profile.activity,
                targetKg = target?.targetKg,
                waterMl = goals.water.daily,
                kcal = goals.food.daily,
                exerciseUnit = goals.exerciseUnit,
                exercise = goals.exercise.daily
            )
        }
    }

    fun edit(transform: (OnboardingDraft) -> OnboardingDraft) {
        _draft.update { it?.let(transform) }
    }

    /** O recorte da foto é salvo na hora; o perfil só passa a usá-lo no Concluir. */
    fun setPhoto(cropped: Bitmap) {
        viewModelScope.launch {
            val name = photoStore.save { cropped.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            edit { it.copy(photoFile = name) }
        }
    }

    fun photo(name: String?): File? = name?.let(photoStore::file)

    /** Pular o estilo de vida: volta ao que era antes. */
    fun resetLifestyle() {
        edit { it.copy(activity = null, targetKg = initialTarget?.targetKg) }
    }

    /** Grava tudo; [saveGoals] = false quando as metas foram puladas. */
    fun finish(saveGoals: Boolean) {
        val draft = _draft.value ?: return
        viewModelScope.launch {
            draft.weightKg?.let { kg -> if (kg != initialWeightKg) weightRepository.save(today, kg) }
            val target = draft.targetKg
            if (target != initialTarget?.targetKg) {
                // Alvo novo começa a barra de progresso no peso de agora (SPEC §6.1)
                goalsStore.setWeightGoal(target?.let { WeightGoal(it, initialTarget?.startKg ?: draft.weightKg ?: it) })
            }
            if (saveGoals) {
                val goals = goalsStore.goals.first()
                goalsStore.setGoal(GoalType.WATER, goals.water.copy(daily = draft.waterMl))
                goalsStore.setGoal(GoalType.FOOD, goals.food.copy(daily = draft.kcal))
                goalsStore.setExerciseUnit(draft.exerciseUnit)
                val exercise = goalsStore.goals.first().exercise
                goalsStore.setGoal(GoalType.EXERCISE, exercise.copy(daily = draft.exercise))
            }
            if (draft.photoFile == null) photoStore.clear()
            // Por último: concluir o onboarding troca a tela para a Home
            profileStore.save(draft.profile())
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WellioApp).container
                OnboardingViewModel(
                    container.profileStore,
                    container.profilePhotoStore,
                    container.goalsStore,
                    container.weightRepository,
                    container.clock
                )
            }
        }
    }
}
