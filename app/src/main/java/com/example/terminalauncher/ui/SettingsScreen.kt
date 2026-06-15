package com.example.terminalauncher.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.terminalauncher.R
import com.example.terminalauncher.terminal.TerminalViewModel
import com.example.terminalauncher.theme.LocalTermPalette
import com.example.terminalauncher.theme.TermPalettes

@Composable
fun SettingsScreen(vm: TerminalViewModel, onBack: () -> Unit) {
  val palette = LocalTermPalette.current
  val context = LocalContext.current

  BackHandler(onBack = onBack)

  fun openSystem(action: String) {
    runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
  }

  val fontScales =
    listOf(
      0.85f to stringResource(R.string.font_small),
      1f to stringResource(R.string.font_normal),
      1.2f to stringResource(R.string.font_large),
    )
  val currentScaleLabel =
    fontScales.firstOrNull { it.first == vm.fontScale }?.second
      ?: stringResource(R.string.font_normal)
  val tapToChange = stringResource(R.string.set_tap_change)

  TuiFrame(
    title = stringResource(R.string.settings_title),
    footer = stringResource(R.string.settings_footer),
    onBack = onBack,
  ) { modifier ->
    LazyColumn(modifier = modifier) {
      item {
        Text(
          text = "│ ─── ${stringResource(R.string.sec_launcher)} ───",
          color = palette.dim,
          style = MaterialTheme.typography.bodyLarge,
        )
      }
      item {
        TuiRow(
          index = null,
          text = stringResource(R.string.set_theme, vm.palette.name),
          detail = tapToChange,
        ) {
          val all = TermPalettes.all
          val next = all[(all.indexOf(vm.palette) + 1) % all.size]
          vm.applyTheme(next)
        }
      }
      item {
        TuiRow(
          index = null,
          text = stringResource(R.string.set_font, currentScaleLabel),
          detail = tapToChange,
        ) {
          val idx = fontScales.indexOfFirst { it.first == vm.fontScale }
          vm.applyFontScale(fontScales[(idx + 1) % fontScales.size].first)
        }
      }
      item {
        TuiRow(
          index = null,
          text = stringResource(R.string.set_config),
          detail = stringResource(R.string.set_config_detail),
        ) {
          vm.openConfig()
        }
      }
      item {
        Text(
          text = "│ ─── ${stringResource(R.string.sec_system)} ───",
          color = palette.dim,
          style = MaterialTheme.typography.bodyLarge,
        )
      }
      item {
        TuiRow(
          index = null,
          text = stringResource(R.string.label_system_settings),
          detail = stringResource(R.string.set_sys_detail),
        ) {
          openSystem(Settings.ACTION_SETTINGS)
        }
      }
      item {
        TuiRow(
          index = null,
          text = stringResource(R.string.set_wifi),
          detail = stringResource(R.string.set_wifi_detail),
        ) {
          openSystem(Settings.ACTION_WIFI_SETTINGS)
        }
      }
      item {
        TuiRow(
          index = null,
          text = stringResource(R.string.set_bt),
          detail = stringResource(R.string.set_bt_detail),
        ) {
          openSystem(Settings.ACTION_BLUETOOTH_SETTINGS)
        }
      }
      item {
        TuiRow(
          index = null,
          text = stringResource(R.string.set_lang),
          detail = stringResource(R.string.set_lang_detail),
        ) {
          openSystem(Settings.ACTION_LOCALE_SETTINGS)
        }
      }
      item {
        TuiRow(
          index = null,
          text = stringResource(R.string.set_display),
          detail = stringResource(R.string.set_display_detail),
        ) {
          openSystem(Settings.ACTION_DISPLAY_SETTINGS)
        }
      }
      item {
        TuiRow(
          index = null,
          text = stringResource(R.string.set_home),
          detail = stringResource(R.string.set_home_detail),
        ) {
          openSystem(Settings.ACTION_HOME_SETTINGS)
        }
      }
    }
  }
}
