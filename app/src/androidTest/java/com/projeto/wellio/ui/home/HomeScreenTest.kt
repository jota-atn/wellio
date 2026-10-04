package com.projeto.wellio.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.home.CardSize
import com.projeto.wellio.domain.home.HomeCard
import com.projeto.wellio.ui.theme.WellioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var saved: List<HomeCard>? = null
    private var dismissed = false

    private fun show(layout: List<HomeCard>, showSuggestion: Boolean = false) {
        compose.setContent {
            WellioTheme {
                HomeScreen(
                    state = HomeUiState(layout = layout, showSuggestion = showSuggestion),
                    onMenuClick = {},
                    onUpdateGoal = { _, _, _ -> },
                    onSaveLayout = { saved = it },
                    onDismissSuggestion = { dismissed = true }
                ) { module, size, modifier ->
                    Box(modifier.testTag("card-${module.id.key}")) { Text("${module.title} ${size.name}") }
                }
            }
        }
    }

    @Test
    fun montaOsCardsDoLayoutSalvo() {
        show(listOf(HomeCard("weight", CardSize.Full), HomeCard("water", CardSize.Half)))

        compose.onNodeWithText("Peso Full").assertIsDisplayed()
        compose.onNodeWithText("Água Half").assertIsDisplayed()
    }

    @Test
    fun metadeSemVizinhoFicaNaMetadeEsquerda() {
        show(listOf(HomeCard("water", CardSize.Half), HomeCard("weight", CardSize.Full)))

        val water = compose.onNodeWithText("Água Half").getBoundsInRoot()
        val weight = compose.onNodeWithText("Peso Full").getBoundsInRoot()
        assertTrue("água fica acima do peso", water.bottom <= weight.top)
        assertEquals(water.left, weight.left)
    }

    @Test
    fun linhasNaoFicamMaisBaixasQueABase() {
        show(listOf(HomeCard("water", CardSize.Full)))

        compose.onNodeWithTag("card-water").assertHeightIsAtLeast(128.dp)
    }

    private val agua = HomeCard("water", CardSize.Full)
    private val peso = HomeCard("weight", CardSize.Half)

    private fun enterEditing() {
        compose.onNodeWithContentDescription("Editar Home").performClick()
    }

    @Test
    fun segurarESoltarAbreOEditorDoCard() {
        show(listOf(agua))

        compose.onNodeWithTag("card-water").performTouchInput { longClick() }

        compose.onNodeWithContentDescription("Editando Água, inteiro").assertIsDisplayed()
        compose.onNodeWithContentDescription("Ocultar Água").assertIsDisplayed()
        compose.onNodeWithTag("alca-tamanho").assertIsDisplayed()
    }

    @Test
    fun editorDoCardOcultaNaHora() {
        show(listOf(agua, peso))

        compose.onNodeWithTag("card-water").performTouchInput { longClick() }
        compose.onNodeWithContentDescription("Ocultar Água").performClick()

        assertEquals(listOf(peso), saved)
    }

    @Test
    fun editorDoCardMudaOTamanhoNaHora() {
        show(listOf(agua, peso))

        compose.onNodeWithTag("card-water").performTouchInput { longClick() }
        val actions = compose.onNodeWithContentDescription("Editando Água, inteiro").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnUiThread { actions.first { it.label == "Deixar metade" }.action() }

        assertEquals(listOf(HomeCard("water", CardSize.Half), peso), saved)
    }

    @Test
    fun lapisEntraNaEdicaoDaHome() {
        show(listOf(agua))

        enterEditing()

        compose.onNodeWithText("Concluir").assertIsDisplayed()
    }

    @Test
    fun segurarEArrastarEntraNaEdicaoLevandoOCard() {
        show(listOf(agua, peso))

        compose.onNodeWithTag("card-water").performTouchInput {
            down(center)
            advanceEventTime(1000)
            // Como o dedo de verdade: vários movimentos pequenos, não um salto só
            repeat(30) { moveBy(Offset(-width * 0.25f / 30, height * 1.2f / 30)) }
            up()
        }
        compose.onNodeWithText("Concluir").performClick()

        assertEquals(listOf(peso, agua), saved)
    }

    @Test
    fun ocultarEAlternarTamanhoSalvamAoConcluir() {
        show(listOf(agua, peso))

        enterEditing()
        compose.onNodeWithContentDescription("Ocultar Água").performClick()
        val actions = compose.onNodeWithContentDescription("Peso, metade").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnUiThread { actions.first { it.label == "Deixar inteiro" }.action() }
        compose.onNodeWithText("Concluir").performClick()

        assertEquals(listOf(HomeCard("weight", CardSize.Full)), saved)
    }

    @Test
    fun talkBackMoveSemArrastar() {
        show(listOf(agua, peso))

        enterEditing()
        val actions = compose.onNodeWithContentDescription("Água, inteiro").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnUiThread { actions.first { it.label == "Mover para depois" }.action() }
        compose.onNodeWithText("Concluir").performClick()

        assertEquals(listOf(peso, agua), saved)
    }

    @Test
    fun sugestaoAbreOCatalogoEAdicionaNaHora() {
        show(listOf(agua), showSuggestion = true)

        compose.onNodeWithText("Adicionar card").performClick()
        compose.onNodeWithText("Adicionar Peso").performScrollTo().performClick()

        assertEquals(listOf(agua, HomeCard("weight", CardSize.Full)), saved)
    }

    @Test
    fun sugestaoPodeSerDispensada() {
        show(listOf(agua), showSuggestion = true)

        compose.onNodeWithContentDescription("Dispensar sugestão").performClick()

        assertTrue(dismissed)
    }

    @Test
    fun alcaEncolheOCardAoPassarDaMetadeESoltar() {
        show(listOf(agua, peso))

        compose.onNodeWithTag("card-water").performTouchInput { longClick() }
        // Card inteiro: a alça fica à direita; arrastar para a esquerda encolhe
        compose.onNodeWithTag("alca-tamanho").performTouchInput {
            down(center)
            repeat(30) { moveBy(Offset(-12f, 0f)) }
            up()
        }
        compose.waitForIdle()

        assertEquals(listOf(HomeCard("water", CardSize.Half), peso), saved)
    }

    @Test
    fun alcaQueNaoPassaDaMetadeVoltaSemMudar() {
        show(listOf(agua, peso))

        compose.onNodeWithTag("card-water").performTouchInput { longClick() }
        compose.onNodeWithTag("alca-tamanho").performTouchInput {
            down(center)
            repeat(5) { moveBy(Offset(-8f, 0f)) }
            up()
        }
        compose.waitForIdle()

        assertEquals(null, saved)
    }

    @Test
    fun naEdicaoTocarNoCardMostraAAlcaQueMudaOTamanho() {
        show(listOf(agua, peso))

        enterEditing()
        compose.onNodeWithContentDescription("Água, inteiro").performClick()
        compose.onNodeWithTag("alca-tamanho").performTouchInput {
            down(center)
            repeat(30) { moveBy(Offset(-12f, 0f)) }
            up()
        }
        compose.waitForIdle()
        compose.onNodeWithText("Concluir").performClick()

        assertEquals(listOf(HomeCard("water", CardSize.Half), peso), saved)
    }

    @Test
    fun puxaoRapidoNaAlcaTrocaAntesDoLimite() {
        show(listOf(agua, peso))

        compose.onNodeWithTag("card-water").performTouchInput { longClick() }
        compose.onNodeWithTag("alca-tamanho").performTouchInput {
            down(center)
            repeat(4) {
                moveBy(Offset(-25f, 0f), delayMillis = 8)
            }
            up()
        }
        compose.waitForIdle()

        assertEquals(listOf(HomeCard("water", CardSize.Half), peso), saved)
    }
}
