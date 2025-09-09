package com.wikigrapher.slim.categories

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Transactional(readOnly = true)
interface ICategoryRepository : ReactiveNeo4jRepository<Category, String> {
    @Query(
// @formatter:off
"""
MATCH (source:category)
RETURN count(source)
""",
// @formatter:on
    )
    fun countCategories(): Mono<Long>

    @Query(
// @formatter:off
"""
MATCH ()-[r:contains]->()
RETURN count(r)
""",
// @formatter:on
    )
    fun countContains(): Mono<Long>
}
