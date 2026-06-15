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

package com.example.terminalauncher.terminal

import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.terminalauncher.R
import com.example.terminalauncher.system.AppEntry
import com.example.terminalauncher.system.AppRepository
import com.example.terminalauncher.system.ContactEntry
import com.example.terminalauncher.system.ContactsRepository
import com.example.terminalauncher.system.FileIndex
import com.example.terminalauncher.system.FileSystemManager
import com.example.terminalauncher.system.SearchHit
import com.example.terminalauncher.system.SystemInfoProvider
import com.example.terminalauncher.theme.TermPalette
import com.example.terminalauncher.theme.TermPalettes
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TerminalViewModel(app: Application) : AndroidViewModel(app) {

  private val context: Context
    get() = getApplication()

  private val prefs = app.getSharedPreferences("termina_prefs", Context.MODE_PRIVATE)

  val appRepository = AppRepository(app)
  val contactsRepository = ContactsRepository(app)
  val fs = FileSystemManager(app)
  val fileIndex = FileIndex(app)
  private val sysInfo = SystemInfoProvider(app)

  val lines = mutableStateListOf<TerminalLine>()
  var screen by mutableStateOf(TermScreen.TERMINAL)
  var cwdLabel by mutableStateOf(fs.promptLabel())
    private set

  private var pendingSudo = false

  var editorFile by mutableStateOf<File?>(null)
    private set

  // ─── caricamento asincrono app / contatti (off main thread, con spinner) ───
  var appsList by mutableStateOf<List<AppEntry>>(emptyList())
    private set
  var appsLoading by mutableStateOf(false)
    private set

  var contactsList by mutableStateOf<List<ContactEntry>>(emptyList())
    private set
  var contactsLoading by mutableStateOf(false)
    private set
  var contactsLoaded by mutableStateOf(false)
    private set

  /** Righe del manuale (assets/handbook.md), caricate la prima volta che si apre. */
  var manualLines by mutableStateOf<List<String>>(emptyList())
    private set

  // ─── stato schermata di ricerca file ───
  var searchQuery by mutableStateOf("")
  var searchResults by mutableStateOf<List<SearchHit>>(emptyList())
    private set

  var searchIndexing by mutableStateOf(false)
    private set

  var searchStatus by mutableStateOf("")
    private set

  private var indexJob: Job? = null

  var palette by mutableStateOf(TermPalettes.byName(prefs.getString("theme", "green")))
    private set

  var fontScale by mutableStateOf(prefs.getFloat("font_scale", 1f))
    private set

  val history = mutableListOf<String>()

  /** Alias definiti in .terminarc o a runtime (nome -> comando), come in bash/zsh. */
  private val aliases = linkedMapOf<String, String>()

  init {
    banner()
    loadRc()
    prewarmIndex()
  }

  // ─── .terminarc / alias ────────────────────────────────────────

  private fun loadRc() {
    fs.ensureRcFile(str(R.string.rc_template))
    val count = parseRc()
    if (count > 0) out(str(R.string.alias_loaded, count))
  }

  /** Ricarica .terminarc su richiesta (comando `source` / `/reload`). */
  fun reloadRc() {
    out(str(R.string.alias_loaded, parseRc()))
  }

  /** Rilegge il .terminarc da zero. Ritorna il numero di alias caricati. */
  private fun parseRc(): Int {
    aliases.clear()
    runCatching {
      if (!fs.rcFile.exists()) return 0
      fs.rcFile.readLines().forEach { raw ->
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEach
        if (!line.startsWith("alias ")) return@forEach
        parseAliasAssignment(line.removePrefix("alias ").trim())
      }
    }
    return aliases.size
  }

  /** Interpreta "nome=valore" (con eventuali apici) e lo aggiunge alla mappa. */
  private fun parseAliasAssignment(spec: String): Pair<String, String>? {
    val eq = spec.indexOf('=')
    if (eq <= 0) return null
    val name = spec.substring(0, eq).trim()
    var value = spec.substring(eq + 1).trim()
    if (value.length >= 2 &&
      ((value.first() == '"' && value.last() == '"') ||
        (value.first() == '\'' && value.last() == '\''))) {
      value = value.substring(1, value.length - 1)
    }
    if (name.isEmpty() || value.isEmpty()) return null
    aliases[name] = value
    return name to value
  }

  /**
   * Espande il primo token se è un alias, ricorsivamente (alias che richiamano alias),
   * con guardia anti-loop: un nome già espanso nella catena ferma l'espansione.
   */
  private fun expandAlias(input: String): String {
    if (aliases.isEmpty()) return input
    var current = input
    val seen = mutableSetOf<String>()
    repeat(20) {
      val sp = current.indexOf(' ')
      val head = if (sp < 0) current else current.substring(0, sp)
      if (head in seen) return current
      val repl = aliases[head] ?: return current
      seen += head
      current = if (sp < 0) repl else repl + current.substring(sp)
    }
    return current
  }

  private fun aliasCommand(cmd: String, rest: String) {
    if (cmd == "unalias") {
      if (rest.isEmpty()) {
        err(R.string.unalias_usage)
        return
      }
      val name = rest.substringBefore(' ').trim()
      if (aliases.remove(name) != null) out(str(R.string.alias_removed, name))
      else err(R.string.alias_unknown, name)
      return
    }
    // alias
    if (rest.isEmpty()) {
      if (aliases.isEmpty()) out(str(R.string.alias_none))
      else aliases.forEach { (k, v) -> out("  $k=$v") }
      return
    }
    val pair = if (rest.contains('=')) parseAliasAssignment(rest) else null
    if (pair == null) err(R.string.alias_usage) else out(str(R.string.alias_set, pair.first, pair.second))
  }

  private fun isRcFile(f: File?): Boolean {
    f ?: return false
    return runCatching { f.canonicalPath == fs.rcFile.canonicalPath }.getOrDefault(false)
  }

  // ─── ricerca file (comando find) ───────────────────────────────

  private var searchJob: Job? = null
  private var indexBuilding = false

  /** target del menu azioni mostrato al tap lungo su un risultato. */
  var searchActionTarget by mutableStateOf<SearchHit?>(null)
    private set

  private enum class BuildUi { FULL, REFRESH, SILENT }

  /**
   * Pre-riscalda l'indice all'avvio: carica la cache (così la prima `find` è istantanea)
   * e, se è obsoleta, la ricostruisce in background. Con cooldown: il reindex automatico
   * non parte più di una volta ogni [PREWARM_COOLDOWN_MS] (un launcher si apre di continuo);
   * un `find` esplicito aggiorna comunque sempre (vedi BuildUi.REFRESH, non soggetto a cooldown).
   */
  private fun prewarmIndex() {
    viewModelScope.launch {
      fileIndex.loadFromCache()
      if (fileIndex.cacheAgeMillis > PREWARM_COOLDOWN_MS && fileIndex.isStale(fs.home)) {
        launchBuild(BuildUi.SILENT)
      }
    }
  }

  private fun find(query: String, reindex: Boolean) {
    if (reindex) {
      searchQuery = query.trim()
      screen = TermScreen.SEARCH
      launchBuild(BuildUi.FULL, force = true)
      return
    }
    if (query.isBlank()) {
      err(R.string.search_usage)
      return
    }
    searchQuery = query.trim()
    searchResults = emptyList()
    screen = TermScreen.SEARCH
    viewModelScope.launch {
      // riusa l'indice in memoria o la cache su disco; altrimenti lo costruisce
      if (!fileIndex.isBuilt && fileIndex.loadFromCache() == 0) {
        launchBuild(BuildUi.FULL)
      } else {
        onSearchQueryChange(searchQuery) // risultati immediati dalla cache
        if (fileIndex.isStale(fs.home)) launchBuild(BuildUi.REFRESH) // aggiorna se obsoleto
      }
    }
  }

  /**
   * Unico punto in cui si costruisce l'indice. Guardato da [indexBuilding] per evitare
   * build concorrenti (prewarm + find). [ui] decide quanto feedback mostrare:
   *  FULL = svuota e mostra "creazione"; REFRESH = tiene i risultati, indicatore acceso;
   *  SILENT = nessun aggiornamento UI (prewarm all'avvio).
   */
  private fun launchBuild(ui: BuildUi, force: Boolean = false) {
    if (indexBuilding && !force) return
    indexJob?.cancel()
    indexBuilding = true
    if (ui == BuildUi.FULL) {
      searchResults = emptyList()
      searchStatus = str(R.string.search_building)
    }
    if (ui != BuildUi.SILENT) searchIndexing = true
    indexJob =
      viewModelScope.launch {
        fileIndex.build(fs.home) { n ->
          if (ui != BuildUi.SILENT) searchStatus = str(R.string.search_indexing, n)
        }
        indexBuilding = false
        searchIndexing = false
        if (screen == TermScreen.SEARCH) onSearchQueryChange(searchQuery)
      }
  }

  /** Aggiorna il filtro della schermata di ricerca (live-filter dal campo TUI). */
  fun onSearchQueryChange(q: String) {
    searchQuery = q
    if (!fileIndex.isBuilt) return // ancora in indicizzazione
    searchJob?.cancel()
    searchJob =
      viewModelScope.launch {
        val hits = withContext(Dispatchers.Default) { fileIndex.search(q) }
        searchResults = hits
        searchStatus =
          if (q.isBlank()) str(R.string.search_footer_idx, fileIndex.entries.size)
          else str(R.string.search_footer, hits.size, fileIndex.entries.size)
      }
  }

  /**
   * Tap su un risultato:
   *  - file → prova ad aprirlo con l'app di sistema (resta nella ricerca);
   *  - cartella, o apertura fallita → naviga nella cartella e torna al terminale.
   */
  fun openSearchHit(hit: SearchHit) {
    if (!hit.isDir && openFileExternally(hit.file)) return
    navigateToHit(hit)
  }

  private fun navigateToHit(hit: SearchHit) {
    val target = if (hit.isDir) hit.file else hit.file.parentFile ?: fs.home
    fs.cd(target.absolutePath)
    cwdLabel = fs.promptLabel()
    screen = TermScreen.TERMINAL
    out(str(R.string.search_cd, fs.prettyPath(target)), LineKind.INPUT)
    if (!hit.isDir) out(fs.prettyPath(hit.file))
  }

  // ─── menu azioni (tap lungo su un risultato) ───
  fun onResultLongPress(hit: SearchHit) {
    searchActionTarget = hit
  }

  fun dismissAction() {
    searchActionTarget = null
  }

  fun actionOpen(hit: SearchHit) {
    searchActionTarget = null
    openSearchHit(hit)
  }

  fun actionCd(hit: SearchHit) {
    searchActionTarget = null
    navigateToHit(hit)
  }

  fun actionShare(hit: SearchHit) {
    searchActionTarget = null
    shareFile(hit.file)
  }

  private fun openFileExternally(file: File): Boolean =
    runCatching {
        val uri =
          FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime =
          MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
            ?: "*/*"
        val intent =
          Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
      }
      .getOrDefault(false)

  private fun shareFile(file: File) {
    val ok =
      runCatching {
          val uri =
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
          val mime =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
              ?: "*/*"
          val send =
            Intent(Intent.ACTION_SEND)
              .setType(mime)
              .putExtra(Intent.EXTRA_STREAM, uri)
              .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          val chooser =
            Intent.createChooser(send, str(R.string.share_via))
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          context.startActivity(chooser)
          true
        }
        .getOrDefault(false)
    if (!ok) err(R.string.share_failed, file.name)
  }

  fun closeSearch() {
    searchActionTarget = null
    searchJob?.cancel()
    screen = TermScreen.TERMINAL
  }

  /** Apre il manuale leggendolo da assets/handbook.md (caricato una sola volta). */
  private fun openManual() {
    if (manualLines.isEmpty()) {
      manualLines =
        runCatching {
            context.assets.open("handbook.md").bufferedReader().use { it.readLines() }
          }
          .getOrElse { listOf(str(R.string.manual_missing)) }
    }
    screen = TermScreen.MANUAL
  }

  fun closeManual() {
    screen = TermScreen.TERMINAL
  }

  // ─── caricamento app / contatti off-main (la query è lenta su device reali) ───

  /**
   * Carica la lista app fuori dal main thread. Riusa la cache in memoria;
   * con [force] la rilegge da PackageManager (loadLabel per app è costoso).
   */
  fun loadApps(force: Boolean = false) {
    if (appsLoading) return
    if (appsList.isNotEmpty() && !force) return
    appsLoading = true
    viewModelScope.launch {
      val result = withContext(Dispatchers.IO) { appRepository.apps(forceRefresh = force) }
      appsList = result
      appsLoading = false
    }
  }

  /** Carica la rubrica fuori dal main thread (la query ContactsContract è lenta). */
  fun loadContacts(force: Boolean = false) {
    if (contactsLoading) return
    if (contactsLoaded && !force) return
    if (!contactsRepository.hasPermission()) {
      contactsList = emptyList()
      contactsLoaded = true
      return
    }
    contactsLoading = true
    viewModelScope.launch {
      val result = withContext(Dispatchers.IO) { contactsRepository.contacts() }
      contactsList = result
      contactsLoading = false
      contactsLoaded = true
    }
  }

  /**
   * Apre il .terminarc reale nell'editor, SEMPRE in [FileSystemManager.rcFile]
   * (= appRoot) a prescindere da sudo. Necessario perché dopo `sudo` `~` punta a
   * /storage/emulated/0, quindi `nano ~/.terminarc` aprirebbe un file sbagliato.
   */
  fun openConfig() {
    fs.ensureRcFile(str(R.string.rc_template))
    editorFile = fs.rcFile
    screen = TermScreen.EDITOR
  }

  private fun str(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

  private fun banner() {
    out("╔════════════════════════════╗", LineKind.ACCENT)
    out("║   TERMINA LAUNCHER v1.0    ║", LineKind.ACCENT)
    out("╚════════════════════════════╝", LineKind.ACCENT)
    out(str(R.string.banner_hint))
    out("")
  }

  private fun out(text: String, kind: LineKind = LineKind.OUTPUT) {
    lines += TerminalLine(text, kind)
    // limita lo scrollback
    while (lines.size > 500) lines.removeAt(0)
  }

  private fun err(text: String) = out(text, LineKind.ERROR)

  private fun err(@StringRes id: Int, vararg args: Any) = err(str(id, *args))

  fun applyTheme(p: TermPalette) {
    palette = p
    prefs.edit().putString("theme", p.name).apply()
  }

  fun applyFontScale(scale: Float) {
    fontScale = scale
    prefs.edit().putFloat("font_scale", scale).apply()
  }

  fun submit(raw: String) {
    val input = raw.trim()
    if (input.isEmpty()) return
    history += input
    out("$ $input", LineKind.INPUT)

    // alias/unalias gestiti prima dell'espansione: la parola "alias" non va espansa
    val head0 = input.substringBefore(' ').lowercase()
    if (head0 == "alias" || head0 == "unalias") {
      aliasCommand(head0, input.substringAfter(' ', "").trim())
      return
    }

    val exec = expandAlias(input)

    if (!exec.startsWith("/")) {
      val tokens = tokenize(exec)
      val first = tokens.firstOrNull()?.lowercase()
      if (first in UNIX_COMMANDS) {
        fileCommand(first!!, tokens.drop(1))
      } else {
        // testo libero = scorciatoia per avviare un'app
        launchApp(exec)
      }
      return
    }

    val parts = exec.split(Regex("\\s+"), limit = 2)
    val cmd = parts[0].lowercase()
    val arg = parts.getOrNull(1)?.trim().orEmpty()

    when (cmd) {
      "/help" -> help()
      "/handbook", "/man" -> openManual()
      "/launch" -> launch(arg)
      "/apps" -> screen = TermScreen.APPS
      "/contacts", "/contatti" -> screen = TermScreen.CONTACTS
      "/settings" -> screen = TermScreen.SETTINGS
      "/battery" -> printAll(sysInfo.battery())
      "/info" -> printAll(sysInfo.deviceInfo())
      "/ip" -> printAll(sysInfo.ipAddresses())
      "/ram" -> printAll(sysInfo.ram())
      "/storage" -> printAll(sysInfo.storage())
      "/date" -> printAll(sysInfo.dateTime())
      "/time" -> printAll(sysInfo.time())
      "/reload" -> reloadRc()
      "/config" -> openConfig()
      "/uptime" -> printAll(sysInfo.uptime())
      "/lang" -> lang()
      "/search" -> search(arg)
      "/call" -> call(arg)
      "/torch" -> torch(arg)
      "/uninstall" -> uninstall(arg)
      "/theme" -> theme(arg)
      "/clear" -> {
        lines.clear()
        banner()
      }
      "/wifi" -> openSystem(Settings.ACTION_WIFI_SETTINGS, str(R.string.label_wifi_settings))
      "/bt", "/bluetooth" ->
        openSystem(Settings.ACTION_BLUETOOTH_SETTINGS, str(R.string.label_bt_settings))
      else -> err(R.string.cmd_not_found, cmd)
    }
  }

  private fun printAll(rows: List<String>) {
    rows.forEach { out(it) }
    out("")
  }

  /** Esegue un comando file potenzialmente lento (du/tree) off-main e stampa il risultato. */
  private fun runFsAsync(produce: () -> List<String>) {
    viewModelScope.launch {
      val rows = withContext(Dispatchers.IO) { produce() }
      printAll(rows)
    }
  }

  private fun help() {
    out(str(R.string.help_title_commands), LineKind.ACCENT)
    COMMANDS.forEach { out("  ${str(it.usageRes).padEnd(18)} ${str(it.descRes)}") }
    out("")
    out(str(R.string.help_title_fs), LineKind.ACCENT)
    UNIX_HELP.forEach { (syntax, descRes) -> out("  ${syntax.padEnd(18)} ${str(descRes)}") }
    out("")
    out(str(R.string.help_free_text))
    out(str(R.string.rc_help, fs.prettyPath(fs.rcFile)))
    out("")
  }

  // ─── file system ───────────────────────────────────────────────

  private fun tokenize(input: String): List<String> {
    val tokens = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuote = false
    for (c in input) {
      when {
        c == '"' -> inQuote = !inQuote
        c == ' ' && !inQuote -> {
          if (sb.isNotEmpty()) {
            tokens += sb.toString()
            sb.clear()
          }
        }
        else -> sb.append(c)
      }
    }
    if (sb.isNotEmpty()) tokens += sb.toString()
    return tokens
  }

  /** Estrae il valore intero di un flag, accettando sia "-L 3" (separato) sia "-L3" (unito). */
  private fun optIntFlag(args: List<String>, flag: String): Int? {
    val i = args.indexOf(flag)
    if (i >= 0 && i + 1 < args.size) return args[i + 1].toIntOrNull()
    return args.firstOrNull { it.startsWith(flag) && it != flag }?.removePrefix(flag)?.toIntOrNull()
  }

  private fun fileCommand(cmd: String, args: List<String>) {
    val flags = args.filter { it.startsWith("-") }.toSet()
    val params = args.filterNot { it.startsWith("-") }
    val p0 = params.getOrNull(0).orEmpty()
    val p1 = params.getOrNull(1).orEmpty()

    when (cmd) {
      "pwd" -> out(fs.cwd.path)
      "cd" -> fs.cd(p0)?.let { err(it) }
      "ls" -> printAll(fs.list(p0, all = "-a" in flags || "-la" in flags, long = "-l" in flags || "-la" in flags))
      "ll" -> printAll(fs.list(p0, all = false, long = true))
      "la" -> printAll(fs.list(p0, all = true, long = true))
      "cat" -> printAll(fs.cat(p0.ifEmpty { return err(R.string.e_usage_cat) }))
      "head" -> printAll(fs.headTail(p0.ifEmpty { return err(R.string.e_usage_head) }, p1.toIntOrNull() ?: 10, fromEnd = false))
      "tail" -> printAll(fs.headTail(p0.ifEmpty { return err(R.string.e_usage_tail) }, p1.toIntOrNull() ?: 10, fromEnd = true))
      "mkdir" -> out(fs.mkdir(p0.ifEmpty { return err(R.string.e_usage_mkdir) }))
      "touch" -> out(fs.touch(p0.ifEmpty { return err(R.string.e_usage_touch) }))
      "rm" -> out(fs.rm(p0.ifEmpty { return err(R.string.e_usage_rm) }, recursive = "-r" in flags || "-rf" in flags))
      "cp" -> {
        if (p0.isEmpty() || p1.isEmpty()) return err(R.string.e_usage_cp)
        out(fs.cp(p0, p1, recursive = "-r" in flags))
      }
      "mv" -> {
        if (p0.isEmpty() || p1.isEmpty()) return err(R.string.e_usage_mv)
        out(fs.mv(p0, p1))
      }
      "nano" -> nano(p0)
      "sudo" -> sudo()
      "source" -> reloadRc()
      "find" -> find(params.joinToString(" "), reindex = "--reindex" in flags || "-r" in flags)
      "du" -> {
        val byName = "-n" in flags
        runFsAsync { fs.du(p0, byName = byName) }
      }
      "tree" -> {
        val depth = optIntFlag(args, "-L")?.coerceIn(1, 8) ?: 2
        // il path è il primo param che non sia il numero consumato da -L
        val path = params.firstOrNull { it != depth.toString() }.orEmpty()
        runFsAsync { fs.tree(path, maxDepth = depth) }
      }
    }
    cwdLabel = fs.promptLabel()
    if (!fs.hasFullAccess && cmd in setOf("ls", "ll", "la") && params.isEmpty()) {
      out(str(R.string.limited_access_hint), LineKind.ACCENT)
    }
  }

  private fun nano(arg: String) {
    if (arg.isEmpty()) {
      err(R.string.e_usage_nano)
      return
    }
    val f = fs.resolve(arg)
    if (f.isDirectory) {
      err(R.string.nano_is_dir, arg)
      return
    }
    editorFile = f
    screen = TermScreen.EDITOR
  }

  fun closeEditor(message: String?) {
    val wasRc = isRcFile(editorFile)
    editorFile = null
    screen = TermScreen.TERMINAL
    message?.let { out(it) }
    if (wasRc) out(str(R.string.alias_loaded, parseRc()))
  }

  private fun sudo() {
    if (fs.hasFullAccess) {
      out(str(R.string.sudo_already, fs.home.path))
      return
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      err(R.string.sudo_unavailable)
      return
    }
    val intent =
      Intent(
          Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
          Uri.parse("package:${context.packageName}"),
        )
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (startSafely(intent)) {
      pendingSudo = true
      out(str(R.string.sudo_grant_hint))
    } else {
      err(R.string.sudo_settings_failed)
    }
  }

  /** Chiamato al resume dell'activity: completa il flusso sudo. */
  fun onResumed() {
    if (pendingSudo && fs.hasFullAccess) {
      pendingSudo = false
      fs.cd("")
      cwdLabel = fs.promptLabel()
      out(str(R.string.sudo_granted, fs.home.path), LineKind.ACCENT)
    }
  }

  // ─── comandi /launch e affini ──────────────────────────────────

  private fun launch(arg: String) {
    if (arg.isEmpty()) {
      err(R.string.e_usage_launch)
      return
    }
    when (arg.lowercase()) {
      "settings", "impostazioni" ->
        openSystem(Settings.ACTION_SETTINGS, str(R.string.label_system_settings))
      "wifi", "wi-fi" -> openSystem(Settings.ACTION_WIFI_SETTINGS, str(R.string.label_wifi_settings))
      "bluetooth", "bt" ->
        openSystem(Settings.ACTION_BLUETOOTH_SETTINGS, str(R.string.label_bt_settings))
      "lingua", "language", "lang" ->
        openSystem(Settings.ACTION_LOCALE_SETTINGS, str(R.string.label_locale_settings))
      "contatti", "contacts" -> openContactsApp()
      else -> launchApp(arg)
    }
  }

  private fun launchApp(query: String) {
    val matches = appRepository.search(query)
    when {
      matches.isEmpty() -> err(R.string.app_not_found, query)
      matches.size == 1 -> {
        val app = matches.first()
        if (appRepository.launch(app)) out(str(R.string.launching_app, app.label))
        else err(R.string.launch_failed, app.label)
      }
      else -> {
        out(str(R.string.multiple_results, query), LineKind.ACCENT)
        matches.take(8).forEach { out("  ${it.label}") }
        out(str(R.string.multiple_results_hint))
      }
    }
  }

  private fun openContactsApp() {
    val intent =
      Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (startSafely(intent)) out(str(R.string.opening_contacts))
    else err(R.string.contacts_app_missing)
  }

  private fun openSystem(action: String, label: String) {
    val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (startSafely(intent)) out(str(R.string.opening, label)) else err(R.string.open_failed, label)
  }

  private fun lang() {
    printAll(sysInfo.language())
    openSystem(Settings.ACTION_LOCALE_SETTINGS, str(R.string.label_locale_settings))
  }

  private fun search(arg: String) {
    if (arg.isEmpty()) {
      err(R.string.e_usage_search)
      return
    }
    val intent =
      Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(arg)))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (startSafely(intent)) out(str(R.string.searching, arg)) else err(R.string.no_browser)
  }

  private fun call(arg: String) {
    if (arg.isEmpty()) {
      err(R.string.e_usage_call)
      return
    }
    val number =
      if (arg.any { it.isLetter() }) {
        if (!contactsRepository.hasPermission()) {
          err(R.string.contacts_permission_missing)
          return
        }
        val match =
          contactsRepository.contacts().firstOrNull { it.name.lowercase().contains(arg.lowercase()) }
        if (match == null) {
          err(R.string.contact_not_found, arg)
          return
        }
        out(str(R.string.contact_found, match.name, match.phone))
        match.phone
      } else arg
    dial(number)
  }

  fun dial(number: String) {
    val intent =
      Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (startSafely(intent)) out(str(R.string.dialing, number)) else err(R.string.no_dialer)
  }

  private var torchOn = false

  private fun torch(arg: String) {
    val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraId =
      cm.cameraIdList.firstOrNull {
        cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
      }
    if (cameraId == null) {
      err(R.string.no_torch)
      return
    }
    val enable =
      when (arg.lowercase()) {
        "on" -> true
        "off" -> false
        "" -> !torchOn
        else -> {
          err(R.string.e_usage_torch)
          return
        }
      }
    runCatching { cm.setTorchMode(cameraId, enable) }
      .onSuccess {
        torchOn = enable
        out(str(if (enable) R.string.torch_on else R.string.torch_off))
      }
      .onFailure { err(R.string.torch_error, it.message.orEmpty()) }
  }

  private fun uninstall(arg: String) {
    if (arg.isEmpty()) {
      err(R.string.e_usage_uninstall)
      return
    }
    val matches = appRepository.search(arg)
    when {
      matches.isEmpty() -> err(R.string.app_not_found, arg)
      matches.size > 1 -> {
        out(str(R.string.multiple_results, arg), LineKind.ACCENT)
        matches.take(8).forEach { out("  ${it.label}") }
      }
      else -> {
        val app = matches.first()
        val intent =
          Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (startSafely(intent)) out(str(R.string.uninstall_requested, app.label))
        else err(R.string.uninstall_failed, app.label)
      }
    }
  }

  private fun theme(arg: String) {
    val p = TermPalettes.all.firstOrNull { it.name == arg.lowercase() }
    if (p == null) {
      err(R.string.themes_list, TermPalettes.all.joinToString(" | ") { it.name })
      return
    }
    applyTheme(p)
    out(str(R.string.theme_set, p.name))
  }

  fun launchFromTui(label: String) {
    val matches = appRepository.search(label)
    matches.firstOrNull()?.let { appRepository.launch(it) }
  }

  private fun startSafely(intent: Intent): Boolean =
    runCatching { context.startActivity(intent) }.isSuccess

  companion object {
    /** Intervallo minimo tra due reindex automatici all'avvio (10 minuti). */
    private const val PREWARM_COOLDOWN_MS = 10 * 60 * 1000L
  }
}
