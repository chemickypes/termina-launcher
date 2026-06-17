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

package com.hooloovoochimico.terminalauncher.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.terminalauncher.R
import com.hooloovoochimico.terminalauncher.theme.LocalTermPalette

/**
 * Tastiera interna sperimentale in Compose, in stile terminale. Sostituisce l'IME di
 * sistema sul campo input del terminale quando attiva dalle impostazioni. Opera su un
 * [TextFieldValue] rispettando il cursore/selezione, così l'editing è coerente col campo.
 */
@Composable
fun TerminalKeyboard(
  value: TextFieldValue,
  onValueChange: (TextFieldValue) -> Unit,
  onSubmit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalTermPalette.current
  var symbols by remember { mutableStateOf(false) }
  var shift by remember { mutableStateOf(false) }

  fun type(s: String) {
    onValueChange(value.insert(s))
    if (shift) shift = false // shift singolo, come sui telefoni
  }

  val letterRows =
    listOf(
      listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
      listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
      listOf("z", "x", "c", "v", "b", "n", "m"),
    )
  val symbolRows =
    listOf(
      listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
      listOf("-", "_", ".", ":", "~", "@", "#", "&", "*"),
      listOf("(", ")", "[", "]", "{", "}", "|", "\\"),
    )
  val rows = if (symbols) symbolRows else letterRows

  Column(
    modifier =
      modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    rows.forEachIndexed { i, keys ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        // ultima riga di lettere: shift a sinistra, backspace a destra
        if (!symbols && i == letterRows.lastIndex) {
          KeyButton(if (shift) "⇪" else "⇧", if (shift) palette.accent else palette.dim, weight = 1.5f) {
            shift = !shift
          }
        }
        keys.forEach { k ->
          val shown = if (!symbols && shift) k.uppercase() else k
          KeyButton(shown, palette.fg) { type(shown) }
        }
        if (i == letterRows.lastIndex || (symbols && i == symbolRows.lastIndex)) {
          KeyButton("⌫", palette.accent, weight = 1.5f) { onValueChange(value.backspace()) }
        }
      }
    }
    // riga finale: switch layer, spazio, invio
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      KeyButton(
        if (symbols) stringResource(R.string.kb_letters) else stringResource(R.string.kb_symbols),
        palette.accent,
        weight = 1.8f,
      ) {
        symbols = !symbols
        shift = false
      }
      // "/" sempre a portata di mano (comandi tipo /apps) senza passare ai simboli
      KeyButton("/", palette.fg, weight = 1.2f) { type("/") }
      KeyButton("·", palette.fg, weight = 3.4f) { type(" ") }
      KeyButton("⏎", palette.accent, weight = 1.8f) { onSubmit() }
    }
  }
}

@Composable
private fun RowScope.KeyButton(
  label: String,
  color: androidx.compose.ui.graphics.Color,
  weight: Float = 1f,
  onClick: () -> Unit,
) {
  val palette = LocalTermPalette.current
  Text(
    text = label,
    color = color,
    style = MaterialTheme.typography.bodyLarge,
    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    modifier =
      Modifier.weight(weight)
        .height(42.dp)
        .border(1.dp, palette.dim, RoundedCornerShape(4.dp))
        .clickable(onClick = onClick)
        .wrapContentHeight(Alignment.CenterVertically),
  )
}

private fun TextFieldValue.insert(s: String): TextFieldValue {
  val start = selection.min
  val end = selection.max
  val newText = text.replaceRange(start, end, s)
  return TextFieldValue(newText, TextRange(start + s.length))
}

private fun TextFieldValue.backspace(): TextFieldValue {
  val start = selection.min
  val end = selection.max
  return when {
    start != end -> TextFieldValue(text.replaceRange(start, end, ""), TextRange(start))
    start > 0 -> TextFieldValue(text.removeRange(start - 1, start), TextRange(start - 1))
    else -> this
  }
}
