package com.wikigrapher.slim.categories

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Transactional(readOnly = true)
interface ICategoryRepository : ReactiveNeo4jRepository<Category, String> {
    @Query("match (source:category) RETURN count(source)")
    fun countCategories(): Mono<Long>

    @Query("match ()-[r:contains]->() RETURN count(r)")
    fun countContains(): Mono<Long>
}
