package com.wikigrapher.slim.pages

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Transactional(readOnly = true)
interface IPageRepository : ReactiveNeo4jRepository<Page, String> {
    fun existsByTitle(title: String): Mono<Boolean>

    @Query(
// @formatter:off
        $$"""
MATCH (source:page)
WITH source, rand() AS rnd
ORDER BY rnd
LIMIT $n
RETURN source
""",
// @formatter:on
    )
    fun getNRandomPages(n: Int): Flux<PageProjection>

    @Query(
// @formatter:off
        $$"""
MATCH path = shortestPath((source:page {title: $sourceTitle})-[:link_to|redirect_to*1..100]->(target:page|redirect {title: $targetTitle}))
RETURN path
""",
// @formatter:on
    )
    fun shortestPath(
        sourceTitle: String,
        targetTitle: String,
    ): Mono<PageProjection>

    @Query(
// @formatter:off
        $$"""
MATCH path = shortestPath((source:page {title: $sourceTitle})-[:link_to|redirect_to*1..100]->(target:page|redirect {title: $targetTitle}))
RETURN length(path)
""",
// @formatter:on
    )
    fun shortestPathLength(
        sourceTitle: String,
        targetTitle: String,
    ): Mono<Int>

    @Query(
// @formatter:off
        $$"""
MATCH (source:page {title: $sourceTitle})
MATCH (target:page|redirect {title: $targetTitle})
OPTIONAL MATCH (redirects:redirect)-[:redirect_to]->(target)
CALL (source, target) {
  MATCH path = SHORTESTPATH((source)-[:link_to|redirect_to*1..100]->(target))
  RETURN length(path) AS len
}
CALL
  apoc.cypher.run(
    "CALL (source, target, len, redirects) {
      MATCH paths = ALLSHORTESTPATHS((source)-[:link_to|redirect_to*1.." + len + "]->(target))
      RETURN paths
      UNION
      OPTIONAL MATCH paths = ALLSHORTESTPATHS((source)-[:link_to|redirect_to*1.." + len + "]->(redirects))
      RETURN paths
    }
    RETURN paths",
    {source: source, target: target, len:len, redirects:redirects}
  )
YIELD value
WITH DISTINCT value.paths AS paths
RETURN COLLECT(paths)
""",
// @formatter:on
    )
    fun shortestPaths(
        sourceTitle: String,
        targetTitle: String,
    ): Mono<PageProjection>

    @Query(
// @formatter:off
        $$"""
MATCH (source:page {title: $sourceTitle})
MATCH (target:page|redirect {title: $targetTitle})
OPTIONAL MATCH (redirects:redirect)-[:redirect_to]->(target)
CALL (source, target) {
  MATCH path = SHORTESTPATH((source)-[:link_to|redirect_to*1..100]->(target))
  RETURN length(path) AS len
}
CALL
  apoc.cypher.run(
    "CALL (source, target, len, redirects) {
      MATCH paths = ALLSHORTESTPATHS((source)-[:link_to|redirect_to*1.." + len + "]->(target))
      RETURN paths
      UNION
      OPTIONAL MATCH paths = ALLSHORTESTPATHS((source)-[:link_to|redirect_to*1.." + len + "]->(redirects))
      RETURN paths
    }
    RETURN paths",
    {source: source, target: target, len:len, redirects:redirects}
  )
YIELD value
WITH DISTINCT value.paths AS paths
SKIP $skip LIMIT $limit
RETURN COLLECT(paths)
""",
// @formatter:on
    )
    fun shortestPaths(
        sourceTitle: String,
        targetTitle: String,
        skip: Int,
        limit: Int,
    ): Mono<PageProjection>

    @Query(
// @formatter:off
"""
MATCH (source:page)
RETURN count(source)
""",
// @formatter:on
    )
    fun countPages(): Mono<Long>

    @Query(
// @formatter:off
"""
MATCH ()-[r:link_to]->()
RETURN count(r)
""",
// @formatter:on
    )
    fun countLinkTo(): Mono<Long>
}
