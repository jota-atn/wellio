package com.projeto.wellio.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Cores de um módulo (SPEC §2.5).
 * - container/onContainer: fundo do card e texto sobre ele (contraste AA)
 * - accent/onAccent: barras, anéis, botões e texto sobre eles
 * - track: trilho das barras de progresso
 */
@Immutable
data class ModuleColors(
    val container: Color,
    val onContainer: Color,
    val accent: Color,
    val onAccent: Color,
    val track: Color
)

@Immutable
data class WellioColors(
    val base: ModuleColors,
    val water: ModuleColors,
    val food: ModuleColors,
    val exercise: ModuleColors
)

// Azul-marinho: cor-base do app (Dashboard, sidebar, botões gerais)
val Navy = Color(0xFF152259)
private val NavyLight = Color(0xFF8C9EFF)

internal val LightWellioColors = WellioColors(
    base = ModuleColors(Navy, Color.White, NavyLight, Navy, Color(0xFF2C3E80)),
    water = ModuleColors(Color(0xFFD6E6FD), Color(0xFF0D3B85), Color(0xFF2563EB), Color.White, Color(0xFFAFCBF7)),
    food = ModuleColors(Color(0xFFD3EED6), Color(0xFF1B5E20), Color(0xFF2E7D32), Color.White, Color(0xFFA9D8AE)),
    exercise = ModuleColors(Color(0xFFFFE0D4), Color(0xFF8A2E0E), Color(0xFFC4471B), Color.White, Color(0xFFFFC0A8))
)

internal val DarkWellioColors = WellioColors(
    base = ModuleColors(Color(0xFF1A2660), Color.White, Color(0xFF9FAEFF), Color(0xFF0E1640), Color(0xFF2C3E80)),
    water = ModuleColors(Color(0xFF10294F), Color(0xFFD6E6FD), Color(0xFF7AA7FF), Color(0xFF0B1F40), Color(0xFF1E3D70)),
    food = ModuleColors(Color(0xFF13311A), Color(0xFFC8E6C9), Color(0xFF6FCF7A), Color(0xFF0B2410), Color(0xFF234A2B)),
    exercise = ModuleColors(Color(0xFF3F1A0E), Color(0xFFFFD9CB), Color(0xFFFF8A62), Color(0xFF3A1206), Color(0xFF5E2A18))
)

// Agulha da balança do Peso: o detalhe vermelho clássico (SPEC §2.9), igual nos dois temas
val NeedleRed = Color(0xFFFF4B4B)
