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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.terminalauncher.R
import com.hooloovoochimico.terminalauncher.terminal.TerminalViewModel
import com.hooloovoochimico.terminalauncher.theme.LocalTermPalette

/**
 * Viewer di file in sola lettura. I markdown sono resi con anteprima/sorgente
 * (riusa i renderer di [MarkdownView]); ogni altro file di testo come sorgente
 * grezzo. L'azione `[modifica]` apre il file nel nostro nano.
 */
@Composable
fun FileViewerScreen(vm: TerminalViewModel, onBack: () -> Unit) {
  val palette = LocalTermPalette.current
  val file = vm.viewerFile ?: return
  val isMarkdown = vm.viewerIsMarkdown
  var tab by rememberSaveable(file.absolutePath) { mutableIntStateOf(0) }

  BackHandler(onBack = onBack)

  TuiFrame(
    title = file.name,
    footer = stringResource(R.string.viewer_footer, vm.viewerLines.size),
    onBack = onBack,
  ) { modifier ->
    Column(modifier = modifier) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      ) {
        Text(text = "│ ", color = palette.dim, style = MaterialTheme.typography.bodyLarge)
        if (isMarkdown) {
          val labels =
            listOf(
              stringResource(R.string.manual_tab_preview),
              stringResource(R.string.manual_tab_source),
            )
          labels.forEachIndexed { i, label ->
            val active = i == tab
            Text(
              text = if (active) "[$label]" else " $label ",
              color = if (active) palette.accent else palette.dim,
              style = MaterialTheme.typography.bodyLarge,
              modifier = Modifier.clickable { tab = i }.padding(horizontal = 2.dp),
            )
          }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
          text = "[" + stringResource(R.string.viewer_edit) + "]",
          color = palette.accent,
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.clickable { vm.editViewerFile() }.padding(horizontal = 2.dp),
        )
      }

      when {
        vm.viewerLoading -> TuiLoading(stringResource(R.string.viewer_loading))
        isMarkdown && tab == 0 -> MarkdownPreview(lines = vm.viewerLines)
        else -> MarkdownSource(lines = vm.viewerLines)
      }
    }
  }
}
