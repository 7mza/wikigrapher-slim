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

        // FIXME: this crap is just for reactor unit tests
        fun isPathConnected(path: List<RelationDto>): Boolean = path.zipWithNext().all { (a, b) -> a.target == b.source }

        // FIXME: this crap is just for reactor unit tests
        fun reconstructPathsFromRelations(relations: List<RelationDto>): List<List<RelationDto>> {
            val graph = relations.groupBy { it.source.id }
            val rootNodes =
                relations
                    .map { it.source }
                    .filter { it.isTopParent }
                    .toSet()
            val paths = mutableListOf<List<RelationDto>>()

            fun dfs(
                currentNode: NodeSubDto,
                pathSoFar: List<RelationDto>,
                visited: Set<String>,
            ) {
                if (currentNode.id in visited) {
                    return
                }
                val outgoingRelations = graph[currentNode.id].orEmpty()
                if (currentNode.isBottomChild || outgoingRelations.isEmpty()) {
                    paths.add(pathSoFar)
                    return
                }
                for (relation in outgoingRelations) {
                    dfs(relation.target, pathSoFar + relation, visited + currentNode.id)
                }
            }
            for (root in rootNodes) {
                dfs(root, emptyList(), emptySet())
            }
            return paths
        }
    }
}
