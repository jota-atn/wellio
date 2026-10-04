package com.projeto.wellio.ui.module

import com.projeto.wellio.domain.home.CardSize
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.projeto.wellio.AppContainer
import com.projeto.wellio.domain.model.GoalType
import com.projeto.wellio.ui.theme.ModuleColors
import com.projeto.wellio.ui.theme.WellioColors
import kotlinx.coroutines.flow.Flow

enum class ModuleId { Water, Food, Exercise, Weight }

/**
 * Tudo o que uma funcionalidade oferece ao resto do app, declarado num lugar só (SPEC §11,
 * M1.5): item da sidebar, tela, resumo, card da Home e, numa próxima etapa, ação do FAB. Um
 * módulo novo implementa este contrato e aparece em todos esses lugares.
 */
interface WellioModule {
    val id: ModuleId
    val title: String

    @get:DrawableRes
    val icon: Int

    /** Cores do módulo dentro da paleta do tema. */
    fun colors(palette: WellioColors): ModuleColors

    /** Meta diária que o módulo alimenta no Dashboard; null se não tiver (Peso). */
    val goalType: GoalType?

    /** Tela própria; null enquanto ela não existe (cai em "Em breve"). */
    val destination: Any?

    /** Resumo de hoje, atualizado sozinho. */
    fun summary(container: AppContainer): Flow<Summary>

    /**
     * Card da Home nos dois tamanhos (SPEC §5.1). Busca os próprios dados; [onOpen] abre a
     * tela do módulo. Ocupa o espaço que o grid der ([modifier] traz o tamanho).
     */
    @Composable
    fun HomeCard(size: CardSize, onOpen: () -> Unit, modifier: Modifier)
}
