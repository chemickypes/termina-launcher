/*
 * Termina Launcher — a terminal-style Android home-screen launcher.
 * Copyright (C) 2026 Angelo Moroni
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example.terminalauncher.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalTermPalette = staticCompositionLocalOf { TermPalettes.Green }

@Composable
fun TerminaLauncherTheme(palette: TermPalette, content: @Composable () -> Unit) {
  val colorScheme =
    darkColorScheme(
      primary = palette.fg,
      secondary = palette.dim,
      background = palette.bg,
      surface = palette.bg,
      onPrimary = palette.bg,
      onBackground = palette.fg,
      onSurface = palette.fg,
      error = palette.error,
    )
  androidx.compose.runtime.CompositionLocalProvider(LocalTermPalette provides palette) {
    MaterialTheme(colorScheme = colorScheme, typography = TermTypography, content = content)
  }
}
