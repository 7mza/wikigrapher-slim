package com.wikigrapher.slim.orphans

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Transactional(readOnly = true)
interface IOrphanRepository : ReactiveNeo4jRepository<Orphan, String> {
    @Query("match (orphan:orphan) RETURN count(orphan)")
    fun countOrphans(): Mono<Long>
}
