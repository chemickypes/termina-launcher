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

package com.hooloovoochimico.terminalauncher.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.hooloovoochimico.terminalauncher.R
import com.hooloovoochimico.terminalauncher.terminal.TerminalViewModel
import com.hooloovoochimico.terminalauncher.theme.TerminaLauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Test UI Compose delle schermate principali, eseguiti su JVM con Robolectric.
 * Verificano che ogni schermata si componga e mostri i propri elementi chiave, e
 * che le interazioni base (digitare un comando, toccare una riga) arrivino al VM.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreensUiTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var app: Application
  private lateinit var vm: TerminalViewModel

  @Before
  fun setup() {
    app = ApplicationProvider.getApplicationContext()
    app.getSharedPreferences("termina_prefs", 0).edit().clear().commit()
    vm = TerminalViewModel(app)
  }

  private fun show(content: @Composable () -> Unit) {
    composeRule.setContent { TerminaLauncherTheme(palette = vm.palette) { content() } }
  }

  private fun str(id: Int, vararg args: Any) = app.getString(id, *args)

  @Test
  fun terminalScreen_rendersBannerAndPrompt() {
    show { TerminalScreen(vm) }
    composeRule.onNodeWithText("TERMINA LAUNCHER", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("termina:", substring = true).assertIsDisplayed()
  }

  @Test
  fun terminalScreen_typingCommandReachesViewModel() {
    show { TerminalScreen(vm) }
    composeRule.onNode(hasSetTextAction()).performTextInput("/clear")
    composeRule.onNode(hasSetTextAction()).performImeAction()
    composeRule.waitForIdle()
    assertEquals("/clear", vm.history.last())
  }

  @Test
  fun terminalScreen_showsCommandSuggestionsWhileTypingSlash() {
    show { TerminalScreen(vm) }
    composeRule.onNode(hasSetTextAction()).performTextInput("/set")
    composeRule.waitForIdle()
    // il suggeritore propone "[/settings]"
    composeRule.onNodeWithText("/settings", substring = true).assertIsDisplayed()
  }

  @Test
  fun appsScreen_rendersTitle() {
    show { AppsScreen(vm, onBack = {}) }
    composeRule.onNodeWithText(str(R.string.apps_title), substring = true).assertIsDisplayed()
  }

  @Test
  fun settingsScreen_rendersTitleAndThemeRow() {
    show { SettingsScreen(vm, onBack = {}) }
    composeRule.onNodeWithText(str(R.string.settings_title), substring = true).assertIsDisplayed()
    // la riga tema mostra il nome della palette corrente
    composeRule.onNodeWithText("green", substring = true).assertIsDisplayed()
  }

  @Test
  fun settingsScreen_tapThemeRowCyclesPalette() {
    show { SettingsScreen(vm, onBack = {}) }
    assertEquals("green", vm.palette.name)
    composeRule.onNode(hasText("green", substring = true)).performClick()
    composeRule.waitForIdle()
    assertEquals("amber", vm.palette.name)
  }

  @Test
  fun settingsScreen_backButtonInvokesCallback() {
    var backCalled = false
    show { SettingsScreen(vm, onBack = { backCalled = true }) }
    composeRule.onNodeWithText("ESC").performClick()
    composeRule.waitForIdle()
    assertTrue(backCalled)
  }

  @Test
  fun historyScreen_listsSubmittedCommands() {
    vm.submit("/battery")
    show { HistoryScreen(vm, onBack = {}) }
    composeRule.onNodeWithText(str(R.string.history_title), substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("/battery", substring = true).assertIsDisplayed()
  }

  @Test
  fun aboutScreen_rendersTitleAndAppName() {
    show { AboutScreen(vm, onBack = {}) }
    composeRule.onNodeWithText(str(R.string.about_title), substring = true).assertIsDisplayed()
    composeRule.onNodeWithText(str(R.string.app_name), substring = true).assertIsDisplayed()
  }
}
