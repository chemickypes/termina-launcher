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

package com.example.terminalauncher.system

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.annotation.StringRes
import com.example.terminalauncher.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * File system "shell-like": directory corrente, path con ~, operazioni base.
 * Senza accesso completo (sudo) la home è la cartella privata dell'app;
 * con MANAGE_EXTERNAL_STORAGE diventa /storage/emulated/0.
 */
class FileSystemManager(private val context: Context) {

  private val sharedRoot: File = Environment.getExternalStorageDirectory()
  private val appRoot: File = context.getExternalFilesDir(null) ?: context.filesDir

  val hasFullAccess: Boolean
    get() =
      Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

  val home: File
    get() = if (hasFullAccess) sharedRoot else appRoot

  var cwd: File = home
    private set

  /**
   * File di configurazione stile .bashrc/.zshrc. Posizione fissa nella cartella
   * privata dell'app (= home quando non si usa sudo), così è sempre scrivibile
   * e raggiungibile con `nano ~/.terminarc` nell'uso normale.
   */
  val rcFile: File
    get() = File(appRoot, ".terminarc")

  /** Crea il .terminarc con un template commentato se non esiste ancora. */
  fun ensureRcFile(defaultContent: String) {
    if (!rcFile.exists()) {
      runCatching {
        rcFile.parentFile?.mkdirs()
        rcFile.writeText(defaultContent)
      }
    }
  }

  private fun str(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

  private fun File.canonicalSafe(): String =
    runCatching { canonicalPath }.getOrElse { absolutePath }

  fun prettyPath(file: File = cwd): String {
    val p = file.canonicalSafe()
    val h = home.canonicalSafe()
    return when {
      p == h -> "~"
      p.startsWith("$h/") -> "~" + p.removePrefix(h)
      else -> p
    }
  }

  /** Versione compatta per il prompt: i path lunghi diventano "…/ultimaDir". */
  fun promptLabel(): String {
    val pretty = prettyPath()
    return if (pretty.length <= 22) pretty else "…/" + cwd.name
  }

  fun resolve(arg: String): File {
    val expanded =
      when {
        arg == "~" -> home.path
        arg.startsWith("~/") -> home.path + arg.substring(1)
        else -> arg
      }
    val f = if (expanded.startsWith("/")) File(expanded) else File(cwd, expanded)
    return File(f.canonicalSafe())
  }

  /** Torna null se ok, altrimenti il messaggio di errore. */
  fun cd(arg: String): String? {
    val target = if (arg.isEmpty()) home else resolve(arg)
    return when {
      !target.exists() -> str(R.string.fs_no_such, "cd", arg)
      !target.isDirectory -> str(R.string.fs_not_dir, "cd", arg)
      !target.canRead() -> str(R.string.fs_denied, "cd", arg)
      else -> {
        cwd = target
        null
      }
    }
  }

  fun list(arg: String, all: Boolean, long: Boolean): List<String> {
    val dir = if (arg.isEmpty()) cwd else resolve(arg)
    if (!dir.exists()) return listOf(str(R.string.fs_no_such, "ls", arg))
    if (dir.isFile) return listOf(if (long) lsLong(dir) else dir.name)
    val files = dir.listFiles() ?: return listOf(str(R.string.fs_denied, "ls", arg.ifEmpty { "." }))
    val visible =
      files
        .filter { all || !it.name.startsWith(".") }
        .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    if (visible.isEmpty()) return listOf(str(R.string.fs_empty))
    return if (long) visible.map { lsLong(it) }
    else visible.map { it.name + if (it.isDirectory) "/" else "" }
  }

  private fun lsLong(f: File): String {
    val type = if (f.isDirectory) "d" else "-"
    val perms =
      (if (f.canRead()) "r" else "-") +
        (if (f.canWrite()) "w" else "-") +
        (if (f.canExecute()) "x" else "-")
    val size = if (f.isDirectory) "-".padStart(9) else humanSize(f.length()).padStart(9)
    val date = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(f.lastModified()))
    val name = f.name + if (f.isDirectory) "/" else ""
    return "$type$perms $size  $date  $name"
  }

