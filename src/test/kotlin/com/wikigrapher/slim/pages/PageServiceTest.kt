package com.wikigrapher.slim.pages

import com.wikigrapher.slim.DELETE_ALL_QUERY
import com.wikigrapher.slim.NodeDto
import com.wikigrapher.slim.ReactiveFileReader
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
import reactor.test.StepVerifier

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("default", "neo4j")
@Testcontainers
class PageServiceTest {
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
    private lateinit var service: IPageService

    @Autowired
    private lateinit var testDatabaseHelper: TestDatabaseHelper

    @Autowired
    private lateinit var reactiveFileReader: ReactiveFileReader

    @BeforeEach
    fun init() {
        reactiveFileReader
            .readFileFromResources("classpath:dump.cypher")
            .flatMap {
                testDatabaseHelper.runCypherStatements(it)
            }.block()
    }

    @AfterEach
    fun clean() {
        testDatabaseHelper.runCypherStatements(DELETE_ALL_QUERY).block()
    }

    @Test
    fun getNRandomPages() {
        var id1 = ""
        var id2 = ""
        StepVerifier
            .create(service.getNRandomPages(2))
            .expectNextMatches {
                assertThat(it).isInstanceOf(NodeDto::class.java)
                assertThat(it.id).isNotNull
                id1 = it.id
                true
            }.expectNextMatches {
                assertThat(it).isInstanceOf(NodeDto::class.java)
                assertThat(it.id).isNotNull
                id2 = it.id
                true
            }.expectNextCount(0)
            .verifyComplete()
        assertThat(id1).isNotEqualTo(id2)
    }
}
