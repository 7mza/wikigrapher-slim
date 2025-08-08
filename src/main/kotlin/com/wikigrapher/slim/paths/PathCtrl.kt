package com.wikigrapher.slim.paths

import com.wikigrapher.slim.Commons
import com.wikigrapher.slim.RelationDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class PathCtrl
    @Autowired
    constructor(
        private val service: IPathService,
    ) : IPathApi {
        private val logger = LoggerFactory.getLogger(this::class.java)

        override fun shortestPathLength(
            sourceTitle: String,
            targetTitle: String,
        ): Mono<Int> {
            val escapedSourceTitle = Commons.sanitizeInput(sourceTitle)
            val escapedTargetTitle = Commons.sanitizeInput(targetTitle)
            logger.debug("shortestPathLength, sourceTitle: {}", escapedSourceTitle)
            logger.debug("shortestPathLength, targetTitle: {}", escapedTargetTitle)
            return service.shortestPathLength(escapedSourceTitle, escapedTargetTitle)
        }

        override fun shortestPathByTitle(
            sourceTitle: String,
            targetTitle: String,
        ): Flux<RelationDto> {
            val escapedSourceTitle = Commons.sanitizeInput(sourceTitle)
            val escapedTargetTitle = Commons.sanitizeInput(targetTitle)
            logger.debug("shortestPathByTitle, sourceTitle: {}", escapedSourceTitle)
            logger.debug("shortestPathByTitle, targetTitle: {}", escapedTargetTitle)
            return service.shortestPathByTitle(escapedSourceTitle, escapedTargetTitle)
        }

        override fun getRandomShortestPath(): Flux<RelationDto> {
            logger.debug("getRandomShortestPath")
            return service.getRandomShortestPath()
        }

        override fun shortestPathsByTitle(
            sourceTitle: String,
            targetTitle: String,
            skip: Int?,
            limit: Int?,
        ): Flux<RelationDto> {
            val escapedSourceTitle = Commons.sanitizeInput(sourceTitle)
            val escapedTargetTitle = Commons.sanitizeInput(targetTitle)
            logger.debug("shortestPathsByTitle, sourceTitle: {}", escapedSourceTitle)
            logger.debug("shortestPathsByTitle, targetTitle: {}", escapedTargetTitle)
            logger.debug("shortestPathsByTitle, skip: {}", skip)
            logger.debug("shortestPathsByTitle, limit: {}", limit)
            return service.shortestPathsByTitle(escapedSourceTitle, escapedTargetTitle, skip!!, limit!!)
        }

        override fun allShortestPathsByTitle(
            sourceTitle: String,
            targetTitle: String,
        ): Flux<RelationDto> {
            val escapedSourceTitle = Commons.sanitizeInput(sourceTitle)
            val escapedTargetTitle = Commons.sanitizeInput(targetTitle)
            logger.debug("allShortestPathsByTitle, sourceTitle: {}", escapedSourceTitle)
            logger.debug("allShortestPathsByTitle, targetTitle: {}", escapedTargetTitle)
            return service.allShortestPathsByTitle(escapedSourceTitle, escapedTargetTitle)
        }
    }
