package com.projeto.wellio.ui.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/** Ícones de interface que não existem no pacote básico do Material. */
object WellioIcons {
    val Undo: ImageVector by lazy {
        materialIcon(name = "Wellio.Undo", autoMirror = true) {
            materialPath {
                moveTo(12.5f, 8f)
                curveToRelative(-2.65f, 0f, -5.05f, 0.99f, -6.9f, 2.6f)
                lineTo(2f, 7f)
                verticalLineToRelative(9f)
                horizontalLineToRelative(9f)
                lineToRelative(-3.62f, -3.62f)
                curveToRelative(1.39f, -1.16f, 3.16f, -1.88f, 5.12f, -1.88f)
                curveToRelative(3.54f, 0f, 6.55f, 2.31f, 7.6f, 5.5f)
                lineToRelative(2.37f, -0.78f)
                curveTo(21.08f, 11.03f, 17.15f, 8f, 12.5f, 8f)
                close()
            }
        }
    }

    val Minus: ImageVector by lazy {
        materialIcon(name = "Wellio.Minus") {
            materialPath {
                moveTo(19f, 13f)
                horizontalLineTo(5f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(2f)
                close()
            }
        }
    }
}
