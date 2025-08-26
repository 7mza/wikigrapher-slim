package com.wikigrapher.slim.pages

import com.wikigrapher.slim.CategorySubDto
import com.wikigrapher.slim.NodeDto
import com.wikigrapher.slim.NodeSubDto
import com.wikigrapher.slim.TYPE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux

@WebFluxTest(controllers = [PageCtrl::class])
class PageCtrlTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var service: IPageService

    private val page =
        NodeDto(
            id = "3",
            title = "gandalf",
            type = TYPE.PAGE,
            outgoing =
                setOf(
                    NodeSubDto(
                        id = "11",
                        title = "wizard",
                        type = TYPE.PAGE,
                    ),
                ),
            incoming =
                setOf(
                    NodeSubDto(id = "4", title = "mithrandir", type = TYPE.REDIRECT),
                    NodeSubDto(id = "5", title = "the grey wizard", type = TYPE.REDIRECT),
                ),
            categories = setOf(CategorySubDto(id = "1", title = "wizards")),
        )

    @BeforeEach
    fun beforeEach() {
        whenever(service.getNRandomPages(anyInt()))
            .thenReturn(Flux.fromIterable(listOf(page.toSubDto(), page.toSubDto())))
    }

    @Test
    fun getNRandomPages() {
        val response: List<NodeDto>? =
            webTestClient
                .get()
                .uri("/api/core/page/random?n=2")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk
                .expectBodyList(NodeDto::class.java)
                .returnResult()
                .responseBody
        assertThat(response?.size).isEqualTo(2)
    }
}
