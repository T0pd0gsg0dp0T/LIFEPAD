package com.lifepad.app.domain.parser

object HashtagParser {
    // Matches #hashtag where hashtag starts with a letter and can contain letters, numbers, underscores
    private val HASHTAG_REGEX = Regex("""#([a-zA-Z][a-zA-Z0-9_]*)""")

    /**
     * Extract all hashtag names from content (without # prefix, lowercase)
     */
    fun extractHashtags(content: String): List<String> {
        return HASHTAG_REGEX.findAll(content)
            .map { it.groupValues[1].lowercase() }
            .distinct()
            .toList()
    }

    /**
     * Find hashtag positions for highlighting
     * Returns list of (range, hashtagName without #)
     */
    fun findHashtagRanges(content: String): List<Pair<IntRange, String>> {
        return HASHTAG_REGEX.findAll(content)
            .map { it.range to it.groupValues[1].lowercase() }
            .toList()
    }

    /**
     * Check if a string is a valid hashtag name (without #)
     */
    fun isValidHashtagName(name: String): Boolean {
        return name.matches(Regex("""^[a-zA-Z][a-zA-Z0-9_]*$"""))
    }

    /**
     * Normalize a hashtag name (lowercase, trim)
     */
    fun normalize(name: String): String {
        return name.lowercase().trim()
    }
}
