package com.wikigrapher.slim.paths

import com.wikigrapher.slim.Commons
import com.wikigrapher.slim.DELETE_ALL_QUERY
import com.wikigrapher.slim.ReactiveFileReader
import com.wikigrapher.slim.TYPE
import com.wikigrapher.slim.TestContainersFactory
import com.wikigrapher.slim.TestDatabaseHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("default", "neo4j")
@Testcontainers
class PathRedirectServiceTest {
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        private val neo4jContainer = TestContainersFactory.neo4jContainer()

        @JvmStatic
        @DynamicPropertySource
        private fun registerContainers(registry: DynamicPropertyRegistry) {
            registry.add("spring.neo4j.uri") {
                neo4jContainer.boltUrl
            }
        }
    }

    @Autowired
    private lateinit var service: IPathService

    @Autowired
    private lateinit var testDatabaseHelper: TestDatabaseHelper

    @Autowired
    private lateinit var reactiveFileReader: ReactiveFileReader

    @BeforeEach
    fun beforeEach() {
        reactiveFileReader
            .readFileAsString("classpath:dump.cypher")
            .flatMap {
                testDatabaseHelper.runCypherStatements(it)
            }.block()
    }

    @AfterEach
    fun afterEach() {
        testDatabaseHelper.runCypherStatements(DELETE_ALL_QUERY).block()
    }

    @Test
    fun getSourceType() {
        StepVerifier
            .create(service.getSourceType("mithrandir"))
            .expectNext(TYPE.REDIRECT)
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun shortestPathLength() {
        StepVerifier
            .create(service.shortestPathLength("the dark lord", "mithrandir"))
            .expectNext(3)
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun shortestPathByTitle() {
        StepVerifier
            .create(
                service
                    .shortestPathByTitle("the dark lord", "mithrandir")
                    .collectList()
                    .flatMapMany {
                        Flux.fromIterable(Commons.reconstructPathsFromRelations(it))
                    },
            ).recordWith { ArrayList() }
            .expectNextCount(1)
            .consumeRecordedWith { path ->
                path.forEach {
                    assertThat(it.size).isEqualTo(3)

                    val first = it.first()
                    assertThat(first.source.title).isEqualTo("the dark lord")
                    assertThat(first.source.type).isEqualTo(TYPE.REDIRECT)
                    assertThat(first.source.isTopParent).isTrue
                    assertThat(first.source.isBottomChild).isNull()

                    val last = it.last()
                    assertThat(last.target.title).isEqualTo("mithrandir")
                    assertThat(last.target.type).isEqualTo(TYPE.REDIRECT)
                    assertThat(last.target.isTopParent).isNull()
                    assertThat(last.target.isBottomChild).isTrue

                    assertThat(Commons.isPathConnected(it))
                }
            }.expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun shortestPathsByTitle() {
        StepVerifier
            .create(
                service
                    .shortestPathsByTitle("the dark lord", "mithrandir", 0, 100)
                    .collectList()
                    .flatMapMany {
                        Flux.fromIterable(Commons.reconstructPathsFromRelations(it))
                    },
            ).recordWith { ArrayList() }
            .expectNextCount(1)
            .consumeRecordedWith { path ->
                path.forEach {
                    assertThat(it.size).isEqualTo(3)

                    val first = it.first()
                    assertThat(first.source.title).isEqualTo("the dark lord")
                    assertThat(first.source.type).isEqualTo(TYPE.REDIRECT)
                    assertThat(first.source.isTopParent).isTrue
                    assertThat(first.source.isBottomChild).isNull()

                    val last = it.last()
                    assertThat(last.target.title).isEqualTo("mithrandir")
                    assertThat(last.target.type).isEqualTo(TYPE.REDIRECT)
                    assertThat(last.target.isTopParent).isNull()
                    assertThat(last.target.isBottomChild).isTrue

                    assertThat(Commons.isPathConnected(it))
                }
            }.expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun allShortestPathsByTitle() {
        StepVerifier
            .create(
                service
                    .allShortestPathsByTitle("the dark lord", "mithrandir")
                    .collectList()
                    .flatMapMany {
                        Flux.fromIterable(Commons.reconstructPathsFromRelations(it))
                    },
            ).recordWith { ArrayList() }
            .expectNextCount(1)
            .consumeRecordedWith { path ->
                path.forEach {
                    assertThat(it.size).isEqualTo(3)

                    val first = it.first()
                    assertThat(first.source.title).isEqualTo("the dark lord")
                    assertThat(first.source.type).isEqualTo(TYPE.REDIRECT)
                    assertThat(first.source.isTopParent).isTrue
                    assertThat(first.source.isBottomChild).isNull()

                    val last = it.last()
                    assertThat(last.target.title).isEqualTo("mithrandir")
                    assertThat(last.target.type).isEqualTo(TYPE.REDIRECT)
                    assertThat(last.target.isTopParent).isNull()
                    assertThat(last.target.isBottomChild).isTrue

                    assertThat(Commons.isPathConnected(it))
                }
            }.expectNextCount(0)
            .verifyComplete()
    }
}
