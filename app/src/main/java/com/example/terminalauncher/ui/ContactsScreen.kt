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

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import com.example.terminalauncher.R
import com.example.terminalauncher.system.ContactEntry
import com.example.terminalauncher.terminal.TerminalViewModel
import com.example.terminalauncher.theme.LocalTermPalette

@Composable
fun ContactsScreen(vm: TerminalViewModel, onBack: () -> Unit) {
  val palette = LocalTermPalette.current
  var hasPermission by remember { mutableStateOf(vm.contactsRepository.hasPermission()) }
  val contacts = vm.contactsList
  var filter by remember { mutableStateOf("") }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasPermission = granted
      if (granted) vm.loadContacts(force = true)
    }

  LaunchedEffect(hasPermission) { if (hasPermission) vm.loadContacts() }
  BackHandler(onBack = onBack)

  val visible: List<ContactEntry> =
    if (filter.isBlank()) contacts
    else contacts.filter { it.name.contains(filter.trim(), ignoreCase = true) }

  TuiFrame(
    title = stringResource(R.string.contacts_title),
    footer = stringResource(R.string.contacts_footer, visible.size),
    onBack = onBack,
  ) { modifier ->
    if (!hasPermission) {
      Column(modifier = modifier.padding(vertical = 12.dp)) {
        Text(
          text = "│ " + stringResource(R.string.contacts_perm_missing),
          color = palette.error,
          style = MaterialTheme.typography.bodyLarge,
        )
        Text(
          text = "│ " + stringResource(R.string.contacts_grant),
          color = palette.accent,
          style = MaterialTheme.typography.bodyLarge,
          modifier =
            Modifier.padding(top = 8.dp).clickable {
              permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
        )
      }
    } else {
      LazyColumn(modifier = modifier) {
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
        if (vm.contactsLoading && contacts.isEmpty()) {
          item { TuiLoading(stringResource(R.string.loading_contacts)) }
        }
        itemsIndexed(visible, key = { _, c -> c.name + c.phone }) { index, contact ->
          TuiRow(index = index + 1, text = contact.name, detail = contact.phone) {
            vm.dial(contact.phone)
          }
        }
      }
    }
  }
}
