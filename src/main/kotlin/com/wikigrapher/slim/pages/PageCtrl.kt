package com.wikigrapher.slim.pages

import com.wikigrapher.slim.NodeDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class PageCtrl
    @Autowired
    constructor(
        private val service: IPageService,
    ) : IPageApi {
        private val logger = LoggerFactory.getLogger(this::class.java)

        override fun getNRandomPages(n: Int?): Flux<NodeDto> {
            logger.debug("getNRandomPages, n: {}", n)
            return service.getNRandomPages(n!!)
        }
    }
