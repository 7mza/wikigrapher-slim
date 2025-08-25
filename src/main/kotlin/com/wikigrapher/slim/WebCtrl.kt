package com.wikigrapher.slim

import com.wikigrapher.slim.meta.IMetaService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.reactive.result.view.Rendering
import reactor.core.publisher.Mono

@RequestMapping(value = ["/"], produces = [MediaType.TEXT_HTML_VALUE])
interface IWeb {
    @GetMapping(path = ["/", "/paths"])
    fun index(): Mono<Rendering>

    @GetMapping("/fragments")
    fun getFragments(): Mono<Rendering>

    @GetMapping("/favicon.ico")
    fun noFavicon(): Mono<ResponseEntity<Void>>
}

@Controller
class WebCtrl
    @Autowired
    constructor(
        private val service: IMetaService,
        private val initProperties: InitProperties,
        private val assetManifestReader: ReactiveAssetManifestReader,
    ) : IWeb {
        override fun index(): Mono<Rendering> =
            service
                .findDumpMeta()
                .onErrorResume { fallBackDumpMeta() }
                .switchIfEmpty(fallBackDumpMeta())
                .flatMap { meta ->
                    val path = initProperties.getRandomPath()
                    assetManifestReader.getAll().map {
                        Rendering
                            .view("paths")
                            .modelAttribute("assetManifest", it)
                            .modelAttribute("dumpMeta", meta)
                            .modelAttribute("source", path.source!!)
                            .modelAttribute("target", path.target!!)
                            .build()
                    }
                }

        override fun getFragments(): Mono<Rendering> =
            assetManifestReader.getAll().map {
                Rendering
                    .view("fragments")
                    .modelAttribute("assetManifest", it)
                    .build()
            }

        override fun noFavicon(): Mono<ResponseEntity<Void>> = Mono.just(ResponseEntity.noContent().build())

        private fun fallBackDumpMeta(): Mono<DumpMetaDto> =
            Mono.just(
                DumpMetaDto(
                    lang = "EN",
                    date = "fallback",
                    url = "fallback",
                    nodes = DumpNodes(-1, -1, -1, -1),
                    relations = DumpRelations(-1, -1, -1),
                ),
            )
    }
