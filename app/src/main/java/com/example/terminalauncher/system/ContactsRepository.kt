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
