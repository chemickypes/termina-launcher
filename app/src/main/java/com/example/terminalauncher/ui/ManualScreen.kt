package com.example.terminalauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.terminalauncher.R
import com.example.terminalauncher.terminal.TerminalViewModel
import com.example.terminalauncher.theme.LocalTermPalette

/** Lettore del manuale (assets/handbook.md) in stile terminale, scorrevole. */
@Composable
fun ManualScreen(vm: TerminalViewModel, onBack: () -> Unit) {
  val palette = LocalTermPalette.current

  BackHandler(onBack = onBack)

  TuiFrame(
    title = stringResource(R.string.manual_title),
    footer = stringResource(R.string.manual_footer),
    onBack = onBack,
  ) { modifier ->
    LazyColumn(modifier = modifier) {
      items(vm.manualLines) { raw ->
        // evidenziazione leggera del markdown: titoli, tabelle, liste
        val trimmed = raw.trimStart()
        val color =
          when {
            trimmed.startsWith("#") -> palette.accent
            trimmed.startsWith("---") -> palette.dim
            trimmed.startsWith("|") -> palette.dim
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> palette.fg
            else -> palette.fg
          }
        val text = raw.replace(Regex("[#*`]"), "").ifEmpty { " " }
        Text(text = text, color = color, style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}
