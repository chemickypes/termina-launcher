package com.example.terminalauncher.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import androidx.annotation.StringRes
import com.example.terminalauncher.R
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
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
    runCatching {
      NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { ni ->
        ni.inetAddresses.toList()
          .filter { !it.isLoopbackAddress && it is Inet4Address }
          .forEach { lines += "${ni.name.padEnd(8)} ${it.hostAddress}" }
      }
    }
    return lines.ifEmpty { listOf(str(R.string.ip_none)) }
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
