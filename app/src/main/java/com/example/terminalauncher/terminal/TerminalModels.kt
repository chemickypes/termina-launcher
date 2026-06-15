package com.example.terminalauncher.terminal

import androidx.annotation.StringRes
import com.example.terminalauncher.R

enum class LineKind {
  INPUT, // comando digitato dall'utente, mostrato col prompt
  OUTPUT,
  ACCENT, // titoli / evidenziazioni
  ERROR,
}

data class TerminalLine(val text: String, val kind: LineKind = LineKind.OUTPUT)

enum class TermScreen {
  TERMINAL,
  APPS,
  CONTACTS,
  SETTINGS,
  EDITOR,
  SEARCH,
  MANUAL,
}

data class CommandSpec(
  val name: String,
  @StringRes val usageRes: Int,
  @StringRes val descRes: Int,
)

val COMMANDS =
  listOf(
    CommandSpec("/help", R.string.u_help, R.string.d_help),
    CommandSpec("/handbook", R.string.u_handbook, R.string.d_handbook),
    CommandSpec("/launch", R.string.u_launch, R.string.d_launch),
    CommandSpec("/apps", R.string.u_apps, R.string.d_apps),
    CommandSpec("/contacts", R.string.u_contacts, R.string.d_contacts),
    CommandSpec("/settings", R.string.u_settings, R.string.d_settings),
    CommandSpec("/battery", R.string.u_battery, R.string.d_battery),
    CommandSpec("/info", R.string.u_info, R.string.d_info),
    CommandSpec("/ip", R.string.u_ip, R.string.d_ip),
    CommandSpec("/ram", R.string.u_ram, R.string.d_ram),
    CommandSpec("/storage", R.string.u_storage, R.string.d_storage),
    CommandSpec("/date", R.string.u_date, R.string.d_date),
    CommandSpec("/uptime", R.string.u_uptime, R.string.d_uptime),
    CommandSpec("/lang", R.string.u_lang, R.string.d_lang),
    CommandSpec("/search", R.string.u_search, R.string.d_search),
    CommandSpec("/call", R.string.u_call, R.string.d_call),
    CommandSpec("/torch", R.string.u_torch, R.string.d_torch),
    CommandSpec("/uninstall", R.string.u_uninstall, R.string.d_uninstall),
    CommandSpec("/theme", R.string.u_theme, R.string.d_theme),
    CommandSpec("/time", R.string.u_time, R.string.d_time),
    CommandSpec("/reload", R.string.u_reload, R.string.d_reload),
    CommandSpec("/config", R.string.u_config, R.string.d_config),
    CommandSpec("/clear", R.string.u_clear, R.string.d_clear),
  )

/** Comandi unix-like del file system (senza prefisso /). */
val UNIX_COMMANDS =
  setOf(
    "ls", "ll", "la", "cd", "pwd", "cat", "head", "tail", "mkdir", "touch", "rm", "cp", "mv",
    "nano", "sudo", "find", "source", "du", "tree",
  )

/** Sintassi (non tradotta) + descrizione (tradotta) per la sezione file system dell'help. */
val UNIX_HELP: List<Pair<String, Int>> =
  listOf(
    "ls | ll | la" to R.string.uh_ls,
    "cd <dir> | pwd" to R.string.uh_cd,
    "cat/head/tail <f>" to R.string.uh_cat,
    "mkdir touch rm" to R.string.uh_mk,
    "cp mv" to R.string.uh_cpmv,
    "nano <file>" to R.string.uh_nano,
    "find <nome>" to R.string.uh_find,
    "du [-n] [path]" to R.string.uh_du,
    "tree [-L n]" to R.string.uh_tree,
    "sudo" to R.string.uh_sudo,
    "source" to R.string.uh_source,
    "alias n=cmd" to R.string.uh_alias,
  )
