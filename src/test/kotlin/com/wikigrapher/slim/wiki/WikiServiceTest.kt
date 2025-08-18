package com.wikigrapher.slim.wiki

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.Fault
import com.wikigrapher.slim.SearchSuggestion
import com.wikigrapher.slim.SearchSuggestionsDto
import com.wikigrapher.slim.ThumbnailDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import reactor.test.StepVerifier

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        $$"clients.wikipedia-api.port=${wiremock.server.port}",
        $$"clients.wikipedia-web.port=${wiremock.server.port}",
    ],
)
@AutoConfigureWireMock(port = 0)
class WikiServiceTest {
    @Autowired
    private lateinit var service: IWikiService

    @AfterEach
    fun afterEach() {
        WireMock.reset()
    }

    @Test
    fun getWikipediaPageImage() {
        WireMock.stubFor(
            WireMock
                .get(
                    WireMock.urlEqualTo(
                        "/w/api.php?action=query&titles=Oreo&prop=pageimages&format=json&pithumbsize=200",
                    ),
                ).withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
// @formatter:off
"""
{
  "batchcomplete": "",
  "query": {
    "pages": {
      "241559": {
        "pageid": 241559,
        "ns": 0,
        "title": "Oreo",
        "thumbnail": {
          "source": "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1b/Oreo-Two-Cookies.png/250px-Oreo-Two-Cookies.png",
          "width": 200,
          "height": 122
        },
        "pageimage": "Oreo-Two-Cookies.png"
      }
    }
  }
}
""".trimIndent(),
// @formatter:on
                        ),
                ),
        )

        StepVerifier
            .create(service.getWikipediaPageImage(title = "Oreo", piThumbSize = 200))
            .expectNextMatches {
                assertThat(it).isEqualTo(
                    ThumbnailDto(
                        source =
                            "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1b/Oreo-Two-Cookies.png/250px-Oreo-Two-Cookies.png",
                        width = 200,
                        height = 122,
                    ),
                )
                true
            }.expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `getWikipediaPageImage onError should return empty and not throw exception`() {
        WireMock.stubFor(
            WireMock
                .get(
                    WireMock.urlEqualTo(
                        "/w/api.php?action=query&titles=Mars&prop=pageimages&format=json&pithumbsize=200",
                    ),
                ).withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(WireMock.aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)),
        )

        StepVerifier
            .create(service.getWikipediaPageImage(title = "Mars", piThumbSize = 200))
            .expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun getWikipediaPageTitle() {
        WireMock.stubFor(
            WireMock
                .get(
                    WireMock.urlEqualTo(
                        "/core/v1/wikipedia/en/search/title?q=Oreo&limit=5",
                    ),
                ).withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
// @formatter:off
"""
{
  "pages": [
    {
      "id": 241559,
      "key": "Oreo",
      "title": "Oreo",
      "excerpt": "Oreo",
      "matched_title": null,
      "anchor": null,
      "description": "Chocolate cookie with creme filling made by Nabisco",
      "thumbnail": {
        "mimetype": "image/png",
        "width": 60,
        "height": 37,
        "duration": null,
        "url": "//upload.wikimedia.org/wikipedia/commons/thumb/1/1b/Oreo-Two-Cookies.png/60px-Oreo-Two-Cookies.png"
      }
    },
    {
      "id": 2075544,
      "key": "Oreo_O's",
      "title": "Oreo O's",
      "excerpt": "Oreo O's",
      "matched_title": null,
      "anchor": null,
      "description": "Breakfast cereal made by Post",
      "thumbnail": {
        "mimetype": "image/png",
        "width": 60,
        "height": 34,
        "duration": null,
        "url": "//upload.wikimedia.org/wikipedia/commons/thumb/b/bc/Oreo_O%27s_logo.png/60px-Oreo_O%27s_logo.png"
      }
    }
  ]
}
""".trimIndent(),
// @formatter:on
                        ),
                ),
        )

        StepVerifier
            .create(service.getWikipediaPageTitle("Oreo"))
            .expectNextMatches {
                assertThat(it).isInstanceOf(SearchSuggestionsDto::class.java)
                assertThat(it.pages).hasSameElementsAs(
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
                        SearchSuggestion(
                            id = "2075544",
                            key = "Oreo_O's",
                            title = "Oreo O's",
                            description = "Breakfast cereal made by Post",
                            thumbnail =
                                ThumbnailDto(
                                    source =
                                        "//upload.wikimedia.org/wikipedia/commons/thumb/b/bc/Oreo_O%27s_logo.png/60px-Oreo_O%27s_logo.png",
                                    width = 60,
                                    height = 34,
                                ),
                        ),
                    ),
                )
                true
            }.expectNextCount(0)
            .verifyComplete()
    }

    @Test
    fun `getWikipediaPageTitle onError should return empty and not throw exception`() {
        WireMock.stubFor(
            WireMock
                .get(
                    WireMock.urlEqualTo(
                        "/core/v1/wikipedia/en/search/title?q=Oreo&limit=5",
                    ),
                ).withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(WireMock.aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)),
        )

        StepVerifier
            .create(service.getWikipediaPageTitle("Oreo"))
            .expectNextCount(0)
            .verifyComplete()
    }
}
