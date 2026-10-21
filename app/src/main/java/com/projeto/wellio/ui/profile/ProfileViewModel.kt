package com.projeto.wellio.ui.profile

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.projeto.wellio.WellioApp
import com.projeto.wellio.data.db.JourneyDao
import com.projeto.wellio.data.prefs.GoalsStore
import com.projeto.wellio.data.prefs.ProfilePhotoStore
import com.projeto.wellio.data.prefs.ProfileStore
import com.projeto.wellio.data.repository.WeightRepository
import com.projeto.wellio.data.time.todayFlow
import com.projeto.wellio.domain.model.Goal
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.model.Goals
import com.projeto.wellio.domain.model.Profile
import com.projeto.wellio.domain.profile.BmiRange
import com.projeto.wellio.domain.profile.CalorieSuggestion
import com.projeto.wellio.domain.profile.Objective
import com.projeto.wellio.domain.profile.age
import com.projeto.wellio.domain.profile.bmi
import com.projeto.wellio.domain.profile.objective
import com.projeto.wellio.domain.profile.suggestedCalories
import com.projeto.wellio.domain.weight.DEFAULT_WEIGHT_KG
import com.projeto.wellio.domain.weight.WeightGoal
import com.projeto.wellio.domain.weight.latestWeight
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ProfileUiState(
    val today: LocalDate,
    val profile: Profile = Profile(),
    /** Recorte salvo da foto, se houver. */
    val photo: File? = null,
    /** Foto da capa, se houver (senão vale a cor). */
    val coverPhoto: File? = null,
    /** Pesagem mais recente; null sem pesagens. */
    val weightKg: Double? = null,
    val weightGoal: WeightGoal? = null,
    val foodGoal: Goal = Goals.Default.food,
    /** Os dados já chegaram (antes disso, o perfil vazio não quer dizer onboarding pendente). */
    val loaded: Boolean = false,
    /** Dia do primeiro registro e quantos registros há (linha "No Wellio há N dias"). */
    val firstRecord: LocalDate? = null,
    val records: Int = 0
) {
    /** Dias desde o primeiro registro, contando hoje; null sem registros. */
    val daysInApp: Int? get() = firstRecord?.let { ChronoUnit.DAYS.between(it, today).toInt() + 1 }

    val age: Int? get() = profile.birthDate?.let { age(it, today) }
    val bmi: Double? get() = if (weightKg != null && profile.heightCm != null) bmi(weightKg, profile.heightCm) else null
    val bmiRange: BmiRange? get() = bmi?.let(BmiRange::of)
    val objective: Objective get() = objective(weightGoal, weightKg)
    val suggestion: CalorieSuggestion get() = suggestedCalories(profile, weightKg, objective, today)
}

class ProfileViewModel(
    private val profileStore: ProfileStore,
    private val photoStore: ProfilePhotoStore,
    private val coverStore: ProfilePhotoStore,
    private val goalsStore: GoalsStore,
    weightRepository: WeightRepository,
    journeyDao: JourneyDao,
    clock: Clock
) : ViewModel() {

    private val initialDay = LocalDate.now(clock)

    val uiState: StateFlow<ProfileUiState> = combine(
        todayFlow(clock),
        profileStore.profile,
        weightRepository.entries,
        goalsStore.weightGoal,
        combine(goalsStore.goals, journeyDao.journey(), ::Pair)
    ) { today, profile, weights, weightGoal, (goals, journey) ->
        ProfileUiState(
            today = today,
            profile = profile,
            photo = profile.photoFile?.let(photoStore::file),
            coverPhoto = profile.coverPhoto?.let(coverStore::file),
            weightKg = latestWeight(weights)?.weightKg,
            weightGoal = weightGoal,
            foodGoal = goals.food,
            loaded = true,
            firstRecord = journey.firstDate,
            records = journey.records
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState(initialDay))

    fun update(transform: (Profile) -> Profile) {
        viewModelScope.launch { profileStore.save(transform(uiState.value.profile)) }
    }

    /** Salva o recorte da foto (SPEC §9.1) e passa a usá-lo no perfil. */
    fun setPhoto(cropped: Bitmap) {
        viewModelScope.launch {
            val name = photoStore.save { cropped.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            profileStore.save(uiState.value.profile.copy(photoFile = name))
        }
    }

    fun removePhoto() {
        viewModelScope.launch {
            profileStore.save(uiState.value.profile.copy(photoFile = null))
            photoStore.clear()
        }
    }

    /** Capa numa cor do app: a foto da capa, se houver, sai. */
    fun setCoverColor(color: CoverColor) {
        viewModelScope.launch {
            profileStore.save(uiState.value.profile.copy(coverColor = color.name, coverPhoto = null))
            coverStore.clear()
        }
    }

    fun setCoverPhoto(cropped: Bitmap) {
        viewModelScope.launch {
            val name = coverStore.save { cropped.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            profileStore.save(uiState.value.profile.copy(coverPhoto = name))
        }
    }

    fun resetCover() {
        viewModelScope.launch {
            profileStore.save(uiState.value.profile.copy(coverColor = null, coverPhoto = null))
            coverStore.clear()
        }
    }

    /** "Usar": a sugestão vira a meta diária de kcal; a semanal própria, se houver, fica. */
    fun useSuggestion(kcal: Int) {
        viewModelScope.launch { goalsStore.setGoal(GoalType.FOOD, uiState.value.foodGoal.copy(daily = kcal)) }
    }

    /** Mesmo alvo da tela de Peso: um alvo novo começa a barra de progresso no peso atual. */
    fun setTarget(targetKg: Double) {
        viewModelScope.launch { goalsStore.setWeightTarget(targetKg, uiState.value.weightKg ?: DEFAULT_WEIGHT_KG) }
    }

    fun clearTarget() {
        viewModelScope.launch { goalsStore.setWeightGoal(null) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WellioApp).container
                ProfileViewModel(
                    container.profileStore,
                    container.profilePhotoStore,
                    container.profileCoverStore,
                    container.goalsStore,
                    container.weightRepository,
                    container.journeyDao,
                    container.clock
                )
            }
        }
    }
}
