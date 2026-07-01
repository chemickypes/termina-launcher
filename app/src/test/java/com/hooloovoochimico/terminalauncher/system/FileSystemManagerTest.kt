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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Comportamento della shell del file system. Senza accesso completo la home è la
 * cartella privata dell'app (Robolectric la mappa su una temp dir), quindi le
 * operazioni sono confinate e ripetibili. Ogni test parte da una home pulita.
 */
@RunWith(RobolectricTestRunner::class)
class FileSystemManagerTest {

  private lateinit var fs: FileSystemManager

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    fs = FileSystemManager(context)
    fs.home.listFiles()?.forEach { it.deleteRecursively() }
  }

  @Test
  fun startsInHomeWithTildePrompt() {
    assertEquals(fs.home.canonicalPath, fs.cwd.canonicalPath)
    assertEquals("~", fs.prettyPath())
  }

  @Test
  fun mkdirThenCdChangesCwd() {
    fs.mkdir("projects")
    assertTrue(File(fs.home, "projects").isDirectory)
    assertNull("cd valido deve ritornare null", fs.cd("projects"))
    assertEquals(File(fs.home, "projects").canonicalPath, fs.cwd.canonicalPath)
    assertEquals("~/projects", fs.prettyPath())
  }

  @Test
  fun cdRejectsMissingDirAndPlainFile() {
    assertNotNull("cd su dir inesistente deve dare errore", fs.cd("nope"))
    fs.touch("file.txt")
    assertNotNull("cd su un file deve dare errore", fs.cd("file.txt"))
  }

  @Test
  fun touchAndCatRoundTrip() {
    fs.resolve("notes.txt").writeText("riga1\nriga2")
    assertEquals(listOf("riga1", "riga2"), fs.cat("notes.txt"))
  }

  @Test
  fun lsHidesDotFilesUnlessAllRequested() {
    fs.touch(".secret")
    fs.touch("visible.txt")
    val normal = fs.list("", all = false, long = false)
    assertTrue(normal.any { it.contains("visible.txt") })
    assertFalse(normal.any { it.contains(".secret") })
    val withAll = fs.list("", all = true, long = false)
    assertTrue(withAll.any { it.contains(".secret") })
  }

  @Test
  fun headAndTailSliceTheFile() {
    fs.resolve("n.txt").writeText((1..5).joinToString("\n") { "l$it" })
    assertEquals(listOf("l1", "l2"), fs.headTail("n.txt", 2, fromEnd = false))
    assertEquals(listOf("l4", "l5"), fs.headTail("n.txt", 2, fromEnd = true))
  }

  @Test
  fun rmRequiresRecursiveFlagForDirectories() {
    fs.mkdir("d")
    val dir = File(fs.home, "d")
    fs.rm("d", recursive = false)
    assertTrue("rm senza -r non deve cancellare una cartella", dir.exists())
    fs.rm("d", recursive = true)
    assertFalse("rm -r deve cancellare la cartella", dir.exists())
  }

  @Test
  fun rmRefusesToDeleteHome() {
    fs.rm("~", recursive = true)
    assertTrue("la home non deve mai essere cancellabile", fs.home.exists())
  }

  @Test
  fun copyAndMoveFiles() {
    fs.resolve("a.txt").writeText("x")
    fs.cp("a.txt", "b.txt", recursive = false)
    assertTrue(File(fs.home, "b.txt").exists())
    assertTrue("cp non deve rimuovere la sorgente", File(fs.home, "a.txt").exists())
    fs.mv("b.txt", "c.txt")
    assertTrue(File(fs.home, "c.txt").exists())
    assertFalse("mv deve rimuovere la sorgente", File(fs.home, "b.txt").exists())
  }

  @Test
  fun resolveExpandsTilde() {
    assertEquals(fs.home.canonicalPath, fs.resolve("~").canonicalPath)
    assertEquals(File(fs.home, "x").canonicalPath, fs.resolve("~/x").canonicalPath)
  }

  @Test
  fun treeShowsNestedEntries() {
    fs.mkdir("top")
    fs.cd("top")
    fs.touch("leaf.txt")
    fs.cd("~")
    val tree = fs.tree("top", maxDepth = 2)
    assertTrue(tree.any { it.contains("leaf.txt") })
  }
}
