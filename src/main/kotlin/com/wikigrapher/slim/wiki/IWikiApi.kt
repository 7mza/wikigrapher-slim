package com.wikigrapher.slim.wiki

import com.wikigrapher.slim.ThumbnailDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Tag(name = "wiki", description = "wikipedia operations")
@RequestMapping(value = ["/api/wiki"], produces = [MediaType.APPLICATION_JSON_VALUE])
interface IWikiApi {
    @GetMapping("/image")
    @Operation(
        summary = "get a wikipedia page image by it's title",
        description = "CORS pass through",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "OK",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = ThumbnailDto::class),
                        examples = [
                            ExampleObject(
                                name = "example-0",
// @formatter:off
                                value =
"""
{
  "source": "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1b/Oreo-Two-Cookies.png/100px-Oreo-Two-Cookies.png",
  "width": 100,
  "height": 61
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
    fun getWikipediaPageImage(
        @Schema(
            description = "title of page",
            example = "Ubuntu",
            required = true,
        )
        @RequestParam
        @NotBlank(message = "title must not be blank")
        @NotEmpty(message = "title must not be empty")
        title: String,
        @Schema(
            description = "size of thumbnail",
            example = "200",
            minimum = "100",
            maximum = "400",
        )
        @RequestParam(required = false)
        @Min(value = 100, message = "piThumbSize min 100")
        @Max(value = 400, message = "piThumbSize max 400")
        piThumbSize: Int? = 200,
    ): Mono<ThumbnailDto>
}
