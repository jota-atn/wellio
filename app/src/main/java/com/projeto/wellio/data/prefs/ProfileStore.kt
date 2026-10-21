package com.projeto.wellio.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.projeto.wellio.domain.model.ActivityLevel
import com.projeto.wellio.domain.model.Profile
import com.projeto.wellio.domain.model.Sex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Perfil do usuário (SPEC §9.1), guardado no DataStore. */
class ProfileStore(private val dataStore: DataStore<Preferences>) {

    val profile: Flow<Profile> = dataStore.data.map { prefs ->
        Profile(
            name = prefs[nameKey] ?: "",
            photoFile = prefs[photoKey],
            birthDate = prefs[birthKey]?.let(LocalDate::parse),
            sex = Sex.entries.firstOrNull { it.name == prefs[sexKey] },
            heightCm = prefs[heightKey],
            activity = ActivityLevel.entries.firstOrNull { it.name == prefs[activityKey] },
            onboardingDone = prefs[onboardingKey] ?: false,
            coverColor = prefs[coverColorKey],
            coverPhoto = prefs[coverPhotoKey]
        )
    }

    /** Grava o perfil inteiro de uma vez; campos null são apagados. */
    suspend fun save(profile: Profile) {
        dataStore.edit { prefs ->
            prefs[nameKey] = profile.name
            prefs.put(photoKey, profile.photoFile)
            prefs.put(birthKey, profile.birthDate?.toString())
            prefs.put(sexKey, profile.sex?.name)
            prefs.put(heightKey, profile.heightCm)
            prefs.put(activityKey, profile.activity?.name)
            prefs[onboardingKey] = profile.onboardingDone
            prefs.put(coverColorKey, profile.coverColor)
            prefs.put(coverPhotoKey, profile.coverPhoto)
        }
    }

    private fun <T> MutablePreferences.put(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private val nameKey = stringPreferencesKey("profile_name")
    private val photoKey = stringPreferencesKey("profile_photo")
    private val birthKey = stringPreferencesKey("profile_birth_date")
    private val sexKey = stringPreferencesKey("profile_sex")
    private val heightKey = intPreferencesKey("profile_height_cm")
    private val activityKey = stringPreferencesKey("profile_activity")
    private val onboardingKey = booleanPreferencesKey("profile_onboarding_done")
    private val coverColorKey = stringPreferencesKey("profile_cover_color")
    private val coverPhotoKey = stringPreferencesKey("profile_cover_photo")
}
