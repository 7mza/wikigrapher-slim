package com.wikigrapher.slim.wiki

import com.wikigrapher.slim.SearchSuggestionsDto
import com.wikigrapher.slim.ThumbnailDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

interface IWikiService {
    fun getWikipediaPageImage(
        title: String,
        piThumbSize: Int?,
    ): Mono<ThumbnailDto>

    fun getWikipediaPageTitle(title: String): Mono<SearchSuggestionsDto>
}

@Service
class WikiService
    @Autowired
    constructor(
        private val client: IWikiClient,
    ) : IWikiService {
        override fun getWikipediaPageImage(
            title: String,
            piThumbSize: Int?,
        ): Mono<ThumbnailDto> =
            client
                .getWikipediaPageImage(title, piThumbSize)
                .map { dto ->
                    dto.query
                        ?.pages
                        ?.values
                        ?.firstOrNull { it?.thumbnail != null }
                        ?.thumbnail ?: ThumbnailDto()
                }.onErrorResume {
                    Mono.empty()
                }

        override fun getWikipediaPageTitle(title: String): Mono<SearchSuggestionsDto> =
            client.getWikipediaPageTitle(title).onErrorResume {
                Mono.empty()
            }
    }
