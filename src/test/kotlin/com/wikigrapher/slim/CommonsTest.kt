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

    @Test
    fun isPathConnected() {
        val relations: List<RelationDto> =
            listOf(
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE, isTopParent = true),
                    NodeSubDto("12", "good", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("12", "good", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT),
                ),
                RelationDto(
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT),
                    NodeSubDto("9", "celebrimbor", TYPE.PAGE, isBottomChild = true),
                ),
            )
        assertThat(Commons.isPathConnected(relations)).isTrue
        assertThat(Commons.isPathConnected(relations - relations[1])).isFalse
    }

    @Test
    fun reconstructPathsFromRelations() {
        val relations: List<RelationDto> =
            listOf(
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE, isTopParent = true),
                    NodeSubDto("12", "good", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("12", "good", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT),
                ),
                RelationDto(
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT),
                    NodeSubDto("9", "celebrimbor", TYPE.PAGE, isBottomChild = true),
                ),
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE, isTopParent = true),
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT),
                ),
                RelationDto(
                    NodeSubDto("100", "zebi", TYPE.PAGE),
                    NodeSubDto("9", "celebrimbor", TYPE.PAGE, isBottomChild = true),
                ),
            )

        val paths = Commons.reconstructPathsFromRelations(relations)
        assertThat(paths.size).isEqualTo(2)

        paths.forEach {
            assertThat(it.first().source.title).isEqualTo("wizard")
            assertThat(it.first().source.isTopParent).isTrue

            assertThat(it.last().target.title).isEqualTo("celebrimbor")
            assertThat(it.last().target.isBottomChild).isTrue

            assertThat(Commons.isPathConnected(it)).isTrue
        }
    }

    @Test
    fun `reconstructPathsFromRelations, ignore parentless source`() {
        val relations: List<RelationDto> =
            listOf(
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                    NodeSubDto("12", "good", TYPE.PAGE, isBottomChild = true),
                ),
            )
        val paths = Commons.reconstructPathsFromRelations(relations)
        assertThat(paths.size).isZero
    }

    @Test
    fun `reconstructPathsFromRelations, ignore disconnected`() {
        val relations: List<RelationDto> =
            listOf(
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE, isTopParent = true),
                    NodeSubDto("12", "good", TYPE.PAGE, isBottomChild = true),
                ),
                RelationDto(
                    NodeSubDto("100", "zebi", TYPE.PAGE),
                    NodeSubDto("101", "bezi", TYPE.PAGE, isTopParent = true, isBottomChild = true),
                ),
            )
        val paths = Commons.reconstructPathsFromRelations(relations)
        assertThat(paths.size).isOne
        assertThat(
            paths
                .first()
                .first()
                .source.title,
        ).isEqualTo("wizard")
        assertThat(
            paths
                .first()
                .last()
                .target.title,
        ).isEqualTo("good")
    }

    @Test
    fun `reconstructPathsFromRelations, infinite loop`() {
        val relations: List<RelationDto> =
            listOf(
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE, isTopParent = true),
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                    NodeSubDto("11", "wizard", TYPE.PAGE, isTopParent = true),
                ),
            )
        val paths = Commons.reconstructPathsFromRelations(relations)
        assertThat(paths.size).isZero
    }
}
