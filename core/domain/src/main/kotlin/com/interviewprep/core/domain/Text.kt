package com.interviewprep.core.domain

/** Text helpers shared by graders and scorers. */
object Text {
    private val quotes = mapOf('’' to '\'', '‘' to '\'', '“' to '"', '”' to '"', '‐' to '-', '–' to '-', '—' to '-')

    fun normalize(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(quotes[c] ?: c)
        return sb.toString().replace("**", "").replace("`", "").trim()
    }

    /** Lower-case, strip surrounding punctuation and collapse whitespace: for fill-in answers. */
    fun loose(s: String): String =
        normalize(s).lowercase()
            .replace(Regex("[\"'()\\[\\]]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('.', ',', ';', ':', '!')

    /** Exact-but-forgiving comparison of program output: trailing spaces and blank edges ignored. */
    fun normalizeOutput(s: String): String =
        normalize(s).lines().joinToString("\n") { it.trimEnd() }.trim()

    private val stop = setOf(
        "the", "and", "for", "with", "that", "this", "from", "when", "then", "than", "you", "your", "are", "can",
        "not", "use", "which", "what", "into", "also", "only", "each", "any", "all", "same", "other", "such",
        "may", "must", "very", "about", "after", "before", "over", "under", "there", "they", "them", "our",
        "just", "like", "have", "has", "had", "been", "being", "where", "who", "while", "because", "would",
        "will", "should", "could", "does", "did", "its", "it's", "i'd", "i'm", "was", "were", "how", "why",
    )

    fun tokens(s: String): List<String> =
        Regex("[A-Za-z0-9_@/.+#-]+").findAll(normalize(s).lowercase())
            .map { it.value.trim('.', '-', '/') }
            .filter { it.length >= 2 && it !in stop }
            .toList()

    /** Very small stemmer so "reboots"/"rebooted"/"rebooting" match "reboot". */
    fun stem(w: String): String {
        var t = w.lowercase()
        for (suf in listOf("ing", "ed", "es", "s", "ly")) {
            if (t.length > suf.length + 3 && t.endsWith(suf)) {
                t = t.dropLast(suf.length)
                break
            }
        }
        return t
    }
}
