package com.wikigrapher.slim.paths

import com.wikigrapher.slim.RelationDto
import com.wikigrapher.slim.pages.IPageRepository
import com.wikigrapher.slim.pages.IPageService
import com.wikigrapher.slim.pages.TmpNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IPathService {
    fun shortestPathLength(
        sourceTitle: String,
        targetTitle: String,
    ): Mono<Int>

    fun shortestPathByTitle(
        sourceTitle: String,
        targetTitle: String,
    ): Flux<RelationDto>

    fun getRandomShortestPath(): Flux<RelationDto>

    fun shortestPathsByTitle(
        sourceTitle: String,
        targetTitle: String,
        skip: Int,
        limit: Int,
    ): Flux<RelationDto>

    fun allShortestPathsByTitle(
        sourceTitle: String,
        targetTitle: String,
    ): Flux<RelationDto>
}

@Service
class PathService
    @Autowired
    constructor(
        private val pageService: IPageService,
        private val pageRepository: IPageRepository,
    ) : IPathService {
        private val logger = LoggerFactory.getLogger(this::class.java)

        override fun shortestPathLength(
            sourceTitle: String,
            targetTitle: String,
        ): Mono<Int> =
            if (sourceTitle.equals(targetTitle, true)) {
                Mono.just(0)
            } else {
                pageRepository.shortestPathLength(sourceTitle, targetTitle)
            }

        override fun shortestPathByTitle(
            sourceTitle: String,
            targetTitle: String,
        ): Flux<RelationDto> =
            if (sourceTitle.equals(targetTitle, true)) {
                Flux.empty()
            } else {
                pageRepository
                    .shortestPath(sourceTitle, targetTitle)
                    .map { it.toNode() }
                    .flatMapMany(::dfsFlatten)
            }

        override fun getRandomShortestPath(): Flux<RelationDto> =
            pageService.getNRandomPages(2).collectList().flatMapMany {
                this.shortestPathByTitle(it.first().title, it[1].title)
            }

        override fun shortestPathsByTitle(
            sourceTitle: String,
            targetTitle: String,
            skip: Int,
            limit: Int,
        ): Flux<RelationDto> =
            if (sourceTitle.equals(targetTitle, true)) {
                Flux.empty()
            } else {
                pageRepository
                    .shortestPaths(
                        sourceTitle,
                        targetTitle,
                        skip,
                        limit,
                    ).map { it.toNode() }
                    .flatMapMany(::dfsFlatten)
                    .onErrorResume(NoSuchElementException::class.java) {
                        logger.error(
                            "shortestPathsByTitle, no paths found between '{}' and '{}'",
                            sourceTitle,
                            targetTitle,
                        )
                        Flux.empty()
                    }
            }

        override fun allShortestPathsByTitle(
            sourceTitle: String,
            targetTitle: String,
        ): Flux<RelationDto> =
            if (sourceTitle.equals(targetTitle, true)) {
                Flux.empty()
            } else {
                pageRepository
                    .shortestPaths(
                        sourceTitle,
                        targetTitle,
                    ).map { it.toNode() }
                    .flatMapMany(::dfsFlatten)
                    .onErrorResume(NoSuchElementException::class.java) {
                        logger.error(
                            "allShortestPathsByTitle, no paths found between '{}' and '{}'",
                            sourceTitle,
                            targetTitle,
                        )
                        Flux.empty()
                    }
            }

        private fun dfsFlatten(root: TmpNode): Flux<RelationDto> =
            Flux
                .create<RelationDto> { emitter ->
                    fun dfs(
                        node: TmpNode,
                        parent: TmpNode?,
                    ) {
                        parent?.let {
                            emitter.next(RelationDto(it.toSubDto(), node.toSubDto()))
                        }
                        node.outgoing?.forEach {
                            dfs(it, node)
                        }
                    }
                    dfs(root, null)
                    emitter.complete()
                }.distinct()
    }
