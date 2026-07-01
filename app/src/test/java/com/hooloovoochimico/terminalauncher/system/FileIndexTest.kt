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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Costruzione dell'indice e ranking della ricerca. La radice è una temp dir
 * controllata, così il numero di voci e l'ordine dei risultati sono deterministici.
 */
@RunWith(RobolectricTestRunner::class)
class FileIndexTest {

  @get:Rule val tmp = TemporaryFolder()

  private lateinit var index: FileIndex
  private lateinit var root: File

  @Before
  fun setup() {
    index = FileIndex(ApplicationProvider.getApplicationContext<Context>())
    index.clearCache()
    root = tmp.newFolder("home")
    File(root, "alpha.txt").writeText("a")
    File(root, "beta.txt").writeText("b")
    File(root, "sub").mkdirs()
    File(root, "sub/alphabet.md").writeText("c")
  }

  @Test
  fun buildIndexesEveryEntry() = runTest {
    val n = index.build(root) {}
    assertTrue(index.isBuilt)
    // alpha.txt, beta.txt, sub/, sub/alphabet.md
    assertEquals(4, n)
  }

  @Test
  fun searchRanksExactMatchFirst() = runTest {
    index.build(root) {}
    val hits = index.search("alpha.txt")
    assertTrue("l'esatto deve essere presente", hits.isNotEmpty())
    assertEquals("alpha.txt", hits.first().file.name)
    assertEquals("il match esatto ha score 0", 0, hits.first().score)
  }

  @Test
  fun searchMatchesSubstringsAcrossEntries() = runTest {
    index.build(root) {}
    val names = index.search("alpha").map { it.file.name }.toSet()
    assertTrue(names.contains("alpha.txt"))
    assertTrue(names.contains("alphabet.md"))
  }

  @Test
  fun searchReturnsEmptyForNoMatch() = runTest {
    index.build(root) {}
    assertTrue(index.search("zzzznope").isEmpty())
  }

  @Test
  fun searchSkipsFilesRemovedAfterIndexing() = runTest {
    index.build(root) {}
    assertTrue(File(root, "alpha.txt").delete())
    assertTrue(
      "un file cancellato dopo l'indicizzazione non deve comparire",
      index.search("alpha.txt").isEmpty(),
    )
  }

  @Test
  fun cacheRoundTripsThroughDisk() = runTest {
    val built = index.build(root) {}
    val fresh = FileIndex(ApplicationProvider.getApplicationContext<Context>())
    assertEquals(built, fresh.loadFromCache())
    assertTrue(fresh.isBuilt)
  }

  @Test
  fun clearCacheEmptiesIndexAndMarksStale() = runTest {
    index.build(root) {}
    index.clearCache()
    assertFalse(index.isBuilt)
    assertTrue("senza cache l'indice è stantio", index.isStale(root))
  }
}
