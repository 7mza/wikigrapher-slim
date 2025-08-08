package com.wikigrapher.slim.redirects

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Transactional(readOnly = true)
interface IRedirectRepository : ReactiveNeo4jRepository<Redirect, String> {
    @Query("match (source:redirect) RETURN count(source)")
    fun countRedirects(): Mono<Long>

    @Query("match ()-[r:redirect_to]->() RETURN count(r)")
    fun countRedirectTo(): Mono<Long>
}
