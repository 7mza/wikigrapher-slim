package com.wikigrapher.slim.meta

import com.wikigrapher.slim.DumpMetaDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

@Tag(name = "meta", description = "db meta operations")
@RequestMapping(value = ["/api/core"], produces = [MediaType.APPLICATION_JSON_VALUE])
interface IMetaApi {
    @GetMapping("/meta/dump")
    @Operation(
        summary = "get current dump metadata",
        description = "TODO",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = DumpMetaDto::class),
                        examples = [
                            ExampleObject(
                                name = "example-0",
// @formatter:off
                                value =
"""
{
  "lang": "en",
  "date": "11111111",
  "url": "https://dumps.wikimedia.org/enwiki/11111111",
  "nodes": { "pages": 9, "redirects": 7, "categories": 3 },
  "relations": { "link_to": 11, "redirect_to": 7, "belong_to": 8 }
}
""",
// @formatter:on
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun findDumpMeta(): Mono<DumpMetaDto>
}
