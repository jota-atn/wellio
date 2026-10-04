package com.projeto.wellio.ui.weight

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.projeto.wellio.domain.model.WeightEntry
import com.projeto.wellio.ui.theme.WellioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class WeightScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 1)
    private val ontem = WeightEntry("w2", today.minusDays(1), 72.4)
    private val semanaPassada = WeightEntry("w1", today.minusDays(7), 73.1)

    private fun show(
        entries: List<Pair<WeightEntry, Double?>> = emptyList(),
        onSave: (LocalDate, Double) -> Unit = { _, _ -> }
    ) {
        compose.setContent {
            WellioTheme {
                WeightScreen(
                    state = WeightUiState(today, entries),
                    onBack = {},
                    onSave = onSave,
                    onUpdate = { _, _, _ -> },
                    onDelete = {},
                    onRestore = {}
                )
            }
        }
    }

    @Test
    fun semPesagensAvisaQueOCalculoUsaOPadrao() {
        show()

        compose.onNodeWithText("Sem pesagens: o cálculo usa 70,0 kg").assertIsDisplayed()
    }

    @Test
    fun mostraVariacaoEmRelacaoAPesagemAnterior() {
        show(entries = listOf(ontem to -0.7, semanaPassada to null))

        compose.onNodeWithContentDescription("Ontem, 72,4 kg, −0,7 kg").assertIsDisplayed()
    }

    @Test
    fun registrarAceitaVirgulaEUsaHojePorPadrao() {
        var saved: Pair<LocalDate, Double>? = null
        show(onSave = { day, kg -> saved = day to kg })

        compose.onNodeWithText("Registrar peso", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Digitar o peso").performClick()
        compose.onNodeWithText("Peso (kg)").performTextReplacement("71,8")
        compose.onNodeWithText("Salvar").performClick()

        assertEquals(today to 71.8, saved)
    }

    @Test
    fun pesoForaDaFaixaNaoPodeSerSalvo() {
        show()

        compose.onNodeWithText("Registrar peso", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Digitar o peso").performClick()
        compose.onNodeWithText("Peso (kg)").performTextReplacement("5")

        compose.onNodeWithText("Salvar").assertIsNotEnabled()
    }

    @Test
    fun botoesAjustamOPesoDe0Virgula1() {
        var saved: Double? = null
        show(entries = listOf(ontem to null), onSave = { _, kg -> saved = kg })

        compose.onNodeWithText("Registrar peso", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Aumentar 0,1 kg").performClick()
        compose.onNodeWithContentDescription("Aumentar 0,1 kg").performClick()
        compose.onNodeWithText("Salvar").performClick()

        assertEquals(72.6, saved!!, 0.0)
    }

    @Test
    fun curvaApareceComDuasPesagensNoPeriodo() {
        show(entries = listOf(ontem to -0.7, semanaPassada to null))

        compose.onNodeWithContentDescription("Curva de peso: de 73,1 kg a 72,4 kg em 30 dias").assertIsDisplayed()
        compose.onNodeWithText("−0,7 kg em 30 dias").assertIsDisplayed()
    }
}
