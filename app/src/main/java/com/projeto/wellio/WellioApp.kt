package com.projeto.wellio

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.projeto.wellio.data.db.AppDatabase
import com.projeto.wellio.data.prefs.GoalsStore
import com.projeto.wellio.data.prefs.HomeLayoutStore
import com.projeto.wellio.data.repository.ExerciseRepository
import com.projeto.wellio.data.repository.FoodRepository
import com.projeto.wellio.data.repository.WaterRepository
import com.projeto.wellio.data.repository.WeightRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Clock

class WellioApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.seedDatabase()
    }
}

private val Context.preferences by preferencesDataStore(name = "wellio")

/** Dependências do app, criadas uma vez e compartilhadas (injeção manual, SPEC §2). */
class AppContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob())
    private val database = AppDatabase.build(context)

    val clock: Clock = Clock.systemDefaultZone()
    val goalsStore = GoalsStore(context.preferences)
    val homeLayoutStore = HomeLayoutStore(context.preferences)
    val waterRepository = WaterRepository(database.waterDao(), clock)
    val foodRepository = FoodRepository(database.foodDao(), clock)
    val weightRepository = WeightRepository(database.weightDao(), clock)
    val exerciseRepository = ExerciseRepository(database.exerciseDao(), weightRepository, clock)

    fun seedDatabase() {
        scope.launch {
            waterRepository.seedIfEmpty()
            foodRepository.seedIfEmpty()
            exerciseRepository.seedIfEmpty()
        }
    }
}
