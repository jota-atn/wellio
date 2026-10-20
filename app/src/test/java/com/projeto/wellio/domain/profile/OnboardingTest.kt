package com.projeto.wellio.domain.profile

import com.projeto.wellio.domain.model.ActivityLevel
import com.projeto.wellio.domain.model.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OnboardingTest {

    private val today = LocalDate.of(2026, 10, 20)

    @Test
    fun `nome e peso sao obrigatorios`() {
        val draft = OnboardingDraft(returning = false)

        assertFalse(draft.canContinue(OnboardingStep.You))
        assertFalse(draft.canContinue(OnboardingStep.Body))
        assertTrue(draft.canContinue(OnboardingStep.Lifestyle))
        assertEquals(OnboardingStep.You, draft.firstIncomplete)
    }

    @Test
    fun `nome so com espacos nao vale`() {
        assertFalse(OnboardingDraft(returning = false, name = "  ").canContinue(OnboardingStep.You))
    }

    @Test
    fun `com nome e peso nada falta`() {
        assertNull(OnboardingDraft(returning = false, name = "João", weightKg = 72.4).firstIncomplete)
    }

    @Test
    fun `so estilo de vida e metas podem ser pulados`() {
        assertEquals(
            listOf(OnboardingStep.Lifestyle, OnboardingStep.Goals),
            OnboardingStep.entries.filter { it.skippable }
        )
    }

    @Test
    fun `quem e novo recebe as metas sugeridas ao chegar nelas`() {
        val draft = OnboardingDraft(
            returning = false,
            name = "João",
            birthDate = LocalDate.of(1999, 3, 14),
            sex = Sex.Male,
            heightCm = 178,
            weightKg = 72.4,
            activity = ActivityLevel.Moderate,
            targetKg = 70.0
        ).withSuggestedGoals(today)

        assertEquals(2550, draft.waterMl)
        assertEquals(2150, draft.kcal)
        assertTrue(draft.goalsReady)
    }

    @Test
    fun `sem dados para a formula a meta de kcal fica a padrao`() {
        val draft = OnboardingDraft(returning = false, name = "João", weightKg = 72.4).withSuggestedGoals(today)

        assertEquals(2000, draft.kcal)
        assertEquals(2550, draft.waterMl)
    }

    @Test
    fun `quem ja usa o app mantem as proprias metas`() {
        val draft = OnboardingDraft(returning = true, name = "João", weightKg = 72.4, waterMl = 3000, kcal = 1800)

        assertEquals(draft, draft.withSuggestedGoals(today))
    }

    @Test
    fun `metas ja ajustadas nao voltam para a sugestao`() {
        val adjusted = OnboardingDraft(returning = false, name = "João", weightKg = 72.4)
            .withSuggestedGoals(today)
            .copy(waterMl = 2000)

        assertEquals(2000, adjusted.withSuggestedGoals(today).waterMl)
    }

    @Test
    fun `perfil final vem com o nome sem espacos e o onboarding concluido`() {
        val profile = OnboardingDraft(returning = false, name = " João ", weightKg = 72.4).profile()

        assertEquals("João", profile.name)
        assertTrue(profile.onboardingDone)
    }
}
