package com.wikigrapher.slim.wiki

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.Fault
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
    properties = [$$"clients.wikipedia.port=${wiremock.server.port}"],
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
    fun `getWikipediaPageImage onError shouldn't throw exception`() {
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
}
