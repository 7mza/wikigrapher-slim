package com.wikigrapher.slim

import com.wikigrapher.slim.meta.IMetaService
import org.assertj.core.api.Assertions.assertThat
import org.htmlunit.WebClient
import org.htmlunit.html.HtmlLink
import org.htmlunit.html.HtmlPage
import org.htmlunit.html.HtmlScript
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebCtrlTest {
    private val htmlClient =
        WebClient().apply {
            options.isJavaScriptEnabled = true
            options.isRedirectEnabled = false
            options.isThrowExceptionOnFailingStatusCode = true
            options.isThrowExceptionOnScriptError = false
        }

    @Autowired
    private lateinit var assetManifestReader: ReactiveAssetManifestReader

    @LocalServerPort
    private lateinit var port: String

    @MockitoBean
    private lateinit var metaService: IMetaService

    @BeforeEach
    fun beforeEach() {
        whenever(metaService.findDumpMeta())
            .thenReturn(
                Mono.just(
                    DumpMetaDto(
                        lang = "en",
                        date = "11111111",
                        url = "https://dumps.wikimedia.org/enwiki/11111111",
                        nodes = DumpNodes(pages = 9, redirects = 7, categories = 3, orphans = 3),
                        relations = DumpRelations(linkTo = 11, redirectTo = 7, belongTo = 8),
                    ),
                ),
            )
    }

    @AfterEach
    fun afterEach() {
        htmlClient.cookieManager.clearCookies()
    }

    @Test
    fun index() {
        val page: HtmlPage = htmlClient.getPage("http://localhost:$port")
        assertThat(page.webResponse.statusCode).isEqualTo(HttpStatus.OK.value())
    }

    @Test
    fun `index with missing findDumpMeta`() {
        whenever(metaService.findDumpMeta()).thenReturn(Mono.empty())
        val page: HtmlPage = htmlClient.getPage("http://localhost:$port")
        assertThat(page.webResponse.statusCode).isEqualTo(HttpStatus.OK.value())
    }

    @Test
    fun `assetManifestReader init`() {
        StepVerifier
            .create(assetManifestReader.getAll())
            .assertNext { map ->
                // assertThat(map).hasSize(6)
                assertThat(map.keys).contains(
                    "bootstrap-icons.woff",
                    "bootstrap-icons.woff2",
                    "paths.js",
                    "shared.css",
                    "shared.js",
                    "vendor.js",
                )
            }.verifyComplete()
    }

    @Test
    fun `static assets are populated from manifest`() {
        val page: HtmlPage = htmlClient.getPage("http://localhost:$port/")
        assertThat(page.webResponse.statusCode).isEqualTo(HttpStatus.OK.value())
        assertThat(page.url.path).isEqualTo("/")

        val scriptTags = page.getByXPath<HtmlScript>("//script[not(@type) or @type='text/javascript']")
        val srcs = scriptTags.map { it.srcAttribute }
        assertThat(srcs)
            .containsExactlyInAnyOrder(
                assetManifestReader.get("paths.js").block(),
                assetManifestReader.get("shared.js").block(),
                assetManifestReader.get("vendor.js").block(),
            )

        val stylesheetLinks = page.getByXPath<HtmlLink>("//link[@rel='stylesheet']")
        val hrefs = stylesheetLinks.map { it.hrefAttribute }
        assertThat(hrefs).containsExactlyInAnyOrder(
            assetManifestReader.get("shared.css").block(),
        )
    }
}
