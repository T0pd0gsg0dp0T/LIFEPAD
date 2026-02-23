package com.lifepad.app.domain.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WikilinkParserTest {

    @Test
    fun extractWikilinks_deduplicatesTargets() {
        val content = "See [[Daily Note]] and [[Daily Note|today]] for context."

        val result = WikilinkParser.extractWikilinks(content)

        assertThat(result).containsExactly("Daily Note")
    }

    @Test
    fun parseWikilinks_supportsAliasAndHeading() {
        val content = "Review [[Project Plan|Plan]] then [[Project Plan#Risks]]."

        val tokens = WikilinkParser.parseWikilinks(content)

        assertThat(tokens).hasSize(2)
        assertThat(tokens[0].target).isEqualTo("Project Plan")
        assertThat(tokens[0].alias).isEqualTo("Plan")
        assertThat(tokens[1].target).isEqualTo("Project Plan")
        assertThat(tokens[1].alias).isNull()
    }

    @Test
    fun replaceWikilinks_usesTargetOnly() {
        val content = "Open [[My Note|Alias]] please."

        val replaced = WikilinkParser.replaceWikilinks(content) { "note:$it" }

        assertThat(replaced).isEqualTo("Open note:My Note please.")
    }

    @Test
    fun escapeWikilinks_escapesBrackets() {
        val escaped = WikilinkParser.escapeWikilinks("Use [[brackets]]")

        assertThat(escaped).isEqualTo("Use \\[\\[brackets\\]\\]")
    }
}
