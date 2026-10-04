package com.projeto.wellio.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Layout da Home salvo como texto (SPEC §5.2); null enquanto o usuário não mexer (vale o padrão). */
class HomeLayoutStore(private val dataStore: DataStore<Preferences>) {

    val layout: Flow<String?> = dataStore.data.map { it[layoutKey] }

    suspend fun save(encoded: String) {
        dataStore.edit { it[layoutKey] = encoded }
    }

    private val layoutKey = stringPreferencesKey("home_layout")
}
