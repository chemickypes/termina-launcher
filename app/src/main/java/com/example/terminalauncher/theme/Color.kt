package com.example.terminalauncher.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette di un tema terminale: sfondo nero fisso, colori fosforo classici.
 */
data class TermPalette(
  val name: String,
  val fg: Color,
  val dim: Color,
  val accent: Color,
  val error: Color,
  val bg: Color = Color(0xFF0A0A0A),
)

object TermPalettes {
  val Green = TermPalette(
    name = "green",
    fg = Color(0xFF33FF33),
    dim = Color(0xFF1A8C1A),
    accent = Color(0xFFB6FFB6),
    error = Color(0xFFFF5555),
  )
  val Amber = TermPalette(
    name = "amber",
    fg = Color(0xFFFFB000),
    dim = Color(0xFF9C6C00),
    accent = Color(0xFFFFE0A3),
    error = Color(0xFFFF5555),
  )
  val Cyan = TermPalette(
    name = "cyan",
    fg = Color(0xFF4DD8E6),
    dim = Color(0xFF2A7680),
    accent = Color(0xFFC2F3F9),
    error = Color(0xFFFF5555),
  )
  val White = TermPalette(
    name = "white",
    fg = Color(0xFFE0E0E0),
    dim = Color(0xFF777777),
    accent = Color(0xFFFFFFFF),
    error = Color(0xFFFF5555),
  )

  val all = listOf(Green, Amber, Cyan, White)

  fun byName(name: String?): TermPalette = all.firstOrNull { it.name == name } ?: Green
}
