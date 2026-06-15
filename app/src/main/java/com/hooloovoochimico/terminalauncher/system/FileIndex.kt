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
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Un file/cartella nell'indice. `lower` è il nome in minuscolo pre-calcolato per la ricerca. */
data class IndexEntry(val path: String, val name: String, val lower: String, val isDir: Boolean)

data class SearchHit(val file: File, val isDir: Boolean, val score: Int)

/**
 * Indice dei file costruito una volta e tenuto in memoria + cache su disco privato.
 *
 * Scelte di sicurezza/robustezza per la scansione del filesystem:
 *  - gira SEMPRE off-main-thread (suspend, Dispatchers.IO) e controlla la cancellazione;
 *  - limiti rigidi: profondità max, numero max di voci, budget di tempo;
 *  - salta sottoalberi enormi/inutili ("Android" = dati app, cartelle nascoste di cache);
 *  - non segue i symlink e traccia le dir canoniche visitate per evitare cicli;
 *  - la cache su disco è una lista di righe "D|path" / "F|path" rigenerabile a comando;
 *  - prima di mostrare i risultati si verifica che i file esistano ancora (anti-stantio).
 */
class FileIndex(private val context: Context) {

  @Volatile var entries: List<IndexEntry> = emptyList()
    private set

  @Volatile var lastBuildMillis: Long = 0L
    private set

  val isBuilt: Boolean
    get() = entries.isNotEmpty()

  /** Età della cache su disco in ms (Long.MAX_VALUE se non esiste). Usata per il cooldown del prewarm. */
  val cacheAgeMillis: Long
    get() = if (cacheFile.exists()) System.currentTimeMillis() - cacheFile.lastModified() else Long.MAX_VALUE

  private val cacheFile: File
    get() = File(context.filesDir, "file_index.txt")

  companion object {
    private const val MAX_ENTRIES = 100_000
    private const val MAX_DEPTH = 12
    private const val TIME_BUDGET_MS = 12_000L
    private val SKIP_DIRS = setOf("Android", ".thumbnails", ".cache", "cache")
  }

  /**
   * Costruisce l'indice partendo da [root]. Sospendibile e cancellabile.
   * [onProgress] viene chiamato periodicamente col numero di voci raccolte.
   */
  suspend fun build(root: File, onProgress: (Int) -> Unit): Int =
    withContext(Dispatchers.IO) {
      val start = System.currentTimeMillis()
      val acc = ArrayList<IndexEntry>(4096)
      val visited = HashSet<String>()
      val stack = ArrayDeque<Pair<File, Int>>()
      stack.addLast(root to 0)

      while (stack.isNotEmpty()) {
        coroutineContext.ensureActive() // rispetta la cancellazione
        if (acc.size >= MAX_ENTRIES) break
        if (System.currentTimeMillis() - start > TIME_BUDGET_MS) break

        val (dir, depth) = stack.removeLast()
        if (depth > MAX_DEPTH) continue

        val canonical = runCatching { dir.canonicalPath }.getOrElse { dir.absolutePath }
        if (!visited.add(canonical)) continue // ciclo / symlink già visto

        val children = runCatching { dir.listFiles() }.getOrNull() ?: continue
        for (child in children) {
          if (acc.size >= MAX_ENTRIES) break
          val name = child.name
          val isDir = child.isDirectory
          acc +=
            IndexEntry(
              path = child.absolutePath,
              name = name,
              lower = name.lowercase(),
              isDir = isDir,
            )
          if (isDir && name !in SKIP_DIRS) {
            // non seguire symlink di directory
            val isLink = runCatching { child.canonicalPath != child.absolutePath }.getOrDefault(false)
            if (!isLink) stack.addLast(child to depth + 1)
          }
          if (acc.size % 2000 == 0) onProgress(acc.size)
        }
      }

      entries = acc
      lastBuildMillis = System.currentTimeMillis()
      onProgress(acc.size)
      runCatching { persist(acc) }
      acc.size
    }

  /**
   * Euristica di obsolescenza economica: l'indice è "stantio" se la cache manca, o se la
   * cartella radice o una delle sue sottocartelle dirette è stata modificata dopo la cache.
   * Il mtime di una directory cambia quando un suo figlio diretto viene aggiunto/rimosso/rinominato,
   * quindi questo cattura le aggiunte ai livelli alti (Download, DCIM…) senza scansionare tutto.
   * Le aggiunte profonde non vengono colte: per quelle resta `find --reindex`.
   */
  suspend fun isStale(root: File): Boolean =
    withContext(Dispatchers.IO) {
      if (!cacheFile.exists()) return@withContext true
      val cacheTime = cacheFile.lastModified()
      if (root.lastModified() > cacheTime) return@withContext true
      val children = runCatching { root.listFiles() }.getOrNull() ?: return@withContext false
      children.any { it.isDirectory && it.lastModified() > cacheTime }
    }

  /** Carica l'indice dalla cache su disco, se presente. Ritorna il numero di voci. */
  suspend fun loadFromCache(): Int =
    withContext(Dispatchers.IO) {
      if (!cacheFile.exists()) return@withContext 0
      val acc =
        runCatching {
            cacheFile.readLines().mapNotNull { line ->
              val sep = line.indexOf('|')
              if (sep <= 0) return@mapNotNull null
              val isDir = line[0] == 'D'
              val path = line.substring(sep + 1)
              val name = path.substringAfterLast('/')
              IndexEntry(path, name, name.lowercase(), isDir)
            }
          }
          .getOrDefault(emptyList())
      entries = acc
      lastBuildMillis = cacheFile.lastModified()
      acc.size
    }

  private fun persist(acc: List<IndexEntry>) {
    cacheFile.bufferedWriter().use { w ->
      acc.forEach { e ->
        w.append(if (e.isDir) 'D' else 'F')
        w.append('|')
        w.append(e.path)
        w.append('\n')
      }
    }
  }

  fun clearCache() {
    runCatching { cacheFile.delete() }
    entries = emptyList()
    lastBuildMillis = 0L
  }

  /**
   * Cerca [query] nei nomi. Ranking: nome esatto > prefisso > sottostringa nel nome >
   * sottostringa nel path. Filtra i file ormai inesistenti. Limita a [limit] risultati.
   */
  fun search(query: String, limit: Int = 200): List<SearchHit> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    val hits = ArrayList<SearchHit>()
    for (e in entries) {
      val score =
        when {
          e.lower == q -> 0
          e.lower.startsWith(q) -> 1
          e.lower.contains(q) -> 2
          e.path.lowercase().contains(q) -> 3
          else -> continue
        }
      hits += SearchHit(File(e.path), e.isDir, score)
    }
    return hits
      .asSequence()
      .sortedWith(compareBy({ it.score }, { it.file.path.length }, { it.file.name.lowercase() }))
      .filter { it.file.exists() } // anti-stantio: scarta ciò che non c'è più
      .take(limit)
      .toList()
  }
}
