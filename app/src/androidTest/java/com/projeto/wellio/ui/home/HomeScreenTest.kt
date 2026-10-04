package com.projeto.wellio.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

    private fun show(layout: List<HomeCard>) {
        compose.setContent {
            WellioTheme {
                HomeScreen(state = HomeUiState(layout = layout), onMenuClick = {}, onUpdateGoal = { _, _, _ -> }) { module, size, modifier ->
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
}
