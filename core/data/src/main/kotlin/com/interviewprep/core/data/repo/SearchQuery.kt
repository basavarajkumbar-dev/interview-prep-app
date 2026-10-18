package com.interviewprep.core.data.repo

/** Builds safe FTS4 MATCH expressions and plain-text snippets for search results. */
object SearchQuery {
    private val STOP = setOf("and", "or", "not", "near", "the", "of", "in", "to", "is", "for", "with", "an", "on", "at", "by", "what", "how", "why")

    /** "pytest fixture" -> "pytest* fixture*". Returns null for empty input. FTS syntax characters are removed. */
    fun toFtsMatch(raw: String): String? {
        val words = raw.lowercase().split(Regex("[^\\p{L}\\p{N}_]+")).filter { it.length >= 2 && it !in STOP }.take(6)
        if (words.isEmpty()) return null
        return words.joinToString(" ") { "$it*" }
    }

    /** ~160 characters of plain text around the first matching word. */
    fun snippet(body: String, raw: String, width: Int = 160): String {
        val plain = body.replace(Regex("```\\w*\\n?"), " ").replace("**", "").replace(Regex("\\s+"), " ").trim()
        val words = raw.lowercase().split(Regex("\\s+")).filter { it.length >= 2 }
        val idx = words.map { plain.lowercase().indexOf(it) }.filter { it >= 0 }.minOrNull() ?: 0
        val start = (idx - width / 3).coerceAtLeast(0)
        val end = (start + width).coerceAtMost(plain.length)
        return (if (start > 0) "…" else "") + plain.substring(start, end) + (if (end < plain.length) "…" else "")
    }
}
