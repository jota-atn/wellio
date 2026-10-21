package com.projeto.wellio.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.projeto.wellio.domain.profile.OnboardingDraft
import com.projeto.wellio.ui.theme.WellioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Os botões do onboarding avançam de verdade (SPEC §5.0). */
class OnboardingScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var finished: Boolean? = null

    private fun show(initial: OnboardingDraft = OnboardingDraft(returning = false)) {
        var draft by mutableStateOf(initial)
        compose.setContent {
            WellioTheme {
                OnboardingScreen(
                    draft = draft,
                    today = LocalDate.of(2026, 10, 20),
                    photo = null,
                    onEdit = { draft = it(draft) },
                    onPhotoCropped = {},
                    onSkipLifestyle = {},
                    onFinish = { finished = it }
                )
            }
        }
    }

    @Test
    fun comecarLevaParaONome() {
        show()

        compose.onNodeWithText("Começar").performClick()

        compose.onNodeWithText("Como te chamamos?").assertIsDisplayed()
    }

    @Test
    fun continuarSoComONomeELevaAoCorpo() {
        show()
        compose.onNodeWithText("Começar").performClick()
        compose.onNodeWithText("Continuar").assertIsNotEnabled()

        compose.onNodeWithText("Seu nome").performTextInput("João")
        compose.onNodeWithText("Continuar").assertIsEnabled().performClick()

        compose.onNodeWithText("Seu corpo").assertIsDisplayed()
    }

    @Test
    fun percorreTudoEConclui() {
        show(OnboardingDraft(returning = true, name = "João", weightKg = 72.4))

        compose.onNodeWithText("Começar").performClick()
        compose.onNodeWithText("Continuar").performClick()
        compose.onNodeWithText("Seu corpo").assertIsDisplayed()
        compose.onNodeWithText("Continuar").performClick()
        compose.onNodeWithText("Seu ritmo").assertIsDisplayed()
        compose.onNodeWithText("Continuar").performClick()
        compose.onNodeWithText("Suas metas diárias").assertIsDisplayed()
        compose.onNodeWithText("Concluir").performClick()

        assertEquals(true, finished)
    }

    @Test
    fun voltarVoltaUmaEtapa() {
        show(OnboardingDraft(returning = true, name = "João", weightKg = 72.4))
        compose.onNodeWithText("Começar").performClick()
        compose.onNodeWithText("Continuar").performClick()

        compose.onNodeWithContentDescription("Voltar").performClick()

        compose.onNodeWithText("Como te chamamos?").assertIsDisplayed()
    }
}
