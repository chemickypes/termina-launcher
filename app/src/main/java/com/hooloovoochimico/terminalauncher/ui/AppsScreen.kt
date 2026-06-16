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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.terminalauncher.R
import com.hooloovoochimico.terminalauncher.terminal.TerminalViewModel
import com.hooloovoochimico.terminalauncher.theme.LocalTermPalette

@Composable
fun AppsScreen(vm: TerminalViewModel, onBack: () -> Unit) {
  val palette = LocalTermPalette.current
  var filter by remember { mutableStateOf("") }
  val hasWork = vm.hasWorkProfile
  // tab 0 = personali, tab 1 = lavoro (mostrata solo se esiste un profilo di lavoro)
  var tab by remember { mutableStateOf(0) }
  val workTab = hasWork && tab == 1

  val allApps = if (workTab) vm.workAppsList else vm.appsList
  val loading = if (workTab) vm.workAppsLoading else vm.appsLoading
  val apps =
    if (filter.isBlank()) allApps
    else allApps.filter { it.label.contains(filter.trim(), ignoreCase = true) }

  LaunchedEffect(Unit) { vm.loadApps() }
  LaunchedEffect(tab) { if (workTab) vm.loadWorkApps() }
  BackHandler(onBack = onBack)

  TuiFrame(
    title = stringResource(R.string.apps_title),
    footer = stringResource(R.string.apps_footer, apps.size, allApps.size),
    onBack = onBack,
  ) { modifier
    ->
    LazyColumn(modifier = modifier) {
      if (hasWork) {
        item {
          TuiTabs(
            tabs =
              listOf(
                stringResource(R.string.apps_tab_personal),
                stringResource(R.string.apps_tab_work),
              ),
            selected = tab,
            onSelect = { tab = it },
          )
        }
      }
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Text(
            text = "│ " + stringResource(R.string.filter_label),
            color = palette.dim,
            style = MaterialTheme.typography.bodyLarge,
          )
          BasicTextField(
            value = filter,
            onValueChange = { filter = it },
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
      if (loading && allApps.isEmpty()) {
        item { TuiLoading(stringResource(R.string.loading_apps)) }
      }
      itemsIndexed(apps, key = { _, app -> app.packageName + app.activityName }) { index, app ->
        TuiRow(index = index + 1, text = app.label, detail = app.packageName) {
          vm.appRepository.launch(app)
        }
      }
    }
  }
}
