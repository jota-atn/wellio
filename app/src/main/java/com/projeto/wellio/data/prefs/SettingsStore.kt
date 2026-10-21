package com.projeto.wellio.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Configurações do app (SPEC §9.3), guardadas no DataStore. */
class SettingsStore(private val dataStore: DataStore<Preferences>) {

    /** Confete e aviso quando uma meta é cumprida (SPEC §9.2); ligado por padrão. */
    val celebrationsEnabled: Flow<Boolean> = dataStore.data.map { it[celebrationsKey] ?: true }

    suspend fun setCelebrationsEnabled(enabled: Boolean) {
        dataStore.edit { it[celebrationsKey] = enabled }
    }

    private val celebrationsKey = booleanPreferencesKey("settings_celebrations")
}
