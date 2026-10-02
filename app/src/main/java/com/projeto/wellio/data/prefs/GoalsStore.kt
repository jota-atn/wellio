package com.projeto.wellio.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.projeto.wellio.domain.model.Goal
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.domain.model.Goals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Metas diárias e semanais, guardadas no DataStore. */
class GoalsStore(private val dataStore: DataStore<Preferences>) {

    val goals: Flow<Goals> = dataStore.data.map { prefs ->
        GoalType.entries.fold(Goals.Default) { goals, type ->
            val default = Goals.Default[type]
            goals.with(
                type,
                Goal(
                    daily = prefs[dailyKey(type)] ?: default.daily,
                    weeklyOverride = prefs[weeklyKey(type)]
                )
            )
        }
    }

    suspend fun setGoal(type: GoalType, goal: Goal) {
        dataStore.edit { prefs ->
            prefs[dailyKey(type)] = goal.daily
            val weekly = goal.weeklyOverride
            if (weekly == null) prefs.remove(weeklyKey(type)) else prefs[weeklyKey(type)] = weekly
        }
    }

    private fun dailyKey(type: GoalType) = intPreferencesKey("goal_${type.name.lowercase()}_daily")
    private fun weeklyKey(type: GoalType) = intPreferencesKey("goal_${type.name.lowercase()}_weekly")
}
