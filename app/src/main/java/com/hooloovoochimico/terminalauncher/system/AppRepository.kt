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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager

/**
 * Una voce della lista app. [userHandle] è il profilo utente a cui appartiene:
 * `null` (o il profilo principale) = app personale, un profilo diverso = profilo
 * di lavoro. Serve per lanciare le app del profilo di lavoro via [LauncherApps].
 */
data class AppEntry(
  val label: String,
  val packageName: String,
  val activityName: String,
  val isWork: Boolean = false,
  val userHandle: UserHandle? = null,
)

class AppRepository(private val context: Context) {

  @Volatile private var cache: List<AppEntry> = emptyList()
  @Volatile private var workCache: List<AppEntry> = emptyList()

  // Istanza unica: register/unregister del callback devono avvenire sullo stesso oggetto.
  private val launcherApps: LauncherApps by lazy {
    context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
  }

  private var launcherCallback: LauncherApps.Callback? = null

  private val userManager: UserManager
    get() = context.getSystemService(Context.USER_SERVICE) as UserManager

  /** Profili utente diversi da quello principale (tipicamente il profilo di lavoro). */
  private fun workProfiles(): List<UserHandle> {
    val main = Process.myUserHandle()
    return runCatching { userManager.userProfiles.filter { it != main } }.getOrDefault(emptyList())
  }

  /** True se esiste almeno un profilo di lavoro sul dispositivo. */
  fun hasWorkProfile(): Boolean = workProfiles().isNotEmpty()

  /**
   * Inizia ad osservare installazioni/disinstallazioni/aggiornamenti via
   * [LauncherApps.Callback]: invalida la cache del profilo interessato e chiama
   * [onChanged] (sul main thread) così la UI può ricaricarsi. Idempotente.
   */
  fun startWatching(onChanged: () -> Unit) {
    if (launcherCallback != null) return
    val main = Process.myUserHandle()
    val cb =
      object : LauncherApps.Callback() {
        private fun invalidate(user: UserHandle?) {
          if (user == null || user == main) cache = emptyList() else workCache = emptyList()
          onChanged()
        }

        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = invalidate(user)

        override fun onPackageAdded(packageName: String?, user: UserHandle?) = invalidate(user)

        override fun onPackageChanged(packageName: String?, user: UserHandle?) = invalidate(user)

        override fun onPackagesAvailable(
          packageNames: Array<out String>?,
          user: UserHandle?,
          replacing: Boolean,
        ) = invalidate(user)

        override fun onPackagesUnavailable(
          packageNames: Array<out String>?,
          user: UserHandle?,
          replacing: Boolean,
        ) = invalidate(user)
      }
    runCatching { launcherApps.registerCallback(cb, Handler(Looper.getMainLooper())) }
    launcherCallback = cb
  }

  /** Smette di osservare (da chiamare in onCleared per non perdere il callback). */
  fun stopWatching() {
    launcherCallback?.let { cb -> runCatching { launcherApps.unregisterCallback(cb) } }
    launcherCallback = null
  }

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

  /** App installate nel/nei profilo/i di lavoro, enumerate via [LauncherApps]. */
  fun workApps(forceRefresh: Boolean = false): List<AppEntry> {
    if (workCache.isEmpty() || forceRefresh) {
      workCache =
        workProfiles()
          .flatMap { user ->
            runCatching {
                launcherApps.getActivityList(null, user).map { info ->
                  AppEntry(
                    label = info.label.toString(),
                    packageName = info.componentName.packageName,
                    activityName = info.componentName.className,
                    isWork = true,
                    userHandle = user,
                  )
                }
              }
              .getOrDefault(emptyList())
          }
          .sortedBy { it.label.lowercase() }
    }
    return workCache
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

  /** Nome leggibile per un package (usato da /usage). Cache app → PackageManager → null. */
  fun labelFor(packageName: String): String? {
    apps().firstOrNull { it.packageName == packageName }?.let { return it.label }
    return runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
      }
      .getOrNull()
  }

  fun launch(app: AppEntry): Boolean =
    runCatching {
        val component = ComponentName(app.packageName, app.activityName)
        if (app.isWork && app.userHandle != null) {
          // Le app del profilo di lavoro non si possono lanciare con un normale
          // startActivity: vanno avviate nel loro profilo via LauncherApps.
          launcherApps.startMainActivity(component, app.userHandle, null, null)
        } else {
          val intent =
            Intent(Intent.ACTION_MAIN)
              .addCategory(Intent.CATEGORY_LAUNCHER)
              .setComponent(component)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
          context.startActivity(intent)
        }
      }
      .isSuccess
}
