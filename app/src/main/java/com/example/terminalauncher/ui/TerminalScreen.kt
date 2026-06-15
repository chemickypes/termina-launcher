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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.terminalauncher.terminal.COMMANDS
import com.example.terminalauncher.terminal.LineKind
import com.example.terminalauncher.terminal.TerminalViewModel
import com.example.terminalauncher.theme.LocalTermPalette

@Composable
fun TerminalScreen(vm: TerminalViewModel) {
  val palette = LocalTermPalette.current
  var input by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(vm.lines.size) {
    if (vm.lines.isNotEmpty()) listState.animateScrollToItem(vm.lines.size - 1)
  }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
    LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
      items(vm.lines) { line ->
        val color =
          when (line.kind) {
            LineKind.INPUT -> palette.accent
            LineKind.OUTPUT -> palette.fg
            LineKind.ACCENT -> palette.accent
            LineKind.ERROR -> palette.error
          }
        Text(text = line.text, color = color, style = MaterialTheme.typography.bodyLarge)
      }
    }

    // suggerimenti comando mentre si digita "/"
    if (input.startsWith("/")) {
      val matches = COMMANDS.filter { it.name.startsWith(input.trim().lowercase()) }
      if (matches.isNotEmpty() && matches.none { it.name == input.trim() }) {
        Row(
          modifier =
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 2.dp)
        ) {
          matches.forEach { spec ->
            Text(
              text = "[${spec.name}] ",
              color = palette.dim,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.clickable { input = spec.name + " " },
            )
          }
        }
      }
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
      Text(
        text = "termina:${vm.cwdLabel}$ ",
        color = palette.accent,
        maxLines = 1,
        style = MaterialTheme.typography.bodyLarge,
      )
      BasicTextField(
        value = input,
        onValueChange = { input = it },
        textStyle =
          TextStyle(
            color = palette.fg,
            fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
          ),
        cursorBrush = SolidColor(palette.fg),
        singleLine = true,
        keyboardOptions =
          KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Go,
          ),
        keyboardActions =
          KeyboardActions(
            onGo = {
              vm.submit(input)
              input = ""
            }
          ),
        modifier = Modifier.weight(1f).focusRequester(focusRequester),
      )
    }
  }

  LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
