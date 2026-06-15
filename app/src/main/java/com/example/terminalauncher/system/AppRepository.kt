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
