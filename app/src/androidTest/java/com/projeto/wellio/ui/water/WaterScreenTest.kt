package com.projeto.wellio.ui.water

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import com.projeto.wellio.domain.model.WaterContainer
import com.projeto.wellio.domain.model.WaterEntry
import com.projeto.wellio.ui.theme.WellioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class WaterScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 1)
    private val copo = WaterContainer("copo", "Copo", 200, isQuick = true, position = 0)
    private val manha = WaterEntry("e1", today, Instant.parse("2026-10-01T08:10:00Z"), 300, "Caneca")

    private fun show(
        entries: List<WaterEntry> = emptyList(),
        onAdd: (Int, String?, LocalTime) -> Unit = { _, _, _ -> },
        onQuickAdd: (WaterContainer) -> Unit = {},
        onUndoQuickAdd: () -> Unit = {},
        onDelete: (WaterEntry) -> Unit = {},
        onRestore: (WaterEntry) -> Unit = {},
        onSetGoal: (Int) -> Unit = {}
    ) {
        compose.setContent {
            WellioTheme {
                WaterScreen(
                    state = WaterUiState(today, today, goalMl = 2000, entries = entries, containers = listOf(copo)),
                    zone = ZoneOffset.UTC,
                    onBack = {},
                    onDayChange = {},
                    onAdd = onAdd,
                    onQuickAdd = onQuickAdd,
                    onUndoQuickAdd = onUndoQuickAdd,
                    onUpdate = { _, _, _, _ -> },
                    onDelete = onDelete,
                    onRestore = onRestore,
                    onSetGoal = onSetGoal,
                    onSaveContainer = { _, _, _, _, _ -> },
                    onDeleteContainer = {}
                )
            }
        }
    }

    @Test
    fun mostraTotalDoDiaEmRelacaoAMeta() {
        show(entries = listOf(manha))

        compose.onNodeWithContentDescription("Água: 300 ml de 2.000 ml, 15 por cento").assertIsDisplayed()
    }

    @Test
    fun diaSemRegistrosAvisa() {
        show()
        compose.onNodeWithText("Registros").performClick()

        compose.onNodeWithText("Nenhum registro neste dia").assertIsDisplayed()
    }

    @Test
    fun registrarPeloRecipienteUsaNomeEQuantidadeDele() {
        var added: Pair<Int, String?>? = null
        show(onAdd = { ml, name, _ -> added = ml to name })

        compose.onNodeWithText("Adicionar", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Copo 200 ml").performClick()
        compose.onNodeWithText("Salvar").performClick()

        assertEquals(200 to "Copo", added)
    }

    @Test
    fun apagarOfereceDesfazer() {
        var deleted: WaterEntry? = null
        var restored: WaterEntry? = null
        show(entries = listOf(manha), onDelete = { deleted = it }, onRestore = { restored = it })

        compose.onNodeWithText("Registros").performClick()
        compose.onNodeWithContentDescription("Apagar registro das 08:10").performClick()
        compose.onNodeWithText("Desfazer").performClick()

        assertEquals(manha, deleted)
        assertEquals(manha, restored)
    }

    @Test
    fun tocarNoRecipienteRegistraNaHoraComDesfazer() {
        var drunk: WaterContainer? = null
        var undone = false
        show(onQuickAdd = { drunk = it }, onUndoQuickAdd = { undone = true })

        compose.onNodeWithContentDescription("Copo, 200 ml").performClick()
        compose.onNodeWithText("Desfazer").performClick()

        assertEquals(copo, drunk)
        assertTrue(undone)
    }

    @Test
    fun segurarORecipienteAbreAEdicaoDele() {
        show()

        compose.onNodeWithContentDescription("Copo, 200 ml").performTouchInput { longClick() }

        compose.onNodeWithText("Editar recipiente").assertIsDisplayed()
    }

    @Test
    fun editarMostraMeusRecipientes() {
        show()

        compose.onNodeWithText("Editar").performClick()

        compose.onNodeWithText("Meus recipientes").assertIsDisplayed()
        compose.onNodeWithContentDescription("Editar Copo, 200 ml").assertIsDisplayed()
    }

    @Test
    fun registrosComecamRecolhidos() {
        show(entries = listOf(manha))

        compose.onNodeWithContentDescription("Apagar registro das 08:10").assertDoesNotExist()
        compose.onNodeWithText("Registros").performClick()
        compose.onNodeWithContentDescription("Apagar registro das 08:10").assertIsDisplayed()
    }

    @Test
    fun desfazerUltimoApagaORegistroMaisRecente() {
        val tarde = WaterEntry("e2", today, Instant.parse("2026-10-01T15:00:00Z"), 200, "Copo")
        var deleted: WaterEntry? = null
        show(entries = listOf(manha, tarde), onDelete = { deleted = it })

        compose.onNodeWithContentDescription("Desfazer último").performClick()

        assertEquals(tarde, deleted)
    }

    @Test
    fun metaPodeSerMudadaNaTela() {
        var goal: Int? = null
        show(onSetGoal = { goal = it })

        compose.onNodeWithText("de 2.000 ml").performClick()
        compose.onNodeWithText("Meta (ml)").performTextReplacement("2500")
        compose.onNodeWithText("Salvar").performClick()

        assertEquals(2500, goal)
    }
}
