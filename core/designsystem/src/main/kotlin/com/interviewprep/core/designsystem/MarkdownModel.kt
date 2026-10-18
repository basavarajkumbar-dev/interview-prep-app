package com.interviewprep.core.designsystem

/**
 * Markdown-lite produced by the content pipeline:
 *   ### heading · paragraphs with **bold** and `code` · "- " / "• " bullets · "1. " numbered items
 *   ```code / ```output fenced blocks · "| a | b |" table rows.
 * Parsed once per text into blocks so long lessons can be shown in a LazyColumn.
 */
sealed interface MdBlock {
    data class Heading(val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullet(val text: String, val marker: String) : MdBlock
    data class Code(val code: String, val isOutput: Boolean) : MdBlock
    data class Table(val rows: List<List<String>>) : MdBlock
}

/** Inline spans for a paragraph: plain, bold or code. */
sealed interface MdSpan {
    val text: String
    data class Plain(override val text: String) : MdSpan
    data class Bold(override val text: String) : MdSpan
    data class Code(override val text: String) : MdSpan
}

object Markdown {
    private val fenceStart = Regex("^```(code|output|\\w*)\\s*$")
    private val numbered = Regex("^(\\d{1,2})[.)]\\s+(.*)$", RegexOption.DOT_MATCHES_ALL)

    fun parse(src: String): List<MdBlock> {
        val out = mutableListOf<MdBlock>()
        val lines = src.replace("\r\n", "\n").split("\n")
        var i = 0
        val para = StringBuilder()
        fun flushPara() {
            val t = para.toString().trim()
            if (t.isNotEmpty()) out += paragraphOrBullet(t)
            para.clear()
        }
        while (i < lines.size) {
            val line = lines[i]
            val fence = fenceStart.matchEntire(line.trim())
            if (fence != null && line.trimStart().startsWith("```")) {
                flushPara()
                val isOutput = fence.groupValues[1] == "output"
                val code = StringBuilder()
                i++
                while (i < lines.size && lines[i].trim() != "```") {
                    if (code.isNotEmpty()) code.append('\n')
                    code.append(lines[i])
                    i++
                }
                out += MdBlock.Code(code.toString().trimEnd(), isOutput)
                i++ // skip closing fence (or end)
                continue
            }
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> flushPara()
                trimmed.startsWith("### ") -> { flushPara(); out += MdBlock.Heading(trimmed.removePrefix("### ").trim()) }
                trimmed.startsWith("|") -> {
                    flushPara()
                    val rows = mutableListOf<List<String>>()
                    while (i < lines.size && lines[i].trim().startsWith("|")) {
                        val cells = lines[i].trim().trim('|').split(" | ").map { it.trim().trim('|').trim() }
                        if (cells.any { it.isNotEmpty() } && !cells.all { it.matches(Regex("-{2,}")) }) rows += cells
                        i++
                    }
                    if (rows.isNotEmpty()) out += MdBlock.Table(rows)
                    continue
                }
                trimmed.startsWith("- ") || trimmed.startsWith("• ") || numbered.matches(trimmed) -> {
                    flushPara()
                    para.append(trimmed)
                }
                else -> {
                    if (para.isNotEmpty()) para.append(' ')
                    para.append(trimmed)
                }
            }
            i++
        }
        flushPara()
        return out
    }

    private fun paragraphOrBullet(t: String): MdBlock {
        if (t.startsWith("- ") || t.startsWith("• ")) return MdBlock.Bullet(t.drop(2).trim(), "•")
        numbered.matchEntire(t)?.let { return MdBlock.Bullet(it.groupValues[2].trim(), it.groupValues[1] + ".") }
        return MdBlock.Paragraph(t)
    }

    /** Splits **bold** and `code` spans; unmatched markers are kept as literal text. */
    fun spans(text: String): List<MdSpan> {
        val out = mutableListOf<MdSpan>()
        val sb = StringBuilder()
        var i = 0
        fun flush() {
            if (sb.isNotEmpty()) { out += MdSpan.Plain(sb.toString()); sb.clear() }
        }
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                val end = text.indexOf("**", i + 2)
                // a real bold span starts right before a non-space character
                if (end > i + 2 && !text[i + 2].isWhitespace()) {
                    flush(); out += MdSpan.Bold(text.substring(i + 2, end)); i = end + 2; continue
                }
            }
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    flush(); out += MdSpan.Code(text.substring(i + 1, end)); i = end + 1; continue
                }
            }
            sb.append(text[i]); i++
        }
        flush()
        return out
    }

    /** Plain text (for search snippets, TalkBack and previews). */
    fun plain(src: String): String =
        src.replace(Regex("```\\w*\\n?"), "").replace("**", "").replace("`", "").replace(Regex("[ \\t]+"), " ").trim()
}
