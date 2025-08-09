package com.wikigrapher.slim.paths

import com.wikigrapher.slim.DELETE_ALL_QUERY
import com.wikigrapher.slim.NodeSubDto
import com.wikigrapher.slim.ReactiveFileReader
import com.wikigrapher.slim.RelationDto
import com.wikigrapher.slim.TYPE
import com.wikigrapher.slim.TestContainersFactory
import com.wikigrapher.slim.TestDatabaseHelper
import com.wikigrapher.slim.pages.IPageRepository
import com.wikigrapher.slim.redirects.IRedirectRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.test.StepVerifier

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("default", "neo4j")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PathServiceTest {
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
    private lateinit var service: IPathService

    @Autowired
    private lateinit var testDatabaseHelper: TestDatabaseHelper

    @Autowired
    private lateinit var reactiveFileReader: ReactiveFileReader

    @MockitoSpyBean
    private lateinit var pageRepository: IPageRepository

    @MockitoSpyBean
    private lateinit var redirectRepository: IRedirectRepository

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
    fun `getSourceType for page should return page`() {
        StepVerifier
            .create(service.getSourceType("wizard"))
            .expectNext(TYPE.PAGE)
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `getSourceType for redirect should return redirect`() {
        StepVerifier
            .create(service.getSourceType("mithrandir"))
            .expectNext(TYPE.REDIRECT)
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `getSourceType for non existing should return empty and not throw Exception`() {
        StepVerifier
            .create(service.getSourceType("zebi"))
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathLength between 2 pages should return correct length`() {
        StepVerifier
            .create(service.shortestPathLength("wizard", "celebrimbor"))
            .expectNext(3)
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathLength between 2 redirects should return correct length`() {
        StepVerifier
            .create(service.shortestPathLength("mithrandir", "the bright lord"))
            .expectNext(4)
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathLength between page and redirect should return correct length`() {
        StepVerifier
            .create(service.shortestPathLength("good", "the bright lord"))
            .expectNext(1)
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathLength between redirect and page should return correct length`() {
        StepVerifier
            .create(service.shortestPathLength("mithrandir", "good"))
            .expectNext(3)
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathLength with same input and target should return 0 and not call db`() {
        StepVerifier
            .create(service.shortestPathLength("wizard", "wizard"))
            .expectNext(0)
            .expectNextCount(0)
            .verifyComplete()
        verify(pageRepository, times(0))
            .shortestPathLength(anyString(), anyString())
        verify(redirectRepository, times(0))
            .shortestPathLength(anyString(), anyString())
    }

    @Test
    fun `shortestPathLength with non existing source should return 0 and not throw any Exception`() {
        StepVerifier
            .create(service.shortestPathLength("zebi", "celebrimbor"))
            .expectNext(0)
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathByTitle between 2 pages should return correct path`() {
        StepVerifier
            .create(service.shortestPathByTitle("morgoth", "gandalf"))
            .expectNext(
                RelationDto(
                    NodeSubDto("10", "morgoth", TYPE.PAGE, isTopParent = true),
                    NodeSubDto("25", "stormcrow", TYPE.REDIRECT),
                ),
                RelationDto(
                    NodeSubDto("25", "stormcrow", TYPE.REDIRECT),
                    NodeSubDto("4", "mithrandir", TYPE.REDIRECT),
                ),
                RelationDto(
                    NodeSubDto("4", "mithrandir", TYPE.REDIRECT),
                    NodeSubDto("3", "gandalf", TYPE.PAGE, isBottomChild = true),
                ),
            ).expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathByTitle between 2 redirects should return correct path`() {
        StepVerifier
            .create(service.shortestPathByTitle("mithrandir", "the bright lord"))
            .expectNext(
                RelationDto(
                    NodeSubDto("4", "mithrandir", TYPE.REDIRECT, isTopParent = true),
                    NodeSubDto("3", "gandalf", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("3", "gandalf", TYPE.PAGE),
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT, isBottomChild = true),
                ),
            ).expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathByTitle with same input and target should return empty and not call db`() {
        StepVerifier
            .create(service.shortestPathByTitle("wizard", "wizard"))
            .expectNextCount(0)
            .verifyComplete()
        verify(pageRepository, times(0))
            .shortestPath(anyString(), anyString())
        verify(redirectRepository, times(0))
            .shortestPathLength(anyString(), anyString())
    }

    @Test
    fun `shortestPathByTitle with non existing target should return empty and not throw NoSuchElementException`() {
        StepVerifier
            .create(service.shortestPathByTitle("wizard", "toto"))
            .expectNextCount(0)
            .verifyComplete()
        verify(pageRepository, times(1))
            .shortestPath(anyString(), anyString())
        verify(redirectRepository, times(0))
            .shortestPathLength(anyString(), anyString())
    }

    @Test
    fun `shortestPathByTitle with non existing source should return empty and not throw any Exception`() {
        StepVerifier
            .create(service.shortestPathByTitle("zebi", "celebrimbor"))
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun getRandomShortestPath() {
        // FIXME: data too small, fire many getRandomShortestPath and expect at least one path found
    }

    @Test
    fun `shortestPathsByTitle between 2 pages should return correct paths`() {
        StepVerifier
            .create(service.shortestPathsByTitle("wizard", "celebrimbor", 0, 100))
            .expectNext(
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE, isTopParent = true),
                    NodeSubDto("12", "good", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("12", "good", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT),
                ),
                RelationDto(
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT),
                    NodeSubDto("9", "celebrimbor", TYPE.PAGE, isBottomChild = true),
                ),
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE, isTopParent = true),
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT),
                ),
            ).expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathsByTitle between 2 redirects should return correct path`() {
        StepVerifier
            .create(service.shortestPathsByTitle("mithrandir", "the bright lord", 0, 100))
            .expectNext(
                RelationDto(
                    NodeSubDto("4", "mithrandir", TYPE.REDIRECT, isTopParent = true),
                    NodeSubDto("3", "gandalf", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("3", "gandalf", TYPE.PAGE),
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                    NodeSubDto("12", "good", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("12", "good", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT, isBottomChild = true),
                ),
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT, isBottomChild = true),
                ),
            ).expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathsByTitle should respect skip`() {
        StepVerifier
            .create(service.shortestPathsByTitle("mithrandir", "the bright lord", 1, 100))
            .expectNext(
                RelationDto(
                    NodeSubDto("4", "mithrandir", TYPE.REDIRECT, isTopParent = true),
                    NodeSubDto("3", "gandalf", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("3", "gandalf", TYPE.PAGE),
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT, isBottomChild = true),
                ),
            ).expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathsByTitle should respect limit`() {
        StepVerifier
            .create(service.shortestPathsByTitle("mithrandir", "the bright lord", 0, 1))
            .expectNext(
                RelationDto(
                    NodeSubDto("4", "mithrandir", TYPE.REDIRECT, isTopParent = true),
                    NodeSubDto("3", "gandalf", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("3", "gandalf", TYPE.PAGE),
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                    NodeSubDto("12", "good", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("12", "good", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT, isBottomChild = true),
                ),
            ).expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathsByTitle should respect skip and limit`() {
        StepVerifier
            .create(service.shortestPathsByTitle("mithrandir", "the bright lord", 1, 1))
            .expectNext(
                RelationDto(
                    NodeSubDto("4", "mithrandir", TYPE.REDIRECT, isTopParent = true),
                    NodeSubDto("3", "gandalf", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("3", "gandalf", TYPE.PAGE),
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT, isBottomChild = true),
                ),
            ).expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathsByTitle with same input and target should return empty and not call db`() {
        StepVerifier
            .create(service.shortestPathsByTitle("wizard", "wizard", 0, 1))
            .expectNextCount(0)
            .verifyComplete()
        verify(pageRepository, times(0))
            .shortestPaths(anyString(), anyString(), anyInt(), anyInt())
        verify(redirectRepository, times(0))
            .shortestPaths(anyString(), anyString(), anyInt(), anyInt())
    }

    @Test
    fun `shortestPathsByTitle with non existing target should return empty and not throw NoSuchElementException`() {
        StepVerifier
            .create(service.shortestPathsByTitle("wizard", "toto", 0, 1))
            .expectNextCount(0)
            .verifyComplete()
        verify(pageRepository, times(1))
            .shortestPaths(anyString(), anyString(), anyInt(), anyInt())
        verify(redirectRepository, times(0))
            .shortestPaths(anyString(), anyString(), anyInt(), anyInt())
    }

    @Test
    fun `shortestPathsByTitle with non existing source should return empty and not throw any Exception`() {
        StepVerifier
            .create(service.shortestPathsByTitle("zebi", "celebrimbor", 0, -1))
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `allShortestPathsByTitle between 2 pages should return all paths`() {
        StepVerifier
            .create(service.allShortestPathsByTitle("wizard", "celebrimbor"))
            .expectNext(
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE, isTopParent = true),
                    NodeSubDto("12", "good", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("12", "good", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT),
                ),
                RelationDto(
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT),
                    NodeSubDto("9", "celebrimbor", TYPE.PAGE, isBottomChild = true),
                ),
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE, isTopParent = true),
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT),
                ),
            ).expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `allShortestPathsByTitle between 2 redirects should return all paths`() {
        StepVerifier
            .create(service.allShortestPathsByTitle("mithrandir", "the bright lord"))
            .expectNext(
                RelationDto(
                    NodeSubDto("4", "mithrandir", TYPE.REDIRECT, isTopParent = true),
                    NodeSubDto("3", "gandalf", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("3", "gandalf", TYPE.PAGE),
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                    NodeSubDto("12", "good", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("12", "good", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT, isBottomChild = true),
                ),
                RelationDto(
                    NodeSubDto("11", "wizard", TYPE.PAGE),
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                ),
                RelationDto(
                    NodeSubDto("15", "wisdom", TYPE.PAGE),
                    NodeSubDto("27", "the bright lord", TYPE.REDIRECT, isBottomChild = true),
                ),
            ).expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `allShortestPathsByTitle with same input and target should return empty and not call db`() {
        StepVerifier
            .create(service.allShortestPathsByTitle("wizard", "wizard"))
            .expectNextCount(0)
            .verifyComplete()
        verify(pageRepository, times(0))
            .shortestPaths(anyString(), anyString())
        verify(redirectRepository, times(0))
            .shortestPaths(anyString(), anyString())
    }

    @Test
    fun `allShortestPathsByTitle with non existing target should return empty and not throw NoSuchElementException`() {
        StepVerifier
            .create(service.allShortestPathsByTitle("wizard", "toto"))
            .expectNextCount(0)
            .verifyComplete()
        verify(pageRepository, times(1))
            .shortestPaths(anyString(), anyString())
        verify(redirectRepository, times(0))
            .shortestPaths(anyString(), anyString())
    }

    @Test
    fun `allShortestPathsByTitle with non existing source should return empty and not throw any Exception`() {
        StepVerifier
            .create(service.allShortestPathsByTitle("zebi", "celebrimbor"))
            .expectNextCount(0)
            .verifyComplete()
    }
}
