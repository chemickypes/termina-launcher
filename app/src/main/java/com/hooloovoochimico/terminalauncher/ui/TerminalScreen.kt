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

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.terminalauncher.terminal.COMMANDS
import com.hooloovoochimico.terminalauncher.terminal.LineKind
import com.hooloovoochimico.terminalauncher.terminal.TerminalViewModel
import com.hooloovoochimico.terminalauncher.theme.LocalTermPalette
import kotlinx.coroutines.delay

@Composable
fun TerminalScreen(vm: TerminalViewModel) {
  val palette = LocalTermPalette.current
  var input by remember { mutableStateOf(TextFieldValue("")) }
  val listState = rememberLazyListState()
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  // schermata principale (home del launcher): il tasto Indietro non deve fare nulla
  BackHandler(enabled = true) {}

  fun runCommand() {
    vm.submit(input.text)
    input = TextFieldValue("")
  }

  LaunchedEffect(vm.lines.size) {
    if (vm.lines.isNotEmpty()) listState.animateScrollToItem(vm.lines.size - 1)
  }

  // comando precaricato dalla history ("modifica"): riempie il campo e lo azzera nel VM
  LaunchedEffect(vm.pendingInput) {
    vm.pendingInput?.let {
      input = TextFieldValue(it, TextRange(it.length))
      vm.clearPendingInput()
    }
  }

  // bug tastiera: dopo aver avviato un'app il VM incrementa il segnale → chiudi subito l'IME.
  LaunchedEffect(vm.hideKeyboardSignal) {
    if (vm.hideKeyboardSignal > 0) {
      keyboardController?.hide()
      focusManager.clearFocus(force = true)
    }
  }


  // con la tastiera interna attiva non deve comparire quella di sistema
  LaunchedEffect(vm.customKeyboard) {
    if (vm.customKeyboard) keyboardController?.hide()
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
    if (input.text.startsWith("/")) {
      val matches = COMMANDS.filter { it.name.startsWith(input.text.trim().lowercase()) }
      if (matches.isNotEmpty() && matches.none { it.name == input.text.trim() }) {
        Row(
          modifier =
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 2.dp)
        ) {
          matches.forEach { spec ->
            Text(
              text = "[${spec.name}] ",
              color = palette.dim,
              style = MaterialTheme.typography.bodyMedium,
              modifier =
                Modifier.clickable {
                  val t = spec.name + " "
                  input = TextFieldValue(t, TextRange(t.length))
                },
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
      if (vm.customKeyboard) {
        // niente IME di sistema: renderizziamo testo + cursore finto lampeggiante,
        // così l'utente capisce di poter scrivere anche senza tastiera nativa.
        FakeCursorInput(
          value = input,
          color = palette.fg,
          modifier = Modifier.weight(1f),
        )
      } else {
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
          keyboardActions = KeyboardActions(onGo = { runCommand() }),
          modifier = Modifier.weight(1f).focusRequester(focusRequester),
        )
      }
    }

    // tastiera interna sperimentale (sostituisce l'IME di sistema quando attiva)
    if (vm.customKeyboard) {
      TerminalKeyboard(
        value = input,
        onValueChange = { input = it },
        onSubmit = { runCommand() },
      )
    }
  }

  // il focusRequester è attaccato solo al BasicTextField (tastiera di sistema);
  // con la tastiera custom non esiste alcun campo da mettere a fuoco → niente requestFocus
  LaunchedEffect(vm.customKeyboard) {
    if (!vm.customKeyboard) focusRequester.requestFocus()
  }
}

/**
 * Riga di input del terminale quando è attiva la tastiera custom: niente [BasicTextField]
 * (che con readOnly non disegna il cursore), ma il testo con un cursore "│" lampeggiante
 * inserito alla posizione del caret. Il blink alterna fg↔trasparente per non spostare il
 * testo; ad ogni modifica il cursore torna pieno (feedback immediato di digitazione).
 */
@Composable
private fun FakeCursorInput(
  value: TextFieldValue,
  color: Color,
  modifier: Modifier = Modifier,
) {
  var on by remember { mutableStateOf(true) }
  LaunchedEffect(value.text, value.selection) {
    on = true
    while (true) {
      delay(530)
      on = !on
    }
  }
  val caret = value.selection.start.coerceIn(0, value.text.length)
  val rendered = buildAnnotatedString {
    append(value.text.substring(0, caret))
    withStyle(SpanStyle(color = if (on) color else Color.Transparent)) { append("│") }
    append(value.text.substring(caret))
  }
  Text(
    text = rendered,
    color = color,
    style = MaterialTheme.typography.bodyLarge,
    modifier = modifier,
  )
}
