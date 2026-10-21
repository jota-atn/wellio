package com.projeto.wellio.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.projeto.wellio.ui.theme.Navy

/** Cores da capa do perfil (SPEC §9.1); [Navy] é o padrão. */
enum class CoverColor(val label: String, val top: Color, val bottom: Color) {
    NavyBlue("Azul-marinho", Navy, Color(0xFF2C3E80)),
    Blue("Azul", Color(0xFF1E4FD8), Color(0xFF3B82F6)),
    Green("Verde", Color(0xFF1B5E20), Color(0xFF2E9D4F)),
    Orange("Laranja", Color(0xFFB23C14), Color(0xFFFF8A3D)),
    Purple("Roxo", Color(0xFF4C1D95), Color(0xFF7C3AED)),
    Pink("Rosa", Color(0xFF9D174D), Color(0xFFEC4899)),
    Teal("Turquesa", Color(0xFF115E59), Color(0xFF14B8A6)),
    Graphite("Grafite", Color(0xFF1F2937), Color(0xFF4B5563));

    val brush: Brush get() = Brush.verticalGradient(listOf(top, bottom))

    companion object {
        fun of(name: String?): CoverColor = entries.firstOrNull { it.name == name } ?: NavyBlue
    }
}

/** Escolher a capa: uma das cores, uma foto, ou voltar ao padrão. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CoverSheet(
    selected: CoverColor,
    hasPhoto: Boolean,
    custom: Boolean,
    onDismiss: () -> Unit,
    onColor: (CoverColor) -> Unit,
    onPhoto: () -> Unit,
    onReset: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Capa", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CoverColor.entries.forEach { color ->
                    val isSelected = !hasPhoto && color == selected
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(color.brush)
                            .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            .clickable { onColor(color) }
                            .semantics {
                                contentDescription = color.label
                                this.selected = isSelected
                            }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onPhoto, modifier = Modifier.fillMaxWidth()) {
                Text(if (hasPhoto) "Escolher outra foto" else "Usar uma foto")
            }
            if (custom) {
                TextButton(onClick = onReset, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Voltar ao padrão") }
            }
        }
    }
}
