package com.wikigrapher.slim.redirects

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Transactional(readOnly = true)
interface IRedirectRepository : ReactiveNeo4jRepository<Redirect, String> {
    fun existsByTitle(title: String): Mono<Boolean>

    @Query(
// @formatter:off
        $$"""
MATCH path = shortestPath((source:redirect {title: $sourceTitle})-[:link_to|redirect_to*1..100]->(target:page|redirect {title: $targetTitle}))
RETURN path
""",
// @formatter:on
    )
    fun shortestPath(
        sourceTitle: String,
        targetTitle: String,
    ): Mono<RedirectProjection>

    @Query(
// @formatter:off
        $$"""
MATCH path = shortestPath((source:redirect {title: $sourceTitle})-[:link_to|redirect_to*1..100]->(target:page|redirect {title: $targetTitle}))
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
MATCH (source:redirect {title: $sourceTitle})
MATCH (target:page|redirect {title: $targetTitle})
OPTIONAL MATCH (redirects:redirect)-[:redirect_to]->(target)
MATCH paths = allShortestPaths((source)-[:link_to|redirect_to*1..20]->(target))
WITH paths AS tmp, length(paths) AS len, source, redirects
CALL
  apoc.cypher.run(
    "CALL (source, len, redirects, tmp) {
        OPTIONAL MATCH paths = ALLSHORTESTPATHS(
          (source)-[:link_to|redirect_to*1.." + len + "]->(redirects)
        )
        RETURN paths
        UNION
        RETURN tmp as paths }
        WITH paths, [node IN nodes(paths) | node.title] AS titles
        ORDER BY titles
        RETURN paths",
    {source: source, redirects: redirects, len: len, tmp:tmp}
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
    ): Mono<RedirectProjection>

    @Query(
// @formatter:off
        $$"""
MATCH (source:redirect {title: $sourceTitle})
MATCH (target:page|redirect {title: $targetTitle})
OPTIONAL MATCH (redirects:redirect)-[:redirect_to]->(target)
MATCH paths = allShortestPaths((source)-[:link_to|redirect_to*1..20]->(target))
WITH paths AS tmp, length(paths) AS len, source, redirects
CALL
  apoc.cypher.run(
    "CALL (source, len, redirects, tmp) {
        OPTIONAL MATCH paths = ALLSHORTESTPATHS(
          (source)-[:link_to|redirect_to*1.." + len + "]->(redirects)
        )
        RETURN paths
        UNION
        RETURN tmp as paths }
        WITH paths, [node IN nodes(paths) | node.title] AS titles
        ORDER BY titles
        RETURN paths",
    {source: source, redirects: redirects, len: len, tmp:tmp}
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
    ): Mono<RedirectProjection>

    @Query(
// @formatter:off
"""
MATCH (source:redirect)
RETURN count(source)
""",
// @formatter:on
    )
    fun countRedirects(): Mono<Long>

    @Query(
// @formatter:off
"""
MATCH ()-[r:redirect_to]->()
RETURN count(r)
""",
// @formatter:on
    )
    fun countRedirectTo(): Mono<Long>
}