  fun cat(arg: String, maxLines: Int = 300): List<String> {
    val f = resolve(arg)
    if (!f.exists()) return listOf(str(R.string.fs_no_such, "cat", arg))
    if (f.isDirectory) return listOf(str(R.string.fs_is_dir, "cat", arg))
    if (!f.canRead()) return listOf(str(R.string.fs_denied, "cat", arg))
    if (f.length() > 1_048_576) return listOf(str(R.string.fs_too_big, arg, humanSize(f.length())))
    return runCatching {
        val lines = f.readLines()
        if (lines.size > maxLines)
          lines.take(maxLines) + str(R.string.fs_lines_omitted, lines.size - maxLines)
        else lines.ifEmpty { listOf(str(R.string.fs_file_empty)) }
      }
      .getOrElse { listOf(str(R.string.fs_read_error, it.message.orEmpty())) }
  }

  fun headTail(arg: String, n: Int, fromEnd: Boolean): List<String> {
    val cmd = if (fromEnd) "tail" else "head"
    val f = resolve(arg)
    if (!f.exists()) return listOf(str(R.string.fs_no_such, cmd, arg))
    if (f.isDirectory) return listOf(str(R.string.fs_is_dir, cmd, arg))
    return runCatching {
        val lines = f.readLines()
        if (fromEnd) lines.takeLast(n) else lines.take(n)
      }
      .getOrElse { listOf(str(R.string.fs_read_error, it.message.orEmpty())) }
  }

  fun mkdir(arg: String): String {
    val f = resolve(arg)
    return when {
      f.exists() -> str(R.string.fs_mkdir_exists, arg)
      f.mkdirs() -> str(R.string.fs_mkdir_created, prettyPath(f))
      else -> str(R.string.fs_mkdir_failed, arg)
    }
  }

  fun touch(arg: String): String {
    val f = resolve(arg)
    return when {
      f.exists() -> {
        f.setLastModified(System.currentTimeMillis())
        str(R.string.fs_touch_updated, prettyPath(f))
      }
      runCatching {
          f.parentFile?.mkdirs()
          f.createNewFile()
        }
        .getOrDefault(false) -> str(R.string.fs_touch_created, prettyPath(f))
      else -> str(R.string.fs_touch_failed, arg)
    }
  }

  fun rm(arg: String, recursive: Boolean): String {
    val f = resolve(arg)
    return when {
      !f.exists() -> str(R.string.fs_no_such, "rm", arg)
      f.canonicalSafe() == home.canonicalSafe() -> str(R.string.fs_rm_home)
      cwd.canonicalSafe().startsWith(f.canonicalSafe()) -> str(R.string.fs_rm_cwd)
      f.isDirectory && !recursive -> str(R.string.fs_rm_isdir, arg)
      f.isDirectory ->
        if (f.deleteRecursively()) str(R.string.fs_rm_done, arg) else str(R.string.fs_rm_failed, arg)
      else -> if (f.delete()) str(R.string.fs_rm_done, arg) else str(R.string.fs_rm_failed, arg)
    }
  }

  fun cp(src: String, dst: String, recursive: Boolean): String {
    val s = resolve(src)
    if (!s.exists()) return str(R.string.fs_no_such, "cp", src)
    if (s.isDirectory && !recursive) return str(R.string.fs_cp_isdir, src)
    val dRaw = resolve(dst)
    val d = if (dRaw.isDirectory) File(dRaw, s.name) else dRaw
    return runCatching {
        if (s.isDirectory) s.copyRecursively(d, overwrite = false)
        else s.copyTo(d, overwrite = false)
        str(R.string.fs_cp_done, src, prettyPath(d))
      }
      .getOrElse { str(R.string.fs_cp_error, it.message.orEmpty()) }
  }

