package com.lifepad.app.domain.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HashtagParserTest {

    @Test
    fun extractHashtags_deduplicatesAndLowercases() {
        val content = "This is #Work and #work plus #Finance_2024."

        val result = HashtagParser.extractHashtags(content)

        assertThat(result).containsExactly("work", "finance_2024")
    }

    @Test
    fun findHashtagRanges_returnsLowercasedNames() {
        val content = "Track #Mood and #Sleep today."

        val ranges = HashtagParser.findHashtagRanges(content)

        assertThat(ranges.map { it.second }).containsExactly("mood", "sleep")
        assertThat(ranges.first().first).isEqualTo(6..10)
    }

    @Test
    fun isValidHashtagName_requiresLeadingLetter() {
        assertThat(HashtagParser.isValidHashtagName("work_1")).isTrue()
        assertThat(HashtagParser.isValidHashtagName("_work")).isFalse()
        assertThat(HashtagParser.isValidHashtagName("1work")).isFalse()
    }

    @Test
    fun normalize_trimsAndLowercases() {
        assertThat(HashtagParser.normalize("  Mood  ")).isEqualTo("mood")
    }
}
