package com.wikigrapher.slim

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.neo4j.core.ReactiveNeo4jClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

const val DELETE_ALL_QUERY = "MATCH ()-[r]-() DELETE r; MATCH (n) DELETE n;"

@Service
class TestDatabaseHelper
    @Autowired
    constructor(
        private val neo4jClient: ReactiveNeo4jClient,
    ) {
        fun runCypherStatements(queryText: String): Mono<Void> {
            val queries =
                queryText
                    .split(";")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            return Flux
                .fromIterable(queries)
                .concatMap { query ->
                    neo4jClient.query(query).run().then()
                }.then()
        }
    }
