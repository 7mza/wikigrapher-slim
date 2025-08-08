package com.wikigrapher.slim

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CommonsTest {
    data class Toto(
        val id: Long = 1L,
        val title: String = "title",
        val names: Set<String> = setOf("name1", "name2", "name3"),
    )

    @Test
    fun parseJson() {
        val result: Toto =
            Commons.parseJson<Toto>(
// @formatter:off
"""
{
  "id": 1,
  "title": "title",
  "names": ["name1", "name2", "name3"]
}
""".trimIndent(),
// @formatter:on
                ObjectMapper(),
            )
        assertThat(result).isEqualTo(Toto())
    }

    @Test
    fun escapeSingleQuotes() {
        assertThat(Commons.escapeSingleQuotes("Women's_sports")).isEqualTo("Women\\'s_sports")
    }

    @Test
    fun escapeDoubleQuotes() {
        assertThat(Commons.escapeDoubleQuotes("Powelliphanta_\"Matiri\"")).isEqualTo("Powelliphanta_\\\"Matiri\\\"")
    }

    @Test
    fun stripDoubleSlashes() {
        assertThat(Commons.stripDoubleSlashes("Women\\\\'s_sports")).isEqualTo("Women's_sports")
        assertThat(Commons.stripDoubleSlashes("Powelliphanta_\\\\\"Matiri\\\\\"")).isEqualTo("Powelliphanta_\"Matiri\"")
    }
}
