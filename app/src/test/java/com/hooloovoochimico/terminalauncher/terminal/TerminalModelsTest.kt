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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Invarianti del catalogo comandi. Sono bug reali se vengono violate: un nome
 * duplicato o senza prefisso renderebbe un comando irraggiungibile o ambiguo.
 */
class TerminalModelsTest {

  @Test
  fun slashCommands_haveUniqueNames() {
    val names = COMMANDS.map { it.name }
    assertEquals("ci sono nomi di comando duplicati", names.size, names.toSet().size)
  }

  @Test
  fun slashCommands_allStartWithSlash() {
    val offenders = COMMANDS.filterNot { it.name.startsWith("/") }
    assertTrue("comandi senza prefisso '/': $offenders", offenders.isEmpty())
  }

  @Test
  fun slashCommands_haveNonBlankNamesAndResources() {
    COMMANDS.forEach {
      assertTrue("nome troppo corto: '${it.name}'", it.name.length > 1)
      assertTrue("usageRes mancante per ${it.name}", it.usageRes != 0)
      assertTrue("descRes mancante per ${it.name}", it.descRes != 0)
    }
  }

  @Test
  fun coreCommands_arePresent() {
    val names = COMMANDS.map { it.name }.toSet()
    listOf("/help", "/clear", "/apps", "/settings", "/search").forEach {
      assertTrue("comando fondamentale assente: $it", it in names)
    }
  }

  @Test
  fun unixCommands_areLowercaseAndWithoutSlash() {
    assertTrue("il set dei comandi unix è vuoto", UNIX_COMMANDS.isNotEmpty())
    UNIX_COMMANDS.forEach {
      assertTrue("comando unix non valido: '$it'", it.isNotBlank() && !it.startsWith("/"))
      assertEquals("comando unix non in minuscolo: '$it'", it.lowercase(), it)
    }
  }

  @Test
  fun unixHelp_isPopulatedWithDistinctSyntax() {
    assertTrue("l'help unix è vuoto", UNIX_HELP.isNotEmpty())
    val syntaxes = UNIX_HELP.map { it.first }
    assertEquals("righe di help unix duplicate", syntaxes.size, syntaxes.toSet().size)
  }
}
