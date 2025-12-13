package com.wikigrapher.slim.paths

import com.wikigrapher.slim.RelationDto
import com.wikigrapher.slim.TYPE
import com.wikigrapher.slim.pages.IPageRepository
import com.wikigrapher.slim.pages.IPageService
import com.wikigrapher.slim.pages.TmpNode
import com.wikigrapher.slim.redirects.IRedirectRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IPathService {
    fun getSourceType(sourceTitle: String): Mono<TYPE>

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
        private val redirectRepository: IRedirectRepository,
    ) : IPathService {
        private val logger = LoggerFactory.getLogger(this::class.java)

        override fun getSourceType(sourceTitle: String): Mono<TYPE> =
            Mono
                .zip(
                    pageRepository.existsByTitle(sourceTitle),
                    redirectRepository.existsByTitle(sourceTitle),
                ).flatMap {
                    when {
                        it.t1 -> {
                            Mono.just(TYPE.PAGE)
                        }

                        it.t2 -> {
                            Mono.just(TYPE.REDIRECT)
                        }

                        else -> {
                            logger.error("getSourceType, node {} not found", sourceTitle)
                            Mono.empty()
                        }
                    }
                }

        override fun shortestPathLength(
            sourceTitle: String,
            targetTitle: String,
        ): Mono<Int> =
            if (sourceTitle.equals(targetTitle, true)) {
                Mono.just(0)
            } else {
                getSourceType(sourceTitle)
                    .flatMap {
                        when (it) {
                            TYPE.PAGE -> pageRepository.shortestPathLength(sourceTitle, targetTitle)
                            TYPE.REDIRECT -> redirectRepository.shortestPathLength(sourceTitle, targetTitle)
                        }
                    }.switchIfEmpty(Mono.just(0))
            }

        override fun shortestPathByTitle(
            sourceTitle: String,
            targetTitle: String,
        ): Flux<RelationDto> =
            if (sourceTitle.equals(targetTitle, true)) {
                Flux.empty()
            } else {
                getSourceType(sourceTitle)
                    .flatMapMany { type ->
                        when (type) {
                            TYPE.PAGE -> {
                                pageRepository.shortestPath(sourceTitle, targetTitle).map { it.toNode() }
                            }

                            TYPE.REDIRECT -> {
                                redirectRepository
                                    .shortestPath(
                                        sourceTitle,
                                        targetTitle,
                                    ).map { it.toNode() }
                            }
                        }.flatMapMany { dfsFlatten(it, sourceTitle, targetTitle) }
                    }.switchIfEmpty(Flux.empty())
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
                getSourceType(sourceTitle)
                    .flatMapMany { type ->
                        when (type) {
                            TYPE.PAGE -> {
                                pageRepository
                                    .shortestPaths(
                                        sourceTitle,
                                        targetTitle,
                                        skip.takeIf { it >= 0 } ?: 0,
                                        limit.takeIf { it >= 0 } ?: 0,
                                    ).map { it.toNode() }
                            }

                            TYPE.REDIRECT -> {
                                redirectRepository
                                    .shortestPaths(
                                        sourceTitle,
                                        targetTitle,
                                        skip.takeIf { it >= 0 } ?: 0,
                                        limit.takeIf { it >= 0 } ?: 0,
                                    ).map { it.toNode() }
                            }
                        }.flatMapMany { dfsFlatten(it, sourceTitle, targetTitle) }
                            .onErrorResume(NoSuchElementException::class.java) {
                                logger.error(
                                    "shortestPathsByTitle, no paths found between '{}' and '{}'",
                                    sourceTitle,
                                    targetTitle,
                                )
                                Flux.empty()
                            }
                    }.switchIfEmpty(Flux.empty())
            }

        override fun allShortestPathsByTitle(
            sourceTitle: String,
            targetTitle: String,
        ): Flux<RelationDto> =
            if (sourceTitle.equals(targetTitle, true)) {
                Flux.empty()
            } else {
                getSourceType(sourceTitle)
                    .flatMapMany { type ->
                        when (type) {
                            TYPE.PAGE -> {
                                pageRepository.shortestPaths(sourceTitle, targetTitle).map { it.toNode() }
                            }

                            TYPE.REDIRECT -> {
                                redirectRepository
                                    .shortestPaths(
                                        sourceTitle,
                                        targetTitle,
                                    ).map { it.toNode() }
                            }
                        }.flatMapMany { dfsFlatten(it, sourceTitle, targetTitle) }
                            .onErrorResume(NoSuchElementException::class.java) {
                                logger.error(
                                    "allShortestPathsByTitle, no paths found between '{}' and '{}'",
                                    sourceTitle,
                                    targetTitle,
                                )
                                Flux.empty()
                            }
                    }.switchIfEmpty(Flux.empty())
            }

        private fun dfsFlatten(
            root: TmpNode,
            sourceTitle: String,
            targetTitle: String,
        ): Flux<RelationDto> =
            Flux
                .create { emitter ->
                    fun dfs(
                        node: TmpNode,
                        parent: TmpNode?,
                    ) {
                        parent?.let {
                            val source =
                                it.toSubDto(
                                    isTopParent = it.title.equals(sourceTitle, true).takeIf { eq -> eq },
                                )
                            val target =
                                node.toSubDto(
                                    isBottomChild = node.title.equals(targetTitle, true).takeIf { eq -> eq },
                                )
                            emitter.next(
                                RelationDto(
                                    source = source,
                                    target = target,
                                ),
                            )
                        }
                        node.outgoing?.forEach {
                            dfs(it, node)
                        }
                    }
                    dfs(root, null)
                    emitter.complete()
                }.distinct()
    }
