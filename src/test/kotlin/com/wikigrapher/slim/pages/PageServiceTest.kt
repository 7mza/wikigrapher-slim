package com.wikigrapher.slim.pages

import com.wikigrapher.slim.DELETE_ALL_QUERY
import com.wikigrapher.slim.NodeDto
import com.wikigrapher.slim.ReactiveFileReader
import com.wikigrapher.slim.TestContainersFactory
import com.wikigrapher.slim.TestDatabaseHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
@Testcontainers
@ActiveProfiles("default", "neo4j")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PageServiceTest {
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        private val neo4jContainer = TestContainersFactory.neo4jContainer()

        @JvmStatic
        @DynamicPropertySource
        private fun registerContainers(registry: DynamicPropertyRegistry) {
            registry.add("neo4j.host") {
                neo4jContainer.host
            }
            registry.add("neo4j.port") {
                neo4jContainer.firstMappedPort
            }
        }
    }

    @Autowired
    private lateinit var service: IPageService

    @Autowired
    private lateinit var testDatabaseHelper: TestDatabaseHelper

    @Autowired
    private lateinit var reactiveFileReader: ReactiveFileReader

    @BeforeAll
    fun beforeAll() {
        reactiveFileReader
            .readFileFromResources("classpath:dump.cypher")
            .flatMap {
                testDatabaseHelper.runCypherStatements(it)
            }.block()
    }

    @AfterAll
    fun afterAll() {
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
