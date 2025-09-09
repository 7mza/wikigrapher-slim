package com.wikigrapher.slim.meta

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Transactional(readOnly = true)
interface IMetaRepository : ReactiveNeo4jRepository<Meta, String> {
    @Query(
// @formatter:off
        $$"""
MATCH (source:meta {property: $property})
RETURN source
""",
// @formatter:on
    )
    fun findMetaByProperty(property: String): Mono<MetaProjection>
}
