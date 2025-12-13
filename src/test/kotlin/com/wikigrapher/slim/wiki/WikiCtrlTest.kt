package com.wikigrapher.slim.wiki

import com.wikigrapher.slim.SearchSuggestion
import com.wikigrapher.slim.SearchSuggestionsDto
import com.wikigrapher.slim.ThumbnailDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class WikiCtrlTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var service: IWikiService

    private val thumbnail =
        ThumbnailDto(
            source =
                "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1b/Oreo-Two-Cookies.png/250px-Oreo-Two-Cookies.png",
            width = 200,
            height = 122,
        )

    private val pages =
        SearchSuggestionsDto(
            pages =
                setOf(
                    SearchSuggestion(
                        id = "241559",
                        key = "Oreo",
                        title = "Oreo",
                        description = "Chocolate cookie with creme filling made by Nabisco",
                        thumbnail =
                            ThumbnailDto(
                                source =
                                    "//upload.wikimedia.org/wikipedia/commons/thumb/1/1b/Oreo-Two-Cookies.png/60px-Oreo-Two-Cookies.png",
                                width = 60,
                                height = 37,
                            ),
                    ),
                ),
        )

    @BeforeEach
    fun beforeEach() {
        whenever(service.getWikipediaPageImage(anyString(), anyInt()))
            .thenReturn(Mono.just(thumbnail))
        whenever(service.getWikipediaPageTitle(anyString()))
            .thenReturn(Mono.just(pages))
    }

    @Test
    fun getWikipediaPageImage() {
        val response: ThumbnailDto? =
            webTestClient
                .get()
                .uri("/api/wiki/image?title=Oreo&piThumbSize=100")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<ThumbnailDto>()
                .returnResult()
                .responseBody
        assertThat(response).isEqualTo(thumbnail)
    }

    @Test
    fun getWikipediaPageTitle() {
        val response: SearchSuggestionsDto? =
            webTestClient
                .get()
                .uri("/api/wiki/title?title=Oreo")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody<SearchSuggestionsDto>()
                .returnResult()
                .responseBody
        assertThat(response).isEqualTo(pages)
    }
}
