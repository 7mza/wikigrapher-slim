package com.wikigrapher.slim.redirects

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Transactional(readOnly = true)
interface IRedirectRepository : ReactiveNeo4jRepository<Redirect, String> {
    fun existsByTitle(title: String): Mono<Boolean>

    @Query(
        $$"MATCH path = shortestPath((source:redirect {title: $sourceTitle})-[:link_to|redirect_to*1..100]->" +
            $$"(target:page|redirect {title: $targetTitle})) RETURN path",
    )
    fun shortestPath(
        sourceTitle: String,
        targetTitle: String,
    ): Mono<RedirectProjection>

    @Query(
        $$"MATCH path = shortestPath((source:redirect {title: $sourceTitle})-[:link_to|redirect_to*1..100]->" +
            $$"(target:page|redirect {title: $targetTitle})) RETURN length(path)",
    )
    fun shortestPathLength(
        sourceTitle: String,
        targetTitle: String,
    ): Mono<Int>

    @Query(
        $$"MATCH path = allShortestPaths((source:redirect {title: $sourceTitle})-[:link_to|redirect_to*1..100]->" +
            $$"(target:page|redirect {title: $targetTitle})) RETURN COLLECT(path)",
    )
    fun shortestPaths(
        sourceTitle: String,
        targetTitle: String,
    ): Mono<RedirectProjection>

    @Query(
        $$"MATCH path = allShortestPaths((source:redirect {title: $sourceTitle})-[:link_to|redirect_to*1..100]->" +
            $$"(target:page|redirect {title: $targetTitle})) WITH path SKIP $skip LIMIT $limit RETURN COLLECT(path)",
    )
    fun shortestPaths(
        sourceTitle: String,
        targetTitle: String,
        skip: Int,
        limit: Int,
    ): Mono<RedirectProjection>

    @Query("match (source:redirect) RETURN count(source)")
    fun countRedirects(): Mono<Long>

    @Query("match ()-[r:redirect_to]->() RETURN count(r)")
    fun countRedirectTo(): Mono<Long>
}
