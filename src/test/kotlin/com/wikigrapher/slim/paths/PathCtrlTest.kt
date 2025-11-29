package com.wikigrapher.slim.paths

import com.wikigrapher.slim.NodeSubDto
import com.wikigrapher.slim.RelationDto
import com.wikigrapher.slim.TYPE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@WebFluxTest(controllers = [PathCtrl::class])
class PathCtrlTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var service: IPathService

    private val path =
        listOf(
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
        )

    @BeforeEach
    fun beforeEach() {
        whenever(service.shortestPathLength(anyString(), anyString()))
            .thenReturn(Mono.just(3))
        whenever(service.shortestPathByTitle(anyString(), anyString()))
            .thenReturn(Flux.fromIterable(path))
        whenever(service.shortestPathsByTitle(anyString(), anyString(), anyInt(), anyInt()))
            .thenReturn(Flux.fromIterable(path))
        whenever(service.allShortestPathsByTitle(anyString(), anyString()))
            .thenReturn(Flux.fromIterable(path))
        whenever(service.getRandomShortestPath())
            .thenReturn(Flux.fromIterable(path))
    }

    @Test
    fun shortestPathLength() {
        val response: Int? =
            webTestClient
                .get()
                .uri("/api/core/path/length?sourceTitle=morgoth&targetTitle=gandalf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody(Int::class.java)
                .returnResult()
                .responseBody
        assertThat(response).isEqualTo(3)
    }

    @Test
    fun shortestPathByTitle() {
        val response: List<RelationDto>? =
            webTestClient
                .get()
                .uri("/api/core/path?sourceTitle=morgoth&targetTitle=gandalf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk
                .expectBodyList(RelationDto::class.java)
                .returnResult()
                .responseBody
        assertThat(response).isEqualTo(path)
    }

    @Test
    fun getRandomShortestPath() {
        val response: List<RelationDto>? =
            webTestClient
                .get()
                .uri("/api/core/path/random?sourceTitle=morgoth&targetTitle=gandalf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk
                .expectBodyList(RelationDto::class.java)
                .returnResult()
                .responseBody
        assertThat(response).isEqualTo(path)
    }

    @Test
    fun shortestPathsByTitle() {
        val response: List<RelationDto>? =
            webTestClient
                .get()
                .uri("/api/core/paths?sourceTitle=morgoth&targetTitle=gandalf&skip=0&limit=2")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk
                .expectBodyList(RelationDto::class.java)
                .returnResult()
                .responseBody
        assertThat(response).isEqualTo(path)
    }

    @Test
    fun allShortestPathsByTitle() {
        val response: List<RelationDto>? =
            webTestClient
                .get()
                .uri("/api/core/paths/all?sourceTitle=morgoth&targetTitle=gandalf")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk
                .expectBodyList(RelationDto::class.java)
                .returnResult()
                .responseBody
        assertThat(response).isEqualTo(path)
    }
}
