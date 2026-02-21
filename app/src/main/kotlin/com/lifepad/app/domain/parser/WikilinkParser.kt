package com.lifepad.app.domain.parser

object WikilinkParser {
    // Matches [[Note Title]], [[Note Title|Alias]], [[Note Title#Heading]]
    private val WIKILINK_REGEX = Regex("""\[\[([^\]]+)]]""")

    data class WikilinkToken(
        val range: IntRange,
        val raw: String,
        val target: String,
        val alias: String?
    ) {
        val displayText: String
            get() = alias?.takeIf { it.isNotBlank() } ?: target
    }

    private fun extractTarget(raw: String): String {
        return raw
            .substringBefore('|')
            .substringBefore('#')
            .trim()
    }

    private fun extractAlias(raw: String): String? {
        val alias = raw.substringAfter('|', "").trim()
        return alias.takeIf { it.isNotBlank() }
    }

    /**
     * Extract all wikilink titles from content
     */
    fun extractWikilinks(content: String): List<String> {
        return parseWikilinks(content)
            .map { it.target }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    /**
     * Parse wikilinks and preserve alias data and source ranges.
     */
    fun parseWikilinks(content: String): List<WikilinkToken> {
        return WIKILINK_REGEX.findAll(content)
            .map { match ->
                val raw = match.groupValues[1]
                WikilinkToken(
                    range = match.range,
                    raw = raw,
                    target = extractTarget(raw),
                    alias = extractAlias(raw)
                )
            }
            .filter { it.target.isNotBlank() }
            .toList()
    }

    /**
     * Find wikilink positions for highlighting/clickability
     * Returns list of (range, noteTitle)
     */
    fun findWikilinkRanges(content: String): List<Pair<IntRange, String>> {
        return parseWikilinks(content)
            .map { it.range to it.target }
            .toList()
    }

    /**
     * Check if content contains any wikilinks
     */
    fun containsWikilinks(content: String): Boolean {
        return WIKILINK_REGEX.containsMatchIn(content)
    }

    /**
     * Replace wikilinks with a custom format
     * @param content The text content
     * @param replacer Function that takes the note title and returns the replacement text
     */
    fun replaceWikilinks(content: String, replacer: (String) -> String): String {
        return WIKILINK_REGEX.replace(content) { match ->
            val title = extractTarget(match.groupValues[1])
            replacer(title)
        }
    }

    /**
     * Escape content so that [[ and ]] are not interpreted as wikilinks
     */
    fun escapeWikilinks(content: String): String {
        return content.replace("[[", "\\[\\[").replace("]]", "\\]\\]")
    }
}
