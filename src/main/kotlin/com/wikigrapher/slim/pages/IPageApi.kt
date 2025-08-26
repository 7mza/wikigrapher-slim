package com.wikigrapher.slim.pages

import com.wikigrapher.slim.NodeDto
import com.wikigrapher.slim.NodeSubDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux

@Tag(name = "page", description = "page operations")
@RequestMapping(value = ["/api/core"], produces = [MediaType.APPLICATION_JSON_VALUE])
interface IPageApi {
    @GetMapping("/page/random")
    @Operation(
        summary = "get N random pages",
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
                        array = ArraySchema(schema = Schema(implementation = NodeSubDto::class)),
                        examples = [
                            ExampleObject(
                                name = "example-0",
// @formatter:off
                                value =
"""
[{ "id": "3", "title": "gandalf", "type": "PAGE" }]
""",
// @formatter:on
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getNRandomPages(
        @Schema(description = "number of random pages", example = "1", minimum = "1", maximum = "10")
        @RequestParam(required = false)
        @Positive(message = "n must be greater than 0")
        @Max(value = 10, message = "n must be lower or eq to 10")
        n: Int? = 1,
    ): Flux<NodeSubDto>
}
