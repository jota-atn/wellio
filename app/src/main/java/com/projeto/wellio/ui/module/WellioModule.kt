package com.projeto.wellio.ui.module

import androidx.annotation.DrawableRes
import com.projeto.wellio.AppContainer
import com.projeto.wellio.ui.theme.ModuleColors
import com.projeto.wellio.ui.theme.WellioColors
import kotlinx.coroutines.flow.Flow

enum class ModuleId { Water, Food, Exercise, Weight }

/**
 * Tudo o que uma funcionalidade oferece ao resto do app, declarado num lugar só (SPEC §11,
 * M1.5): item da sidebar, tela, resumo e, nas próximas etapas, cards e ação do FAB. Um
 * módulo novo implementa este contrato e aparece em todos esses lugares.
 */
interface WellioModule {
    val id: ModuleId
    val title: String

    @get:DrawableRes
    val icon: Int

    /** Cores do módulo dentro da paleta do tema. */
    fun colors(palette: WellioColors): ModuleColors

    /** Tela própria; null enquanto ela não existe (cai em "Em breve"). */
    val destination: Any?

    /** Resumo de hoje, atualizado sozinho. */
    fun summary(container: AppContainer): Flow<Summary>
}
