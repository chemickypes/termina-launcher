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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.terminalauncher.R
import com.hooloovoochimico.terminalauncher.terminal.TerminalViewModel
import com.hooloovoochimico.terminalauncher.theme.LocalTermPalette

/** Editor di testo minimale ispirato a nano. */
@Composable
fun EditorScreen(vm: TerminalViewModel) {
  val palette = LocalTermPalette.current
  val context = LocalContext.current
  val file = vm.editorFile ?: return
  val originalText = remember(file) {
    runCatching { if (file.exists()) file.readText() else "" }.getOrDefault("")
  }
  var text by remember(file) { mutableStateOf(originalText) }
  var status by remember {
    mutableStateOf(
      context.getString(if (file.exists()) R.string.ed_existing else R.string.ed_new)
    )
  }

  fun save(): Boolean =
    runCatching {
        file.parentFile?.mkdirs()
        file.writeText(text)
      }
      .onSuccess { status = context.getString(R.string.ed_saved, text.toByteArray().size) }
      .onFailure { status = context.getString(R.string.ed_error, it.message.orEmpty()) }
      .isSuccess

  BackHandler { vm.closeEditor(null) }

  Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
    Text(
      text = "┌─[ NANO ${vm.fs.prettyPath(file)}${if (text != originalText) " *" else ""} ]" + "─".repeat(30),
      color = palette.accent,
      maxLines = 1,
      overflow = TextOverflow.Clip,
      style = MaterialTheme.typography.bodyLarge,
    )
    BasicTextField(
      value = text,
      onValueChange = { text = it },
      textStyle =
        TextStyle(
          color = palette.fg,
          fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
          fontSize = MaterialTheme.typography.bodyLarge.fontSize,
          lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
        ),
      cursorBrush = SolidColor(palette.fg),
      modifier =
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
    )
    Text(
      text = "─".repeat(48),
      color = palette.dim,
      maxLines = 1,
      overflow = TextOverflow.Clip,
      style = MaterialTheme.typography.bodyMedium,
    )
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
      Text(
        text = stringResource(R.string.ed_save),
        color = palette.accent,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.clickable { save() }.padding(end = 12.dp),
      )
      Text(
        text = stringResource(R.string.ed_exit),
        color = palette.accent,
        style = MaterialTheme.typography.bodyLarge,
        modifier =
          Modifier.clickable {
              val msg =
                if (text != originalText && save())
                  context.getString(R.string.nano_saved, vm.fs.prettyPath(file))
                else null
              vm.closeEditor(msg)
            }
            .padding(end = 12.dp),
      )
      Text(
        text = status,
        color = palette.dim,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyLarge,
      )
    }
  }
}
