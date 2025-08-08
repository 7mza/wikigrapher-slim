package com.wikigrapher.slim.meta

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Transactional(readOnly = true)
interface IMetaRepository : ReactiveNeo4jRepository<Meta, String> {
    @Query($$"MATCH (source:meta {property: $property}) RETURN source")
    fun findMetaByProperty(property: String): Mono<MetaProjection>
}
