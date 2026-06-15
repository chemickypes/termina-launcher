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

package com.example.terminalauncher.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.terminalauncher.R
import com.example.terminalauncher.theme.LocalTermPalette
import kotlinx.coroutines.withTimeoutOrNull

/** Durata della pressione continua per far scattare il recovery. */
private const val HOLD_MS = 5000L
/** Da quanto tempo di pressione mostrare il countdown a schermo. */
private const val SHOW_AFTER_MS = 1200L

/** Stato condiviso del gesto di recovery (avanzamento della pressione). */
class RecoveryState {
  var holdElapsed by mutableStateOf(0L)
  var triggered by mutableStateOf(false)
}

@Composable fun rememberRecoveryState(): RecoveryState = remember { RecoveryState() }

/**
 * Rileva la pressione prolungata e ferma per [HOLD_MS] → riavvia l'app.
 *
 * IMPORTANTE: va applicato al Box ROOT (antenato di tutte le schermate), NON a un
 * overlay sopra il contenuto. Un antenato condivide gli eventi con i discendenti,
 * quindi osserva sul pass Initial SENZA consumare e i tap/scroll/clickable
 * sottostanti continuano a funzionare. (Un sibling sopra il contenuto, invece,
 * vincerebbe l'hit-test e bloccherebbe i tap.)
 *
 * Un movimento oltre lo slop annulla (= scroll, non pressione deliberata).
 */
fun Modifier.recoveryHoldGesture(state: RecoveryState, onTrigger: () -> Unit): Modifier =
  this.pointerInput(Unit) {
    val slopPx = 24.dp.toPx()
    awaitEachGesture {
      val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
      val startPos = down.position
      val start = System.currentTimeMillis()
      var cancelled = false
      while (true) {
        state.holdElapsed = System.currentTimeMillis() - start
        if (state.holdElapsed >= HOLD_MS) break
        val ev = withTimeoutOrNull(40) { awaitPointerEvent(PointerEventPass.Initial) }
        if (ev != null) {
          if (ev.changes.none { it.pressed }) {
            cancelled = true
            break
          }
          if (ev.changes.any { (it.position - startPos).getDistance() > slopPx }) {
            cancelled = true
            break
          }
        }
      }
      if (!cancelled && state.holdElapsed >= HOLD_MS && !state.triggered) {
        state.triggered = true
        onTrigger()
      }
      state.holdElapsed = 0L
    }
  }

/**
 * Countdown a schermo durante la pressione. È puramente visivo (nessun
 * pointerInput / clickable), quindi NON intercetta i tap.
 */
@Composable
fun RecoveryOverlay(state: RecoveryState) {
  if (state.holdElapsed < SHOW_AFTER_MS) return
  val palette = LocalTermPalette.current
  val seconds = (((HOLD_MS - state.holdElapsed) / 1000L) + 1).coerceAtLeast(1).toInt()
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
      modifier =
        Modifier.background(palette.bg.copy(alpha = 0.94f))
          .border(1.dp, palette.accent)
          .padding(horizontal = 22.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = "⟳ " + stringResource(R.string.recovery_title),
        color = palette.accent,
        style = MaterialTheme.typography.titleMedium,
      )
      Text(
        text = stringResource(R.string.recovery_countdown, seconds),
        color = palette.fg,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 6.dp),
      )
    }
  }
}

/** Riavvio completo: rilancia l'intent del launcher e termina il processo corrente. */
fun restartApp(context: Context) {
  val ctx = context.applicationContext
  val intent =
    ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
  if (intent != null) ctx.startActivity(intent)
  Runtime.getRuntime().exit(0)
}
