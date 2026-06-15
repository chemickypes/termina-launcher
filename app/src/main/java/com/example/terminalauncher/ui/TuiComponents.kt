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

package com.example.terminalauncher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.terminalauncher.theme.LocalTermPalette
import kotlinx.coroutines.delay

/**
 * Cornice TUI in stile ncurses: titolo in alto, footer in basso,
 * contenuto scorrevole nel mezzo.
 */
@Composable
fun TuiFrame(
  title: String,
  footer: String,
  onBack: () -> Unit,
  content: @Composable (Modifier) -> Unit,
) {
  val palette = LocalTermPalette.current
  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
    Text(
      text = "┌─[ $title ]" + "─".repeat(40),
      color = palette.accent,
      maxLines = 1,
      overflow = TextOverflow.Clip,
      style = MaterialTheme.typography.bodyLarge,
    )
    content(Modifier.weight(1f).fillMaxWidth())
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "└─[ ",
        color = palette.accent,
        style = MaterialTheme.typography.bodyLarge,
      )
      Text(
        text = "ESC",
        color = palette.error,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.clickable(onClick = onBack),
      )
      Text(
        text = " ]─[ $footer ]" + "─".repeat(30),
        color = palette.accent,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        style = MaterialTheme.typography.bodyLarge,
      )
    }
  }
}

/** Indicatore di caricamento stile terminale: spinner braille animato + etichetta. */
@Composable
fun TuiLoading(label: String, modifier: Modifier = Modifier) {
  val palette = LocalTermPalette.current
  val frames = remember { listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏") }
  var frame by remember { mutableStateOf(0) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(90)
      frame = (frame + 1) % frames.size
    }
  }
  Text(
    text = "│ ${frames[frame]} $label",
    color = palette.accent,
    style = MaterialTheme.typography.bodyLarge,
    modifier = modifier.padding(vertical = 6.dp),
  )
}

@Composable
fun TuiRow(
  index: Int?,
  text: String,
  detail: String? = null,
  onLongClick: (() -> Unit)? = null,
  onClick: () -> Unit,
) {
  val palette = LocalTermPalette.current
  val clickModifier =
    if (onLongClick != null)
      Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    else Modifier.clickable(onClick = onClick)
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth().then(clickModifier).padding(vertical = 5.dp),
  ) {
    Text(
      text = "│ " + (index?.toString()?.padStart(3)?.plus("  ") ?: ""),
      color = palette.dim,
      style = MaterialTheme.typography.bodyLarge,
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = text,
        color = palette.fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyLarge,
      )
      if (detail != null) {
        Text(
          text = detail,
          color = palette.dim,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}
