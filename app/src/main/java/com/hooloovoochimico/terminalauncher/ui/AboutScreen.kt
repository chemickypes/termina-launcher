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

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hooloovoochimico.terminalauncher.BuildConfig
import com.hooloovoochimico.terminalauncher.R
import com.hooloovoochimico.terminalauncher.terminal.TerminalViewModel
import com.hooloovoochimico.terminalauncher.theme.LocalTermPalette

/**
 * Pagina info / crediti in stile terminale: nome, versione, licenza, autore e un
 * link (cliccabile) al repository del progetto — dove vive anche l'edizione
 * open source completa (accesso a tutto il filesystem) non disponibile su Play.
 */
@Composable
fun AboutScreen(vm: TerminalViewModel, onBack: () -> Unit) {
  val palette = LocalTermPalette.current
  val context = LocalContext.current
  val url = stringResource(R.string.about_url)
  val edition = if (BuildConfig.FULL_ACCESS) "-COMPLETE" else ""

  BackHandler(onBack = onBack)

  TuiFrame(
    title = stringResource(R.string.about_title),
    footer = stringResource(R.string.about_footer),
    onBack = onBack,
  ) { modifier ->
    LazyColumn(modifier = modifier) {
      item {
        Text(
          text = "│ ${stringResource(R.string.app_name)}",
          color = palette.accent,
          style = MaterialTheme.typography.bodyLarge,
        )
      }
      item {
        Text(
          text = "│ ${stringResource(R.string.about_version, "${BuildConfig.VERSION_NAME}$edition", BuildConfig.VERSION_CODE)}",
          color = palette.fg,
          style = MaterialTheme.typography.bodyLarge,
        )
      }
      item {
        Text(
          text = "│ ${stringResource(R.string.about_author)}",
          color = palette.fg,
          style = MaterialTheme.typography.bodyLarge,
        )
      }
      item {
        Text(
          text = "│ ${stringResource(R.string.about_license)}",
          color = palette.fg,
          style = MaterialTheme.typography.bodyLarge,
        )
      }
      item { Text(text = "│", color = palette.dim, style = MaterialTheme.typography.bodyLarge) }
      item {
        Text(
          text = "│ ${stringResource(R.string.about_credits)}",
          color = palette.dim,
          style = MaterialTheme.typography.bodyLarge,
        )
      }
      item { Text(text = "│", color = palette.dim, style = MaterialTheme.typography.bodyLarge) }
      item {
        TuiRow(
          index = null,
          text = stringResource(R.string.about_link),
          detail = url,
        ) {
          runCatching {
            context.startActivity(
              Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
          }
        }
      }
    }
  }
}
