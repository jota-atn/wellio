package com.projeto.wellio.testing

import androidx.test.platform.app.InstrumentationRegistry
import com.projeto.wellio.WellioApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource

/** O app de verdade abre no onboarding na primeira vez (SPEC §5.0); nos testes da Home ele já foi concluído. */
class OnboardingDoneRule : ExternalResource() {
    override fun before() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as WellioApp
        val store = app.container.profileStore
        runBlocking { store.save(store.profile.first().copy(onboardingDone = true)) }
    }
}
