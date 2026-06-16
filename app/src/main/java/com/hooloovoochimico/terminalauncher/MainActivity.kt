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

package com.hooloovoochimico.terminalauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hooloovoochimico.terminalauncher.terminal.TermScreen
import com.hooloovoochimico.terminalauncher.terminal.TerminalViewModel
import com.hooloovoochimico.terminalauncher.theme.TerminaLauncherTheme
import com.hooloovoochimico.terminalauncher.ui.AppsScreen
import com.hooloovoochimico.terminalauncher.ui.ContactsScreen
import com.hooloovoochimico.terminalauncher.ui.EditorScreen
import com.hooloovoochimico.terminalauncher.ui.FileViewerScreen
import com.hooloovoochimico.terminalauncher.ui.HistoryScreen
import com.hooloovoochimico.terminalauncher.ui.ManualScreen
import com.hooloovoochimico.terminalauncher.ui.RecoveryOverlay
import com.hooloovoochimico.terminalauncher.ui.SearchScreen
import com.hooloovoochimico.terminalauncher.ui.recoveryHoldGesture
import com.hooloovoochimico.terminalauncher.ui.rememberRecoveryState
import com.hooloovoochimico.terminalauncher.ui.restartApp
import com.hooloovoochimico.terminalauncher.ui.SettingsScreen
import com.hooloovoochimico.terminalauncher.ui.TerminalScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { TerminaApp() }
  }
}

@Composable
fun TerminaApp(vm: TerminalViewModel = viewModel()) {
  LifecycleResumeEffect(Unit) {
    vm.onResumed()
    onPauseOrDispose {}
  }
  val context = LocalContext.current
  val recovery = rememberRecoveryState()
  TerminaLauncherTheme(palette = vm.palette) {
    val density = LocalDensity.current
    CompositionLocalProvider(
      LocalDensity provides Density(density.density, density.fontScale * vm.fontScale)
    ) {
      androidx.compose.foundation.layout.Box(
        // gesto di emergenza sul Box ROOT (antenato): non blocca i tap sottostanti
        modifier =
          Modifier.fillMaxSize()
            .background(vm.palette.bg)
            .systemBarsPadding()
            .imePadding()
            .recoveryHoldGesture(recovery) { restartApp(context) }
      ) {
        when (vm.screen) {
          TermScreen.TERMINAL -> TerminalScreen(vm)
          TermScreen.APPS -> AppsScreen(vm) { vm.screen = TermScreen.TERMINAL }
          TermScreen.CONTACTS -> ContactsScreen(vm) { vm.screen = TermScreen.TERMINAL }
          TermScreen.SETTINGS -> SettingsScreen(vm) { vm.screen = TermScreen.TERMINAL }
          TermScreen.EDITOR -> EditorScreen(vm)
          TermScreen.SEARCH -> SearchScreen(vm) { vm.closeSearch() }
          TermScreen.MANUAL -> ManualScreen(vm) { vm.closeManual() }
          TermScreen.VIEWER -> FileViewerScreen(vm) { vm.closeViewer() }
          TermScreen.HISTORY -> HistoryScreen(vm) { vm.closeHistory() }
        }
        // countdown puramente visivo (nessun pointerInput → non intercetta i tap)
        RecoveryOverlay(recovery)
      }
    }
  }
}
