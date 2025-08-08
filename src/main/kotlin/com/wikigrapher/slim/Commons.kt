package com.wikigrapher.slim

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

class Commons {
    companion object {
        inline fun <reified T> parseJson(
            json: String,
            objectMapper: ObjectMapper,
        ): T = objectMapper.readValue(json.trimIndent())

        // for neo4j
        fun sanitizeInput(input: String): String =
            input
                .let(::escapeSingleQuotes)
                .let(::escapeDoubleQuotes)

        fun escapeSingleQuotes(input: String): String = input.replace(Regex("(?<!\\\\)'"), "\\\\'")

        fun escapeDoubleQuotes(input: String): String = input.replace(Regex("(?<!\\\\)\""), "\\\\\"")

        fun sanitizeOutput(output: String): String = output.let(::stripDoubleSlashes)

        fun stripDoubleSlashes(output: String): String = output.replace("\\\\".toRegex(), "")
    }
}
