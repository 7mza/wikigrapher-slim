package com.wikigrapher.slim.pages

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Transactional(readOnly = true)
interface IPageRepository : ReactiveNeo4jRepository<Page, String> {
    @Query($$"MATCH (source:page) WITH source, rand() AS rnd ORDER BY rnd LIMIT $N RETURN source")
    fun getNRandomPages(
        @Param("N") n: Int,
    ): Flux<PageProjection>

    @Query(
        $$"MATCH path = shortestPath((source:page {title: $sourceTitle})-[:link_to|redirect_to*1..100]->" +
            $$"(target:page|redirect {title: $targetTitle})) RETURN path",
    )
    fun shortestPath(
        sourceTitle: String,
        targetTitle: String,
    ): Mono<PageProjection>

    @Query(
        $$"MATCH path = shortestPath((source:page|redirect {title: $sourceTitle})-[:link_to|redirect_to*1..100]->" +
            $$"(target:page|redirect {title: $targetTitle})) RETURN length(path)",
    )
    fun shortestPathLength(
        sourceTitle: String,
        targetTitle: String,
    ): Mono<Int>

    @Query(
        $$"MATCH path = allShortestPaths((source:page|redirect {title: $sourceTitle})-[:link_to|redirect_to*1..100]->" +
            $$"(target:page|redirect {title: $targetTitle})) RETURN COLLECT(path)",
    )
    fun shortestPaths(
        sourceTitle: String,
        targetTitle: String,
    ): Mono<PageProjection>

    @Query(
        $$"MATCH path = allShortestPaths((source:page|redirect {title: $sourceTitle})-[:link_to|redirect_to*1..100]->" +
            $$"(target:page|redirect {title: $targetTitle})) WITH path SKIP $SKIP LIMIT $LIMIT RETURN COLLECT(path)",
    )
    fun shortestPaths(
        sourceTitle: String,
        targetTitle: String,
        @Param("SKIP") skip: Int,
        @Param("LIMIT") limit: Int,
    ): Mono<PageProjection>

    @Query("match (source:page) RETURN count(source)")
    fun countPages(): Mono<Long>

    @Query("match ()-[r:link_to]->() RETURN count(r)")
    fun countLinkTo(): Mono<Long>
}
