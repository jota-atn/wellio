package com.projeto.wellio.ui.module

import androidx.annotation.DrawableRes

/**
 * Ação de 1 toque do FAB (SPEC §5.1): segurar a pílula do módulo mostra estas ações, que
 * registram sem abrir nada. [perform] registra e devolve como desfazer.
 */
class OneTapAction(
    val label: String,
    @DrawableRes val icon: Int,
    val perform: suspend () -> (suspend () -> Unit)
)
