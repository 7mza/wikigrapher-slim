package com.wikigrapher.slim.meta

import com.wikigrapher.slim.DELETE_ALL_QUERY
import com.wikigrapher.slim.DumpMetaDto
import com.wikigrapher.slim.DumpNodes
import com.wikigrapher.slim.DumpRelations
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
class MetaServiceTest {
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
    private lateinit var service: IMetaService

    @Autowired
    private lateinit var testDatabaseHelper: TestDatabaseHelper

    @Autowired
    private lateinit var reactiveFileReader: ReactiveFileReader

    @BeforeEach
    fun init() {
        reactiveFileReader
            .readFileAsString("classpath:dump.cypher")
            .flatMap {
                testDatabaseHelper.runCypherStatements(it)
            }.block()
    }

    @AfterEach
    fun clean() {
        testDatabaseHelper.runCypherStatements(DELETE_ALL_QUERY).block()
    }

    @Test
    fun findDumpMeta() {
        StepVerifier
            .create(service.findDumpMeta())
            .expectNextMatches {
                assertThat(it).isEqualTo(
                    DumpMetaDto(
                        lang = "en",
                        date = "11111111",
                        url = "https://dumps.wikimedia.org/enwiki/11111111",
                        nodes = DumpNodes(pages = 11, redirects = 11, categories = 3, orphans = 3),
                        relations = DumpRelations(linkTo = 14, redirectTo = 8, belongTo = 10),
                    ),
                )
                true
            }.expectNextCount(0)
            .verifyComplete()
    }
}
