package com.projeto.wellio.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Layout da Home salvo como texto (SPEC §5.2; null enquanto o usuário não mexer, vale o padrão) e a sugestão de card. */
class HomeLayoutStore(private val dataStore: DataStore<Preferences>) {

    val layout: Flow<String?> = dataStore.data.map { it[layoutKey] }

    suspend fun save(encoded: String) {
        dataStore.edit { it[layoutKey] = encoded }
    }

    /** A sugestão "+ Adicionar card" nas linhas vazias foi dispensada (volta em Configurações, M3). */
    val suggestionDismissed: Flow<Boolean> = dataStore.data.map { it[dismissedKey] ?: false }

    suspend fun dismissSuggestion() {
        dataStore.edit { it[dismissedKey] = true }
    }

    private val layoutKey = stringPreferencesKey("home_layout")
    private val dismissedKey = booleanPreferencesKey("home_suggestion_dismissed")
}
