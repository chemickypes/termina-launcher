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

package com.example.terminalauncher.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class ContactEntry(val name: String, val phone: String)

class ContactsRepository(private val context: Context) {

  fun hasPermission(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
      PackageManager.PERMISSION_GRANTED

  fun contacts(): List<ContactEntry> {
    if (!hasPermission()) return emptyList()
    val result = LinkedHashMap<String, ContactEntry>()
    val projection =
      arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
      )
    context.contentResolver
      .query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC",
      )
      ?.use { cursor ->
        while (cursor.moveToNext()) {
          val name = cursor.getString(0) ?: continue
          val phone = cursor.getString(1) ?: continue
          // un contatto per nome, primo numero trovato
          result.putIfAbsent(name, ContactEntry(name, phone))
        }
      }
    return result.values.toList()
  }
}
