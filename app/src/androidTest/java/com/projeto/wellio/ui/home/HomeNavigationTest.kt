package com.projeto.wellio.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.projeto.wellio.MainActivity
import org.junit.Rule
import org.junit.Test

/** A Home dentro do app de verdade (com a sidebar), para os gestos que dependem dela. */
class HomeNavigationTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun swipeAcross() {
        compose.onRoot().performTouchInput {
            swipeRight(startX = width * 0.15f, endX = width * 0.85f, durationMillis = 300)
        }
        compose.waitForIdle()
    }

    @Test
    fun foraDaEdicaoDeslizarAbreASidebar() {
        swipeAcross()

        compose.onNodeWithText("Registros").assertIsDisplayed()
    }

    @Test
    fun naEdicaoDeslizarNaoAbreASidebar() {
        compose.onNodeWithContentDescription("Editar Home").performClick()

        swipeAcross()

        compose.onNodeWithText("Registros").assertIsNotDisplayed()
    }

    private fun openWaterCardEditor() {
        compose.onNodeWithContentDescription("ml de", substring = true).performTouchInput { longClick() }
    }

    private fun dragHandle(dx: Float) {
        compose.onNodeWithTag("alca-tamanho").performTouchInput {
            down(center)
            repeat(30) { moveBy(Offset(dx / 30, 0f)) }
            up()
        }
        compose.waitForIdle()
    }

    @Test
    fun comOEditorDeCardAbertoDeslizarNaoAbreASidebar() {
        openWaterCardEditor()

        swipeAcross()

        compose.onNodeWithText("Registros").assertIsNotDisplayed()
    }

    @Test
    fun alcaFuncionaVariasVezesNoAppDeVerdade() {
        openWaterCardEditor()
        compose.onNodeWithContentDescription("Editando Água, inteiro").assertIsDisplayed()

        dragHandle(-360f)
        compose.onNodeWithContentDescription("Editando Água, metade").assertIsDisplayed()

        dragHandle(360f)
        compose.onNodeWithContentDescription("Editando Água, inteiro").assertIsDisplayed()
    }
}
