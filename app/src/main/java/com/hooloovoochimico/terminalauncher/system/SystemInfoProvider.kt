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

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.SystemClock
import androidx.annotation.StringRes
import com.hooloovoochimico.terminalauncher.R
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Letture di sistema formattate come righe di testo per il terminale. */
class SystemInfoProvider(private val context: Context) {

  private fun str(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

  fun battery(): List<String> {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val plugged = sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val tempC = (sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
    val voltage = sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

    val statusText =
      str(
        when (status) {
          BatteryManager.BATTERY_STATUS_CHARGING -> R.string.bat_charging
          BatteryManager.BATTERY_STATUS_FULL -> R.string.bat_full
          BatteryManager.BATTERY_STATUS_DISCHARGING -> R.string.bat_discharging
          BatteryManager.BATTERY_STATUS_NOT_CHARGING -> R.string.bat_not_charging
          else -> R.string.bat_unknown
        }
      )
    val source =
      when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> " (AC)"
        BatteryManager.BATTERY_PLUGGED_USB -> " (USB)"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> " (wireless)"
        else -> ""
      }
    return listOf(
      "[${bar(level)}] $level%",
      str(R.string.bat_status, statusText + source),
      str(R.string.bat_temp, "%.1f".format(Locale.US, tempC)),
      str(R.string.bat_volt, (voltage / 1000.0).toString()),
    )
  }

  fun deviceInfo(): List<String> =
    listOf(
      str(R.string.info_model, "${Build.MANUFACTURER} ${Build.MODEL}"),
      str(R.string.info_device, "${Build.DEVICE} (${Build.PRODUCT})"),
      str(R.string.info_android, Build.VERSION.RELEASE, Build.VERSION.SDK_INT),
      str(R.string.info_patch, Build.VERSION.SECURITY_PATCH),
      str(R.string.info_build, Build.DISPLAY),
      str(R.string.info_cpu, Build.SUPPORTED_ABIS.joinToString(", ")),
      str(R.string.info_screen, screenResolution()),
    )

  private fun screenResolution(): String {
    val dm = context.resources.displayMetrics
    return "${dm.widthPixels}x${dm.heightPixels} @ ${dm.densityDpi}dpi"
  }

  fun ipAddresses(): List<String> {
    val lines = mutableListOf<String>()
    // Sorgente primaria: ConnectivityManager → indirizzo della/e rete/i attiva/e con
    // etichetta leggibile (Wi-Fi / dati / Ethernet / VPN). NetworkInterface da solo
    // su molti device restituisce una lista vuota, da cui il vecchio "nessuna interfaccia".
    runCatching {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      cm?.allNetworks?.forEach { net ->
        val caps = cm.getNetworkCapabilities(net) ?: return@forEach
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return@forEach
        val lp = cm.getLinkProperties(net) ?: return@forEach
        val label = transportLabel(caps)
        lp.linkAddresses
          .map { it.address }
          .filter { !it.isLoopbackAddress && it is Inet4Address }
          .forEach { lines += "${label.padEnd(10)} ${it.hostAddress}" }
      }
    }
    // Fallback (es. permesso assente o nessuna rete riconosciuta): elenco grezzo.
    if (lines.isEmpty()) {
      runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { ni ->
          ni.inetAddresses.toList()
            .filter { !it.isLoopbackAddress && it is Inet4Address }
            .forEach { lines += "${ni.name.padEnd(10)} ${it.hostAddress}" }
        }
      }
    }
    return lines.ifEmpty { listOf(str(R.string.ip_none)) }
  }

  private fun transportLabel(caps: NetworkCapabilities): String =
    when {
      caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> str(R.string.ip_wifi)
      caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> str(R.string.ip_cellular)
      caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> str(R.string.ip_ethernet)
      caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> str(R.string.ip_vpn)
      else -> str(R.string.ip_other)
    }

