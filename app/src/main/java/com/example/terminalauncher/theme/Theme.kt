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