  fun mv(src: String, dst: String): String {
    val s = resolve(src)
    if (!s.exists()) return str(R.string.fs_no_such, "mv", src)
    val dRaw = resolve(dst)
    val d = if (dRaw.isDirectory) File(dRaw, s.name) else dRaw
    if (d.exists()) return str(R.string.fs_mv_exists, prettyPath(d))
    if (s.renameTo(d)) return str(R.string.fs_mv_done, src, prettyPath(d))
    return runCatching {
        if (s.isDirectory) {
          s.copyRecursively(d)
          s.deleteRecursively()
        } else {
          s.copyTo(d)
          s.delete()
        }
        str(R.string.fs_mv_done, src, prettyPath(d))
      }
      .getOrElse { str(R.string.fs_mv_error, it.message.orEmpty()) }
  }

  /**
   * `du`: dimensione (ricorsiva) dei contenuti della cartella, ordinati dal più grande.
   * Pensato per girare off-main-thread; `dirSize` è limitato in tempo/numero nodi.
   */
  fun du(arg: String, byName: Boolean = false): List<String> {
    val dir = if (arg.isEmpty()) cwd else resolve(arg)
    if (!dir.exists()) return listOf(str(R.string.fs_no_such, "du", arg))
    if (dir.isFile) return listOf(humanSize(dir.length()).padStart(10) + "  " + dir.name)
    val children = dir.listFiles() ?: return listOf(str(R.string.fs_denied, "du", arg.ifEmpty { "." }))
    val sized =
      children.map { it to dirSize(it) }.let { list ->
        if (byName) list.sortedBy { it.first.name.lowercase() } else list.sortedByDescending { it.second }
      }
    if (sized.isEmpty()) return listOf(str(R.string.fs_empty))
    val rows =
      sized.map { (f, size) ->
        humanSize(size).padStart(10) + "  " + f.name + if (f.isDirectory) "/" else ""
      }
    val total = sized.sumOf { it.second }
    return rows + "──────────" + str(R.string.du_total, humanSize(total).trim())
  }

  private fun dirSize(file: File): Long {
    if (file.isFile) return file.length()
    var total = 0L
    var count = 0
    val start = System.currentTimeMillis()
    val stack = ArrayDeque<File>()
    stack.addLast(file)
    while (stack.isNotEmpty()) {
      if (count > 200_000 || System.currentTimeMillis() - start > 4000) break
      val ch = runCatching { stack.removeLast().listFiles() }.getOrNull() ?: continue
      for (c in ch) {
        count++
        if (c.isDirectory) {
          val isLink = runCatching { c.canonicalPath != c.absolutePath }.getOrDefault(false)
          if (!isLink) stack.addLast(c)
        } else total += c.length()
      }
    }
    return total
  }

  /** `tree`: albero della cartella con indentazione ASCII, limitato in profondità e voci. */
  fun tree(arg: String, maxDepth: Int = 2, maxEntries: Int = 500): List<String> {
    val root = if (arg.isEmpty()) cwd else resolve(arg)
    if (!root.exists()) return listOf(str(R.string.fs_no_such, "tree", arg))
    if (root.isFile) return listOf(root.name)
    val out = ArrayList<String>()
    out += prettyPath(root)
    var count = 0
    fun walk(dir: File, prefix: String, depth: Int) {
      if (depth > maxDepth || count >= maxEntries) return
      val children =
        (runCatching { dir.listFiles() }.getOrNull() ?: return)
          .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
      children.forEachIndexed { i, c ->
        if (count >= maxEntries) return
        count++
        val last = i == children.lastIndex
        out += prefix + (if (last) "└─ " else "├─ ") + c.name + if (c.isDirectory) "/" else ""
        if (c.isDirectory) {
          val isLink = runCatching { c.canonicalPath != c.absolutePath }.getOrDefault(false)
          if (!isLink) walk(c, prefix + if (last) "   " else "│  ", depth + 1)
        }
      }
    }
    walk(root, "", 1)
    if (count >= maxEntries) out += str(R.string.tree_truncated)
    return out
  }

  private fun humanSize(bytes: Long): String =
    when {
      bytes >= 1_073_741_824 -> "%.1f GB".format(Locale.US, bytes / 1_073_741_824.0)
      bytes >= 1_048_576 -> "%.1f MB".format(Locale.US, bytes / 1_048_576.0)
      bytes >= 1024 -> "%.1f KB".format(Locale.US, bytes / 1024.0)
      else -> "$bytes B"
    }
}
