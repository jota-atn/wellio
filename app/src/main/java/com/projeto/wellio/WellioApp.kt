package com.projeto.wellio

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.projeto.wellio.data.db.AppDatabase
import com.projeto.wellio.data.prefs.GoalsStore
import com.projeto.wellio.data.prefs.HomeLayoutStore
import com.projeto.wellio.data.prefs.ProfilePhotoStore
import com.projeto.wellio.data.prefs.ProfileStore
import com.projeto.wellio.data.prefs.SettingsStore
import com.projeto.wellio.data.repository.ExerciseRepository
import com.projeto.wellio.data.repository.FoodRepository
import com.projeto.wellio.data.repository.GoalRepository
import com.projeto.wellio.data.repository.WaterRepository
import com.projeto.wellio.data.repository.WeightRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.datastore.preferences.core.edit
import java.io.File
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
    val goalRepository = GoalRepository(database.goalDao(), clock)
    val journeyDao = database.journeyDao()
    val goalsStore = GoalsStore(context.preferences, onDailyGoalsChanged = goalRepository::recordDaily)
    val homeLayoutStore = HomeLayoutStore(context.preferences)
    val settingsStore = SettingsStore(context.preferences)
    val profileStore = ProfileStore(context.preferences)
    val profilePhotoStore = ProfilePhotoStore(File(context.filesDir, "profile"))
    val profileCoverStore = ProfilePhotoStore(File(context.filesDir, "profile"), prefix = "cover")
    val waterRepository = WaterRepository(database.waterDao(), clock)
    val foodRepository = FoodRepository(database.foodDao(), clock)
    val weightRepository = WeightRepository(database.weightDao(), clock)
    val exerciseRepository = ExerciseRepository(database.exerciseDao(), weightRepository, clock)

    private val appContext = context.applicationContext

    /**
     * Ferramenta de teste (só em debug e staging): apaga registros, perfil, metas, fotos e o
     * layout da Home, e devolve o app ao primeiro uso (o onboarding volta sozinho).
     */
    suspend fun resetAll() {
        withContext(Dispatchers.IO) {
            database.clearAllTables()
            File(appContext.filesDir, "profile").deleteRecursively()
        }
        appContext.preferences.edit { it.clear() }
        waterRepository.seedIfEmpty()
        foodRepository.seedIfEmpty()
        exerciseRepository.seedIfEmpty()
    }

    fun seedDatabase() {
        scope.launch {
            waterRepository.seedIfEmpty()
            foodRepository.seedIfEmpty()
            exerciseRepository.seedIfEmpty()
        }
    }
}