  /** True se l'utente ha concesso l'accesso alle statistiche d'uso (permesso speciale). */
  fun hasUsageAccess(): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
          AppOpsManager.OPSTR_GET_USAGE_STATS,
          Process.myUid(),
          context.packageName,
        )
      } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
          AppOpsManager.OPSTR_GET_USAGE_STATS,
          Process.myUid(),
          context.packageName,
        )
      }
    return mode == AppOpsManager.MODE_ALLOWED
  }

  /**
   * Tempo di schermo per app dalla mezzanotte di oggi (top [limit]), risolvendo le
   * etichette tramite [labelOf]. Richiede [hasUsageAccess]; off main thread (chiamato da IO).
   */
  fun appUsageToday(limit: Int = 8, labelOf: (String) -> String?): List<String> {
    val usm =
      context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        ?: return listOf(str(R.string.usage_unavailable))
    val start =
      Calendar.getInstance()
        .apply {
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        .timeInMillis
    val now = System.currentTimeMillis()
    // Niente queryUsageStats(INTERVAL_DAILY): i suoi bucket giornalieri includono tempo
    // di ieri (di fatto ~24h mobili). Ricostruiamo il foreground time SOLO da mezzanotte
    // sommando gli eventi foreground/background dentro [start, now].
    val totals = HashMap<String, Long>()
    val fgStart = HashMap<String, Long>() // package → istante in cui è passato in foreground
    // MOVE_TO_FOREGROUND/BACKGROUND sono in realtà ACTIVITY_RESUMED/PAUSED: eventi per-activity,
    // non per-app, quindi NON arrivano in coppie pulite. Un background "spaiato" (senza foreground
    // corrispondente nella finestra) va contato da mezzanotte SOLO se è il primissimo evento che
    // vediamo per quel package — cioè l'app era già aperta prima di mezzanotte. Per i background
    // spaiati successivi non contiamo nulla, altrimenti ripartiremmo ogni volta da mezzanotte
    // sommando intervalli sovrapposti e gonfiando il totale oltre le 24h.
    val seen = HashSet<String>()
    val events = usm.queryEvents(start, now)
    val ev = UsageEvents.Event()
    while (events.hasNextEvent()) {
      events.getNextEvent(ev)
      val pkg = ev.packageName ?: continue
      when (ev.eventType) {
        UsageEvents.Event.MOVE_TO_FOREGROUND -> fgStart[pkg] = ev.timeStamp
        UsageEvents.Event.MOVE_TO_BACKGROUND -> {
          val from = fgStart.remove(pkg) ?: if (pkg !in seen) start else continue
          val dur = ev.timeStamp - from
          if (dur > 0) totals[pkg] = (totals[pkg] ?: 0L) + dur
        }
      }
      seen += pkg
    }
    // app ancora in foreground adesso: conta fino a ora.
    fgStart.forEach { (pkg, from) ->
      val dur = now - from
      if (dur > 0) totals[pkg] = (totals[pkg] ?: 0L) + dur
    }
    // Il tempo speso dentro TerminaLauncher non conta: lo escludiamo dal totale e dalla lista.
    totals.remove(context.packageName)
    // Rete di sicurezza: nessuna app può aver usato più del tempo trascorso da mezzanotte.
    val elapsed = now - start
    totals.replaceAll { _, ms -> ms.coerceAtMost(elapsed) }
    if (totals.isEmpty()) return listOf(str(R.string.usage_none))
    val top = totals.entries.sortedByDescending { it.value }.take(limit)
    val grand = totals.values.sum()
    val rows = mutableListOf(str(R.string.usage_total, formatDuration(grand)))
    top.forEach { (pkg, ms) ->
      val name = labelOf(pkg) ?: pkg
      rows += "${formatDuration(ms).padEnd(8)} $name"
    }
    return rows
  }

  private fun formatDuration(ms: Long): String {
    val totalMin = ms / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
  }

  fun ram(): List<String> {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mi = ActivityManager.MemoryInfo()
    am.getMemoryInfo(mi)
    val used = mi.totalMem - mi.availMem
    val pct = (used * 100 / mi.totalMem).toInt()
    return listOf(
      str(R.string.pct_used, bar(pct), pct),
      str(R.string.mem_total, formatBytes(mi.totalMem)),
      str(R.string.mem_available, formatBytes(mi.availMem)),
    )
  }

  fun storage(): List<String> {
    val stat = StatFs(Environment.getDataDirectory().path)
    val total = stat.totalBytes
    val free = stat.availableBytes
    val pct = ((total - free) * 100 / total).toInt()
    return listOf(
      str(R.string.pct_used, bar(pct), pct),
      str(R.string.mem_total, formatBytes(total)),
      str(R.string.disk_free, formatBytes(free)),
    )
  }

  fun dateTime(): List<String> {
    val fmt = SimpleDateFormat("EEEE d MMMM yyyy, HH:mm:ss", Locale.getDefault())
    return listOf(fmt.format(Date()), str(R.string.date_tz, java.util.TimeZone.getDefault().id))
  }

  fun time(): List<String> {
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return listOf(str(R.string.time_now, fmt.format(Date())))
  }

  fun uptime(): List<String> {
    val ms = SystemClock.elapsedRealtime()
    val d = ms / 86_400_000
    val h = (ms / 3_600_000) % 24
    val m = (ms / 60_000) % 60
    return listOf(str(R.string.uptime_fmt, d, h, m))
  }

  fun language(): List<String> {
    val locale = Locale.getDefault()
    return listOf(str(R.string.lang_fmt, locale.displayLanguage, locale.toLanguageTag()))
  }

  private fun bar(pct: Int): String {
    val filled = (pct.coerceIn(0, 100) * 20) / 100
    return "█".repeat(filled) + "░".repeat(20 - filled)
  }

  private fun formatBytes(bytes: Long): String {
    val gb = bytes / 1_073_741_824.0
    return if (gb >= 1) "%.2f GB".format(Locale.US, gb)
    else "%.0f MB".format(Locale.US, bytes / 1_048_576.0)
  }
}
