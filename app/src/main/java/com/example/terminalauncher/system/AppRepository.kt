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

import android.content.ComponentName
import android.content.Context
import android.content.Intent

data class AppEntry(val label: String, val packageName: String, val activityName: String)

class AppRepository(private val context: Context) {

  @Volatile private var cache: List<AppEntry> = emptyList()

  fun apps(forceRefresh: Boolean = false): List<AppEntry> {
    if (cache.isEmpty() || forceRefresh) {
      val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
      cache =
        context.packageManager
          .queryIntentActivities(intent, 0)
          .map {
            AppEntry(
              label = it.loadLabel(context.packageManager).toString(),
              packageName = it.activityInfo.packageName,
              activityName = it.activityInfo.name,
            )
          }
          .filter { it.packageName != context.packageName }
          .sortedBy { it.label.lowercase() }
    }
    return cache
  }

  /** Cerca per nome: prima match esatto, poi prefisso, poi sottostringa. */
  fun search(query: String): List<AppEntry> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    val all = apps()
    val exact = all.filter { it.label.lowercase() == q }
    if (exact.isNotEmpty()) return exact
    val prefix = all.filter { it.label.lowercase().startsWith(q) }
    if (prefix.isNotEmpty()) return prefix
    return all.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
  }

  fun launch(app: AppEntry): Boolean =
    runCatching {
        val intent =
          Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.packageName, app.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
      }
      .isSuccess
}
