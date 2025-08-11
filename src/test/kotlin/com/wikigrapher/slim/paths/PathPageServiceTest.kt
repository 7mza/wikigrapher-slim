package com.wikigrapher.slim.paths

import com.wikigrapher.slim.Commons
import com.wikigrapher.slim.DELETE_ALL_QUERY
import com.wikigrapher.slim.ReactiveFileReader
import com.wikigrapher.slim.RelationDto
import com.wikigrapher.slim.TYPE
import com.wikigrapher.slim.TestContainersFactory
import com.wikigrapher.slim.TestDatabaseHelper
import com.wikigrapher.slim.pages.IPageRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("default", "neo4j")
@Testcontainers
class PathPageServiceTest {
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

    @MockitoSpyBean
    private lateinit var repo: IPageRepository

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
    fun getSourceType() {
        StepVerifier
            .create(service.getSourceType("gandalf"))
            .expectNext(TYPE.PAGE)
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
    fun shortestPathLength() {
        StepVerifier
            .create(service.shortestPathLength("morgoth", "gandalf"))
            .expectNext(3)
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathLength with same input and target should return 0 and not call db`() {
        StepVerifier
            .create(service.shortestPathLength("gandalf", "gandalf"))
            .expectNext(0)
            .expectNextCount(0)
            .verifyComplete()
        verify(repo, times(0))
            .shortestPathLength(anyString(), anyString())
    }

    @Test
    fun `shortestPathLength with non existing source should return 0 and not throw any Exception`() {
        StepVerifier
            .create(service.shortestPathLength("zebi", "gandalf"))
            .expectNext(0)
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathLength with no path`() {
        StepVerifier
            .create(service.shortestPathLength("wizard", "gandalf"))
            .expectNext(0)
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathByTitle should return 1 shortest path`() {
        StepVerifier
            .create(
                service
                    .shortestPathByTitle("gandalf", "celebrimbor")
                    .collectList()
                    .flatMapMany {
                        Flux.fromIterable(Commons.reconstructPathsFromRelations(it))
                    },
            ).recordWith { ArrayList() }
            .expectNextCount(1)
            .consumeRecordedWith { path ->
                path.forEach {
                    assertThat(it.size).isEqualTo(4)

                    val first = it.first()
                    assertThat(first.source.title).isEqualTo("gandalf")
                    assertThat(first.source.type).isEqualTo(TYPE.PAGE)
                    assertThat(first.source.isTopParent).isTrue
                    assertThat(first.source.isBottomChild).isNull()

                    val last = it.last()
                    assertThat(last.target.title).isEqualTo("celebrimbor")
                    assertThat(last.target.type).isEqualTo(TYPE.PAGE)
                    assertThat(last.target.isTopParent).isNull()
                    assertThat(last.target.isBottomChild).isTrue

                    assertThat(Commons.isPathConnected(it))
                }
            }.expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathByTitle with same input and target should return empty and not call db`() {
        StepVerifier
            .create(service.shortestPathByTitle("gandalf", "gandalf"))
            .expectNextCount(0)
            .verifyComplete()
        verify(repo, times(0))
            .shortestPath(anyString(), anyString())
    }

    @Test
    fun `shortestPathByTitle with non existing target should return empty and not throw NoSuchElementException`() {
        StepVerifier
            .create(service.shortestPathByTitle("gandalf", "toto"))
            .expectNextCount(0)
            .verifyComplete()
        verify(repo, times(1))
            .shortestPath(anyString(), anyString())
    }

    @Test
    fun `shortestPathByTitle with non existing source should return empty and not throw any Exception`() {
        StepVerifier
            .create(service.shortestPathByTitle("zebi", "gandalf"))
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathByTitle with no path`() {
        StepVerifier
            .create(service.shortestPathByTitle("wizard", "gandalf"))
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun getRandomShortestPath() {
        // FIXME: data too small, fire many getRandomShortestPath and expect at least one path found
    }

    @Test
    fun `shortestPathsByTitle should return all shortest paths if unlimited`() {
        StepVerifier
            .create(
                service
                    .shortestPathsByTitle("gandalf", "celebrimbor", 0, 100)
                    .collectList()
                    .flatMapMany {
                        Flux.fromIterable(Commons.reconstructPathsFromRelations(it))
                    },
            ).recordWith { ArrayList() }
            .expectNextCount(3)
            .consumeRecordedWith { path ->
                path.forEach {
                    assertThat(it.size).isEqualTo(4)

                    val first = it.first()
                    assertThat(first.source.title).isEqualTo("gandalf")
                    assertThat(first.source.type).isEqualTo(TYPE.PAGE)
                    assertThat(first.source.isTopParent).isTrue
                    assertThat(first.source.isBottomChild).isNull()

                    val last = it.last()
                    assertThat(last.target.title).isEqualTo("celebrimbor")
                    assertThat(last.target.type).isEqualTo(TYPE.PAGE)
                    assertThat(last.target.isTopParent).isNull()
                    assertThat(last.target.isBottomChild).isTrue

                    assertThat(Commons.isPathConnected(it))
                }
            }.expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathsByTitle should respect skip`() {
        var firstPath: List<RelationDto>? = null
        var secondPath: List<RelationDto>? = null

        StepVerifier
            .create(
                service
                    .shortestPathsByTitle("gandalf", "celebrimbor", 0, 1)
                    .collectList()
                    .flatMapMany { Flux.fromIterable(Commons.reconstructPathsFromRelations(it)) },
            ).expectNextMatches {
                firstPath = it
                true
            }.verifyComplete()

        StepVerifier
            .create(
                service
                    .shortestPathsByTitle("wizard", "celebrimbor", 1, 1)
                    .collectList()
                    .flatMapMany { Flux.fromIterable(Commons.reconstructPathsFromRelations(it)) },
            ).expectNextMatches {
                secondPath = it
                true
            }.verifyComplete()

        assertThat(firstPath).isNotNull
        assertThat(Commons.isPathConnected(firstPath!!.toList()))

        assertThat(secondPath).isNotNull
        assertThat(Commons.isPathConnected(secondPath!!.toList()))

        assertThat(firstPath).isNotEqualTo(secondPath)
    }

    @Test
    fun `shortestPathsByTitle should respect limit`() {
        StepVerifier
            .create(
                service
                    .shortestPathsByTitle("wizard", "celebrimbor", 0, 1)
                    .collectList()
                    .flatMapMany {
                        Flux.fromIterable(Commons.reconstructPathsFromRelations(it))
                    },
            ).recordWith { ArrayList() }
            .expectNextCount(1)
            .consumeRecordedWith { path ->
                path.forEach {
                    val first = it.first()
                    assertThat(first.source.title).isEqualTo("wizard")
                    assertThat(first.source.type).isEqualTo(TYPE.PAGE)
                    assertThat(first.source.isTopParent).isTrue
                    assertThat(first.source.isBottomChild).isNull()

                    val last = it.last()
                    assertThat(last.target.title).isEqualTo("celebrimbor")
                    assertThat(last.target.type).isEqualTo(TYPE.PAGE)
                    assertThat(last.target.isTopParent).isNull()
                    assertThat(last.target.isBottomChild).isTrue

                    assertThat(Commons.isPathConnected(it))
                }
            }.expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathsByTitle with same input and target should return empty and not call db`() {
        StepVerifier
            .create(service.shortestPathsByTitle("wizard", "wizard", 0, 1))
            .expectNextCount(0)
            .verifyComplete()
        verify(repo, times(0))
            .shortestPaths(anyString(), anyString(), anyInt(), anyInt())
    }

    @Test
    fun `shortestPathsByTitle with non existing target should return empty and not throw NoSuchElementException`() {
        StepVerifier
            .create(service.shortestPathsByTitle("wizard", "toto", 0, 1))
            .expectNextCount(0)
            .verifyComplete()
        verify(repo, times(1))
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
    fun `shortestPathsByTitle with invalid skip and limit should return empty and not throw any Exception`() {
        StepVerifier
            .create(service.shortestPathsByTitle("wizard", "gandalf", -1, -1))
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `shortestPathsByTitle with no path`() {
        StepVerifier
            .create(service.shortestPathsByTitle("wizard", "gandalf", 0, 100))
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `allShortestPathsByTitle should return all paths`() {
        StepVerifier
            .create(
                service
                    .allShortestPathsByTitle("gandalf", "celebrimbor")
                    .collectList()
                    .flatMapMany {
                        Flux.fromIterable(Commons.reconstructPathsFromRelations(it))
                    },
            ).recordWith { ArrayList() }
            .expectNextCount(3)
            .consumeRecordedWith { path ->
                path.forEach {
                    assertThat(it.size).isEqualTo(4)

                    val first = it.first()
                    assertThat(first.source.title).isEqualTo("gandalf")
                    assertThat(first.source.type).isEqualTo(TYPE.PAGE)
                    assertThat(first.source.isTopParent).isTrue
                    assertThat(first.source.isBottomChild).isNull()

                    val last = it.last()
                    assertThat(last.target.title).isEqualTo("celebrimbor")
                    assertThat(last.target.type).isEqualTo(TYPE.PAGE)
                    assertThat(last.target.isTopParent).isNull()
                    assertThat(last.target.isBottomChild).isTrue

                    assertThat(Commons.isPathConnected(it))
                }
            }.expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `allShortestPathsByTitle with same input and target should return empty and not call db`() {
        StepVerifier
            .create(service.allShortestPathsByTitle("wizard", "wizard"))
            .expectNextCount(0)
            .verifyComplete()
        verify(repo, times(0))
            .shortestPaths(anyString(), anyString())
    }

    @Test
    fun `allShortestPathsByTitle with non existing target should return empty and not throw NoSuchElementException`() {
        StepVerifier
            .create(service.allShortestPathsByTitle("wizard", "toto"))
            .expectNextCount(0)
            .verifyComplete()
        verify(repo, times(1))
            .shortestPaths(anyString(), anyString())
    }

    @Test
    fun `allShortestPathsByTitle with non existing source should return empty and not throw any Exception`() {
        StepVerifier
            .create(service.allShortestPathsByTitle("zebi", "celebrimbor"))
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `allShortestPathsByTitle with no path`() {
        StepVerifier
            .create(service.allShortestPathsByTitle("wizard", "gandalf"))
            .expectNextCount(0)
            .verifyComplete()
    }
}
