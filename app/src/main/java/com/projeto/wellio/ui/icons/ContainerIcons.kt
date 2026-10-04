package com.projeto.wellio.ui.icons

import androidx.annotation.DrawableRes
import com.projeto.wellio.R
import com.projeto.wellio.domain.water.ContainerIcon

/** Desenho de cada ícone de recipiente (fonte em tools/icons/icones.py). */
@DrawableRes
fun containerIconRes(iconKey: String): Int = when (ContainerIcon.fromKey(iconKey)) {
    ContainerIcon.Copo -> R.drawable.ic_container_copo
    ContainerIcon.Caneca -> R.drawable.ic_container_caneca
    ContainerIcon.Xicara -> R.drawable.ic_container_xicara
    ContainerIcon.Garrafa -> R.drawable.ic_container_garrafa
    ContainerIcon.Squeeze -> R.drawable.ic_container_squeeze
    ContainerIcon.Taca -> R.drawable.ic_container_taca
    ContainerIcon.Termica -> R.drawable.ic_container_termica
    ContainerIcon.Galao -> R.drawable.ic_container_galao
}
