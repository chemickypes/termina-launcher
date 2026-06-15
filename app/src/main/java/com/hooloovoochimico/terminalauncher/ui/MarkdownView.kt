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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.hooloovoochimico.terminalauncher.theme.LocalTermPalette
import com.hooloovoochimico.terminalauncher.theme.TermPalette

/**
 * Renderer markdown minimale e nativo (nessuna libreria esterna), pensato per
 * leggere documenti come l'handbook in stile terminale. Supporta: titoli,
 * paragrafi, liste puntate/numerate, citazioni, regole orizzontali, blocchi e
 * span di codice, tabelle e formattazione inline (**grassetto**, *corsivo*,
 * `codice`).
 */

// ─── modello a blocchi ───

private sealed interface MdBlock {
  data class Heading(val level: Int, val text: String) : MdBlock
  data class Paragraph(val text: String) : MdBlock
  data class Bullet(val text: String, val indent: Int) : MdBlock
  data class Ordered(val marker: String, val text: String) : MdBlock
  data class Quote(val text: String) : MdBlock
  data class Code(val text: String) : MdBlock
  data class TableRow(val cells: List<String>) : MdBlock
  object Rule : MdBlock
  object Blank : MdBlock
}

private val TABLE_SEP = Regex("^:?-{2,}:?$")

private fun parseMarkdown(lines: List<String>): List<MdBlock> {
  val blocks = ArrayList<MdBlock>()
  var inCode = false
  for (raw in lines) {
    val trimmed = raw.trim()
    if (trimmed.startsWith("```")) {
      inCode = !inCode
      continue
    }
    if (inCode) {
      blocks += MdBlock.Code(raw)
      continue
    }
    when {
      trimmed.isEmpty() -> blocks += MdBlock.Blank
      trimmed.startsWith("#") -> {
        val level = trimmed.takeWhile { it == '#' }.length
        blocks += MdBlock.Heading(level, trimmed.drop(level).trim())
      }
      trimmed.startsWith("---") || trimmed.startsWith("***") || trimmed.startsWith("___") ->
        blocks += MdBlock.Rule
      trimmed.startsWith(">") -> blocks += MdBlock.Quote(trimmed.drop(1).trim())
      trimmed.startsWith("|") -> {
        val cells =
          trimmed.trim('|').split("|").map { it.trim() }
        // salta la riga separatore di intestazione (|---|---|)
        if (cells.all { it.isEmpty() || TABLE_SEP.matches(it) }) continue
        blocks += MdBlock.TableRow(cells)
      }
      trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
        val indent = raw.takeWhile { it == ' ' }.length
        blocks += MdBlock.Bullet(trimmed.drop(2).trim(), indent)
      }
      Regex("^\\d+\\. ").containsMatchIn(trimmed) -> {
        val marker = trimmed.takeWhile { it != ' ' }
        blocks += MdBlock.Ordered(marker, trimmed.drop(marker.length).trim())
      }
      else -> blocks += MdBlock.Paragraph(trimmed)
    }
  }
  return blocks
}

// ─── formattazione inline ───

/** Costruisce un AnnotatedString gestendo `codice`, **grassetto** e *corsivo*. */
private fun mdInline(text: String, palette: TermPalette): AnnotatedString = buildAnnotatedString {
  var i = 0
  val n = text.length
  while (i < n) {
    val c = text[i]
    when {
      c == '`' -> {
        val end = text.indexOf('`', i + 1)
        if (end > i) {
          withStyle(SpanStyle(color = palette.accent)) { append(text.substring(i + 1, end)) }
          i = end + 1
        } else {
          append(c); i++
        }
      }
      c == '*' && i + 1 < n && text[i + 1] == '*' -> {
        val end = text.indexOf("**", i + 2)
        if (end > i) {
          withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = palette.fg)) {
            append(text.substring(i + 2, end))
          }
          i = end + 2
        } else {
          append(c); i++
        }
      }
      c == '*' -> {
        val end = text.indexOf('*', i + 1)
        if (end > i) {
          withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            append(text.substring(i + 1, end))
          }
          i = end + 1
        } else {
          append(c); i++
        }
      }
      else -> {
        append(c); i++
      }
    }
  }
}

// ─── rendering ───

/** Anteprima markdown scorrevole. */
@Composable
fun MarkdownPreview(lines: List<String>, modifier: Modifier = Modifier) {
  val palette = LocalTermPalette.current
  val blocks = remember(lines) { parseMarkdown(lines) }
  LazyColumn(modifier = modifier) {
    items(blocks) { block -> MarkdownBlock(block, palette) }
  }
}

@Composable
private fun MarkdownBlock(block: MdBlock, palette: TermPalette) {
  val typo = MaterialTheme.typography
  when (block) {
    is MdBlock.Heading -> {
      val style =
        when (block.level) {
          1 -> typo.titleLarge
          2 -> typo.titleMedium
          else -> typo.bodyLarge
        }
      Text(
        text = mdInline(block.text, palette),
        color = palette.accent,
        style = style.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
      )
    }
    is MdBlock.Paragraph ->
      Text(mdInline(block.text, palette), color = palette.fg, style = typo.bodyMedium)
    is MdBlock.Bullet -> {
      val pad = (8 * (block.indent / 2)).dp
      Text(
        buildAnnotatedString {
          withStyle(SpanStyle(color = palette.accent)) { append("• ") }
          append(mdInline(block.text, palette))
        },
        color = palette.fg,
        style = typo.bodyMedium,
        modifier = Modifier.padding(start = pad),
      )
    }
    is MdBlock.Ordered ->
      Text(
        buildAnnotatedString {
          withStyle(SpanStyle(color = palette.accent)) { append(block.marker + " ") }
          append(mdInline(block.text, palette))
        },
        color = palette.fg,
        style = typo.bodyMedium,
      )
    is MdBlock.Quote ->
      Text(
        buildAnnotatedString {
          withStyle(SpanStyle(color = palette.accent)) { append("▏ ") }
          withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            append(mdInline(block.text, palette))
          }
        },
        color = palette.dim,
        style = typo.bodyMedium,
      )
    is MdBlock.Code ->
      Text(
        text = "│ " + block.text,
        color = palette.dim,
        style = typo.bodyMedium,
      )
    is MdBlock.TableRow ->
      Text(
        buildAnnotatedString {
          block.cells.forEachIndexed { idx, cell ->
            if (idx > 0) withStyle(SpanStyle(color = palette.dim)) { append(" │ ") }
            append(mdInline(cell, palette))
          }
        },
        color = palette.fg,
        style = typo.bodyMedium,
      )
    MdBlock.Rule ->
      Text("─".repeat(40), color = palette.dim, style = typo.bodyMedium)
    MdBlock.Blank -> Spacer(Modifier.height(6.dp))
  }
}

/** Vista "sorgente": il markdown grezzo, riga per riga, senza formattazione. */
@Composable
fun MarkdownSource(lines: List<String>, modifier: Modifier = Modifier) {
  val palette = LocalTermPalette.current
  LazyColumn(modifier = modifier) {
    items(lines) { raw ->
      Text(
        text = raw.ifEmpty { " " },
        color = palette.dim,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}
