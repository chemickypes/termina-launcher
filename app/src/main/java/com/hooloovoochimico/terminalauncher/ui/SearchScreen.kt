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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.terminalauncher.R
import com.hooloovoochimico.terminalauncher.system.SearchHit
import com.hooloovoochimico.terminalauncher.terminal.TerminalViewModel
import com.hooloovoochimico.terminalauncher.theme.LocalTermPalette

@Composable
fun SearchScreen(vm: TerminalViewModel, onBack: () -> Unit) {
  val actionTarget = vm.searchActionTarget

  // il tap lungo apre il menu; il back lo chiude se aperto, altrimenti esce dalla ricerca
  BackHandler { if (actionTarget != null) vm.dismissAction() else onBack() }

  Box(modifier = Modifier.fillMaxSize()) {
    SearchContent(vm)
    if (actionTarget != null) ActionMenu(vm, actionTarget)
  }
}

@Composable
private fun SearchContent(vm: TerminalViewModel) {
  val palette = LocalTermPalette.current
  TuiFrame(
    title = stringResource(R.string.search_title),
    footer = vm.searchStatus,
    onBack = { vm.closeSearch() },
  ) { modifier ->
    LazyColumn(modifier = modifier) {
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Text(
            text = "│ find: ",
            color = palette.dim,
            style = MaterialTheme.typography.bodyLarge,
          )
          BasicTextField(
            value = vm.searchQuery,
            onValueChange = { vm.onSearchQueryChange(it) },
            textStyle =
              TextStyle(
                color = palette.fg,
                fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
              ),
            cursorBrush = SolidColor(palette.fg),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
      }

      if (vm.searchIndexing) {
        item { TuiLoading(vm.searchStatus) }
      } else if (vm.searchResults.isEmpty() && vm.searchQuery.isNotBlank()) {
        item {
          Text(
            text = "│ " + stringResource(R.string.search_no_results, vm.searchQuery),
            color = palette.dim,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 6.dp),
          )
        }
      }

      itemsIndexed(vm.searchResults, key = { _, hit -> hit.file.path }) { index, hit ->
        TuiRow(
          index = index + 1,
          text = hit.file.name + if (hit.isDir) "/" else "",
          detail = hit.file.parent ?: hit.file.path,
          onLongClick = { vm.onResultLongPress(hit) },
        ) {
          vm.openSearchHit(hit)
        }
      }
    }
  }
}

/** Menu azioni a comparsa (tap lungo): apri / vai alla cartella / condividi. */
@Composable
private fun ActionMenu(vm: TerminalViewModel, hit: SearchHit) {
  val palette = LocalTermPalette.current
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(palette.bg.copy(alpha = 0.85f))
        .clickable { vm.dismissAction() },
    contentAlignment = Alignment.BottomStart,
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
      Text(
        text = "┌─[ " + hit.file.name + " ]",
        color = palette.accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyLarge,
      )
      ActionItem(stringResource(R.string.act_open)) { vm.actionOpen(hit) }
      ActionItem(stringResource(R.string.act_cd)) { vm.actionCd(hit) }
      if (!hit.isDir) ActionItem(stringResource(R.string.act_share)) { vm.actionShare(hit) }
      ActionItem(stringResource(R.string.act_cancel), dim = true) { vm.dismissAction() }
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
