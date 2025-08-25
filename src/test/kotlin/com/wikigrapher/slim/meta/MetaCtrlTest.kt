package com.wikigrapher.slim.meta

import com.wikigrapher.slim.DumpMetaDto
import com.wikigrapher.slim.DumpNodes
import com.wikigrapher.slim.DumpRelations
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest(controllers = [MetaCtrl::class])
class MetaCtrlTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var service: IMetaService

    private val meta =
        DumpMetaDto(
            lang = "en",
            date = "11111111",
            url = "https://dumps.wikimedia.org/enwiki/11111111",
            nodes = DumpNodes(pages = 9, redirects = 7, categories = 3, orphans = 3),
            relations = DumpRelations(linkTo = 11, redirectTo = 7, belongTo = 8),
        )

    @BeforeEach
    fun beforeEach() {
        whenever(service.findDumpMeta()).thenReturn(Mono.just(meta))
    }

    @Test
    fun findDumpMeta() {
        val response: DumpMetaDto? =
            webTestClient
                .get()
                .uri("/api/core/meta/dump")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody(DumpMetaDto::class.java)
                .returnResult()
                .responseBody
        assertThat(response).isEqualTo(meta)
    }
}
