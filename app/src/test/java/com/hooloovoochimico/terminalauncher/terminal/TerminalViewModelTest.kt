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

package com.hooloovoochimico.terminalauncher.terminal

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Smistamento dei comandi del terminale: verifica gli effetti osservabili di
 * `submit()` sullo stato del ViewModel (righe stampate, schermata attiva,
 * palette, history, alias) senza toccare hardware o intent di sistema.
 */
@RunWith(RobolectricTestRunner::class)
class TerminalViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var vm: TerminalViewModel

  @Before
  fun setup() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    app.getSharedPreferences("termina_prefs", 0).edit().clear().commit()
    vm = TerminalViewModel(app)
  }

  @Test
  fun startsWithBannerOnTerminalScreen() {
    assertEquals(TermScreen.TERMINAL, vm.screen)
    assertTrue("il banner deve essere stampato all'avvio", vm.lines.isNotEmpty())
    assertTrue(vm.lines.any { it.kind == LineKind.ACCENT && it.text.startsWith("╔") })
  }

  @Test
  fun blankInputIsIgnored() {
    val before = vm.lines.size
    vm.submit("   ")
    assertEquals(before, vm.lines.size)
    assertTrue(vm.history.isEmpty())
  }

  @Test
  fun submitRecordsHistoryAndEchoesInput() {
    vm.submit("/help")
    assertEquals("/help", vm.history.last())
    assertTrue(vm.lines.any { it.kind == LineKind.INPUT && it.text == "$ /help" })
  }

  @Test
  fun helpPrintsTheCommandCatalogue() {
    val before = vm.lines.size
    vm.submit("/help")
    // una riga di titolo + una riga per ciascun comando + sezione file system
    assertTrue(vm.lines.size > before + COMMANDS.size)
    assertTrue(vm.lines.any { it.text.contains("/help") })
  }

  @Test
  fun clearWipesScrollbackThenReprintsBanner() {
    vm.submit("/help")
    vm.submit("/clear")
    assertTrue("dopo /clear resta solo il banner", vm.lines.first().text.startsWith("╔"))
    assertTrue(vm.lines.none { it.text == "$ /help" })
  }

  @Test
  fun unknownSlashCommandReportsError() {
    vm.submit("/definitelynotacommand")
    assertEquals(LineKind.ERROR, vm.lines.last().kind)
  }

  @Test
  fun slashCommandsNavigateBetweenScreens() {
    vm.submit("/settings")
    assertEquals(TermScreen.SETTINGS, vm.screen)
    vm.submit("/apps")
    assertEquals(TermScreen.APPS, vm.screen)
    vm.submit("/contacts")
    assertEquals(TermScreen.CONTACTS, vm.screen)
    vm.submit("/history")
    assertEquals(TermScreen.HISTORY, vm.screen)
  }

  @Test
  fun themeCommandSwitchesPaletteAndRejectsUnknown() {
    assertEquals("green", vm.palette.name)
    vm.submit("/theme amber")
    assertEquals("amber", vm.palette.name)

    val current = vm.palette.name
    vm.submit("/theme nonexistent")
    assertEquals("una palette sconosciuta non cambia il tema", current, vm.palette.name)
    assertEquals(LineKind.ERROR, vm.lines.last().kind)
  }

  @Test
  fun aliasIsDefinedAndListed() {
    vm.submit("alias g=/help")
    val listingStart = vm.lines.size
    vm.submit("alias")
    assertTrue(
      "alias senza argomenti elenca gli alias definiti",
      vm.lines.drop(listingStart).any { it.text.contains("g=/help") },
    )
  }

  @Test
  fun aliasExpandsBeforeExecution() {
    vm.submit("alias g=/help")
    val before = vm.lines.size
    vm.submit("g")
    // "g" viene espanso in "/help" e stampa il catalogo comandi
    assertTrue(vm.lines.size > before + COMMANDS.size)
  }

  @Test
  fun unaliasRemovesAnAlias() {
    vm.submit("alias g=/help")
    vm.submit("unalias g")
    val before = vm.lines.size
    // "g" non è più un alias: senza corrispondenze diventa un tentativo di avvio app
    // (che fallisce con un errore) invece di espandersi in /help e stampare il catalogo.
    vm.submit("g")
    assertEquals(LineKind.ERROR, vm.lines.last().kind)
    assertTrue("g non deve più stampare il catalogo comandi", vm.lines.size < before + COMMANDS.size)
  }

  @Test
  fun freeTextWithoutMatchReportsAppNotFound() {
    vm.submit("zzznotaninstalledapp")
    assertEquals(LineKind.ERROR, vm.lines.last().kind)
  }
}
