package com.wikigrapher.slim.pages

import com.wikigrapher.slim.NodeDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

interface IPageService {
    fun getNRandomPages(n: Int): Flux<NodeDto>
}

@Service
class PageService
    @Autowired
    constructor(
        private val pageRepository: IPageRepository,
    ) : IPageService {
        override fun getNRandomPages(n: Int): Flux<NodeDto> = pageRepository.getNRandomPages(n).map { it.toDto() }
    }
