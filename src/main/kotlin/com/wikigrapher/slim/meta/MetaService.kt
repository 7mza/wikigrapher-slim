package com.wikigrapher.slim.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.wikigrapher.slim.Commons
import com.wikigrapher.slim.DumpMetaDto
import com.wikigrapher.slim.DumpNodes
import com.wikigrapher.slim.DumpRelations
import com.wikigrapher.slim.categories.ICategoryRepository
import com.wikigrapher.slim.orphans.IOrphanRepository
import com.wikigrapher.slim.pages.IPageRepository
import com.wikigrapher.slim.redirects.IRedirectRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

interface IMetaService {
    fun findDumpMeta(): Mono<DumpMetaDto>
}

@Service
class MetaService
    @Autowired
    constructor(
        private val pageRepository: IPageRepository,
        private val redirectRepository: IRedirectRepository,
        private val categoryRepository: ICategoryRepository,
        private val metaRepository: IMetaRepository,
        private val orphanRepository: IOrphanRepository,
        private val objectMapper: ObjectMapper,
    ) : IMetaService {
        override fun findDumpMeta(): Mono<DumpMetaDto> =
            Mono
                .zip(
                    metaRepository.findMetaByProperty("dump"),
                    pageRepository.countPages(),
                    redirectRepository.countRedirects(),
                    categoryRepository.countCategories(),
                    orphanRepository.countOrphans(),
                    pageRepository.countLinkTo(),
                    redirectRepository.countRedirectTo(),
                    categoryRepository.countContains(),
                ).map {
                    Commons.parseJson<Dump>(it.t1.getValue(), objectMapper).toDto(
                        nodes = DumpNodes(it.t2, it.t3, it.t4, it.t5),
                        relations = DumpRelations(it.t6, it.t7, it.t8),
                    )
                }
    }

data class Dump(
    val lang: String,
    val date: String,
    val url: String,
) {
    fun toDto(
        nodes: DumpNodes,
        relations: DumpRelations,
    ) = DumpMetaDto(
        lang = this.lang,
        date = this.date,
        url = this.url,
        nodes = nodes,
        relations = relations,
    )
}
