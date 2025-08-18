package com.wikigrapher.slim.wiki

import com.wikigrapher.slim.SearchSuggestionsDto
import com.wikigrapher.slim.WikipediaPageImageDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

interface IWikiClient {
    fun getWikipediaPageImage(
        title: String,
        piThumbSize: Int? = 200,
    ): Mono<WikipediaPageImageDto>

    fun getWikipediaPageTitle(title: String): Mono<SearchSuggestionsDto>
}

@Service
class WikiClient
    @Autowired
    constructor(
        @param:Qualifier("wikipedia-web-client") private val webClient: WebClient,
        @param:Qualifier("wikipedia-api-client") private val apiClient: WebClient,
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

        override fun getWikipediaPageTitle(title: String): Mono<SearchSuggestionsDto> {
            val project = "wikipedia"
            val language = "en"
            val limit = 5
            return apiClient
                .get()
                .uri {
                    it
                        .path("/core/v1/{project}/{language}/search/title")
                        .queryParam("q", "{title}")
                        .queryParam("limit", "{limit}")
                        .build(project, language, title, limit)
                }.retrieve()
                .bodyToMono(SearchSuggestionsDto::class.java)
        }
    }
