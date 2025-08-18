package com.wikigrapher.slim.wiki

import com.wikigrapher.slim.Commons
import com.wikigrapher.slim.SearchSuggestionsDto
import com.wikigrapher.slim.ThumbnailDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class WikiCtrl
    @Autowired
    constructor(
        private val service: IWikiService,
    ) : IWikiApi {
        private val logger = LoggerFactory.getLogger(this::class.java)

        override fun getWikipediaPageImage(
            title: String,
            piThumbSize: Int?,
        ): Mono<ThumbnailDto> {
            val escapedTitle = Commons.sanitizeInput(title)
            logger.debug("getWikipediaPageImage, title: {}", escapedTitle)
            return service.getWikipediaPageImage(escapedTitle, piThumbSize)
        }

        override fun getWikipediaPageTitle(title: String): Mono<SearchSuggestionsDto> =
            service.getWikipediaPageTitle(Commons.sanitizeInput(title))
    }
