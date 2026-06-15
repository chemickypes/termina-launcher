package com.example.terminalauncher

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
import com.example.terminalauncher.terminal.TermScreen
import com.example.terminalauncher.terminal.TerminalViewModel
import com.example.terminalauncher.theme.TerminaLauncherTheme
import com.example.terminalauncher.ui.AppsScreen
import com.example.terminalauncher.ui.ContactsScreen
import com.example.terminalauncher.ui.EditorScreen
import com.example.terminalauncher.ui.ManualScreen
import com.example.terminalauncher.ui.RecoveryOverlay
import com.example.terminalauncher.ui.SearchScreen
import com.example.terminalauncher.ui.recoveryHoldGesture
import com.example.terminalauncher.ui.rememberRecoveryState
import com.example.terminalauncher.ui.restartApp
import com.example.terminalauncher.ui.SettingsScreen
import com.example.terminalauncher.ui.TerminalScreen

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
        }
        // countdown puramente visivo (nessun pointerInput → non intercetta i tap)
        RecoveryOverlay(recovery)
      }
    }
  }
}
