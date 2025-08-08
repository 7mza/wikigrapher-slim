package com.wikigrapher.slim.wiki

import com.wikigrapher.slim.WikipediaPageImageDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

interface IWikiClient {
    fun getWikipediaPageImage(
        title: String,
        piThumbSize: Int? = 200,
    ): Mono<WikipediaPageImageDto>
}

@Service
class WikiClient
    @Autowired
    constructor(
        private val webClient: WebClient,
    ) : IWikiClient {
        override fun getWikipediaPageImage(
            title: String,
            piThumbSize: Int?,
        ): Mono<WikipediaPageImageDto> =
            webClient
                .get()
                .uri {
                    it
                        .path("/w/api.php")
                        .queryParam("action", "query")
                        .queryParam("titles", "{title}")
                        .queryParam("prop", "pageimages")
                        .queryParam("format", "json")
                        .queryParam("pithumbsize", "{piThumbSize}")
                        .build(title, piThumbSize)
                }.retrieve()
                .bodyToMono(WikipediaPageImageDto::class.java)
    }
