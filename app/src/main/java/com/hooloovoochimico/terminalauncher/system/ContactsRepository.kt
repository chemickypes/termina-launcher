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

package com.hooloovoochimico.terminalauncher.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** Un singolo numero di un contatto, con l'etichetta del tipo (mobile/casa/lavoro…). */
data class ContactPhone(val number: String, val label: String)

data class ContactEntry(val name: String, val phones: List<ContactPhone>) {
  /** Primo numero del contatto (stringa vuota se per qualche motivo non ne ha). */
  val primaryPhone: String
    get() = phones.firstOrNull()?.number.orEmpty()

  val hasMultiple: Boolean
    get() = phones.size > 1
}

class ContactsRepository(private val context: Context) {

  fun hasPermission(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
      PackageManager.PERMISSION_GRANTED

  /** Accumulatore mutabile usato durante la scansione del cursore. */
  private class Builder(val name: String) {
    val phones = mutableListOf<ContactPhone>()
    val seen = mutableSetOf<String>()
  }

  fun contacts(): List<ContactEntry> {
    if (!hasPermission()) return emptyList()
    // Raggruppa per CONTACT_ID così da raccogliere TUTTI i numeri di un contatto,
    // non solo il primo. LinkedHashMap mantiene l'ordine alfabetico della query.
    val byId = LinkedHashMap<Long, Builder>()
    val projection =
      arrayOf(
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.TYPE,
        ContactsContract.CommonDataKinds.Phone.LABEL,
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
        val res = context.resources
        while (cursor.moveToNext()) {
          val id = cursor.getLong(0)
          val name = cursor.getString(1) ?: continue
          val number = cursor.getString(2) ?: continue
          val type = cursor.getInt(3)
          val custom = cursor.getString(4)
          val label =
            ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, type, custom).toString()
          val builder = byId.getOrPut(id) { Builder(name) }
          // dedup sullo stesso numero (le sole differenze di spazi/punteggiatura
          // generano duplicati): confronta solo cifre e '+'.
          val key = number.filter { it.isDigit() || it == '+' }
          if (builder.seen.add(key)) builder.phones.add(ContactPhone(number, label))
        }
      }
    return byId.values.map { ContactEntry(it.name, it.phones.toList()) }
  }
}
