package com.wikigrapher.slim.meta

import com.wikigrapher.slim.DumpMetaDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class MetaCtrl
    @Autowired
    constructor(
        private val service: IMetaService,
    ) : IMetaApi {
        override fun findDumpMeta(): Mono<DumpMetaDto> = service.findDumpMeta()
    }
