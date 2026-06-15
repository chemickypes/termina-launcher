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
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.hooloovoochimico.terminalauncher.R
import com.hooloovoochimico.terminalauncher.terminal.TerminalViewModel

/**
 * Lettore del manuale (assets/handbook*.md) in stile terminale: un lettore
 * markdown nativo a due schede — anteprima formattata e sorgente grezzo.
 */
@Composable
fun ManualScreen(vm: TerminalViewModel, onBack: () -> Unit) {
  BackHandler(onBack = onBack)

  var tab by rememberSaveable { mutableIntStateOf(0) }

  TuiFrame(
    title = stringResource(R.string.manual_title),
    footer = stringResource(R.string.manual_footer),
    onBack = onBack,
  ) { modifier ->
    Column(modifier = modifier) {
      TuiTabs(
        tabs =
          listOf(
            stringResource(R.string.manual_tab_preview),
            stringResource(R.string.manual_tab_source),
          ),
        selected = tab,
        onSelect = { tab = it },
      )
      if (tab == 0) {
        MarkdownPreview(lines = vm.manualLines)
      } else {
        MarkdownSource(lines = vm.manualLines)
      }
    }
  }
}
