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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.terminalauncher.R
import com.hooloovoochimico.terminalauncher.terminal.TerminalViewModel
import com.hooloovoochimico.terminalauncher.theme.LocalTermPalette

@Composable
fun HistoryScreen(vm: TerminalViewModel, onBack: () -> Unit) {
  val actionTarget = vm.historyActionTarget

  // il tap lungo apre il menu; il back lo chiude se aperto, altrimenti esce
  BackHandler { if (actionTarget != null) vm.dismissHistoryAction() else onBack() }

  Box(modifier = Modifier.fillMaxSize()) {
    HistoryContent(vm)
    if (actionTarget != null) ActionMenu(vm, actionTarget)
  }
}

@Composable
private fun HistoryContent(vm: TerminalViewModel) {
  val palette = LocalTermPalette.current
  val total = vm.history.size
  TuiFrame(
    title = stringResource(R.string.history_title),
    footer = stringResource(R.string.history_footer, total),
    onBack = { vm.closeHistory() },
  ) { modifier ->
    LazyColumn(modifier = modifier) {
      if (total == 0) {
        item {
          Text(
            text = "│ " + stringResource(R.string.history_empty),
            color = palette.dim,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 6.dp),
          )
        }
      } else {
        item {
          Text(
            text = "│ " + stringResource(R.string.history_hint),
            color = palette.dim,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 4.dp),
          )
        }
        // più recenti in alto; la numerazione segue la posizione reale nella history
        itemsIndexed(vm.history.asReversed()) { i, cmd ->
          TuiRow(
            index = total - i,
            text = cmd,
            onLongClick = { vm.onHistoryLongPress(cmd) },
          ) {
            vm.runFromHistory(cmd)
          }
        }
      }
    }
  }
}

/** Menu azioni a comparsa (tap lungo): esegui / modifica. */
@Composable
private fun ActionMenu(vm: TerminalViewModel, cmd: String) {
  val palette = LocalTermPalette.current
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(palette.bg.copy(alpha = 0.85f))
        .clickable { vm.dismissHistoryAction() },
    contentAlignment = Alignment.BottomStart,
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
      Text(
        text = "┌─[ $cmd ]",
        color = palette.accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyLarge,
      )
      ActionItem(stringResource(R.string.hist_run)) { vm.runFromHistory(cmd) }
      ActionItem(stringResource(R.string.hist_edit)) { vm.editFromHistory(cmd) }
      ActionItem(stringResource(R.string.act_cancel), dim = true) { vm.dismissHistoryAction() }
    }
  }
}

@Composable
private fun ActionItem(label: String, dim: Boolean = false, onClick: () -> Unit) {
  val palette = LocalTermPalette.current
  Text(
    text = "│ > $label",
    color = if (dim) palette.dim else palette.fg,
    style = MaterialTheme.typography.bodyLarge,
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
  )
}
