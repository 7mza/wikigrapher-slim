package com.wikigrapher.slim.paths

import com.wikigrapher.slim.RelationDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Tag(name = "path", description = "path operations")
@RequestMapping(value = ["/api/core"], produces = [MediaType.APPLICATION_JSON_VALUE])
interface IPathApi {
    @GetMapping("/path/length")
    @Operation(
        summary = "find shortest path length between two nodes",
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
                        schema = Schema(implementation = Int::class),
                        examples = [
                            ExampleObject(
                                name = "example-0",
                                value = "3",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun shortestPathLength(
        @Schema(
            description = "title of source page",
            example = "Gandalf",
            required = true,
        )
        @RequestParam
        @NotBlank(message = "sourceTitle must not be blank")
        @NotEmpty(message = "sourceTitle must not be empty")
        sourceTitle: String,
        @Schema(
            description = "title of target page",
            example = "Ubuntu",
            required = true,
        )
        @RequestParam
        @NotBlank(message = "targetTitle must not be blank")
        @NotEmpty(message = "targetTitle must not be empty")
        targetTitle: String,
    ): Mono<Int>

    @GetMapping("/path")
    @Operation(
        summary = "find shortest path between two nodes",
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
                        array = ArraySchema(schema = Schema(implementation = RelationDto::class)),
                        examples = [
                            ExampleObject(
                                name = "example-0",
// @formatter:off
                                value =
"""
[
  {
    "source": {
      "id": "3",
      "title": "gandalf",
      "type": "PAGE",
      "isSourceTopParent": true,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "11",
      "title": "wizard",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    }
  },
  {
    "source": {
      "id": "11",
      "title": "wizard",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "15",
      "title": "wisdom",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    }
  },
  {
    "source": {
      "id": "15",
      "title": "wisdom",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "27",
      "title": "the bright lord",
      "type": "REDIRECT",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    }
  },
  {
    "source": {
      "id": "27",
      "title": "the bright lord",
      "type": "REDIRECT",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "9",
      "title": "celebrimbor",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": true
    }
  }
]
""",
// @formatter:on
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun shortestPathByTitle(
        @Schema(
            description = "title of source page",
            example = "Gandalf",
            required = true,
        )
        @RequestParam
        @NotBlank(message = "sourceTitle must not be blank")
        @NotEmpty(message = "sourceTitle must not be empty")
        sourceTitle: String,
        @Schema(
            description = "title of target page",
            example = "Ubuntu",
            required = true,
        )
        @RequestParam
        @NotBlank(message = "targetTitle must not be blank")
        @NotEmpty(message = "targetTitle must not be empty")
        targetTitle: String,
    ): Flux<RelationDto>

    @GetMapping("/path/random")
    @Operation(
        summary = "get shortest path between two random page nodes",
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
                        array = ArraySchema(schema = Schema(implementation = RelationDto::class)),
                        examples = [
                            ExampleObject(
                                name = "example-1",
// @formatter:off
                                value =
"""
[
  {
    "source": {
      "id": "3",
      "title": "gandalf",
      "type": "PAGE",
      "isSourceTopParent": true,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "11",
      "title": "wizard",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    }
  },
  {
    "source": {
      "id": "11",
      "title": "wizard",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "15",
      "title": "wisdom",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    }
  },
  {
    "source": {
      "id": "15",
      "title": "wisdom",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "27",
      "title": "the bright lord",
      "type": "REDIRECT",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    }
  },
  {
    "source": {
      "id": "27",
      "title": "the bright lord",
      "type": "REDIRECT",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "9",
      "title": "celebrimbor",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": true
    }
  }
]
""",
// @formatter:on
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getRandomShortestPath(): Flux<RelationDto>

    @GetMapping("/paths")
    @Operation(
        summary = "find N paginated shortest paths between two nodes",
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
                        array = ArraySchema(schema = Schema(implementation = RelationDto::class)),
                        examples = [
                            ExampleObject(
                                name = "example-2",
// @formatter:off
                                value =
"""
[
  {
    "source": {
      "id": "3",
      "title": "gandalf",
      "type": "PAGE",
      "isSourceTopParent": true,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "11",
      "title": "wizard",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    }
  },
  {
    "source": {
      "id": "11",
      "title": "wizard",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "15",
      "title": "wisdom",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    }
  },
  {
    "source": {
      "id": "15",
      "title": "wisdom",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "27",
      "title": "the bright lord",
      "type": "REDIRECT",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    }
  },
  {
    "source": {
      "id": "27",
      "title": "the bright lord",
      "type": "REDIRECT",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "9",
      "title": "celebrimbor",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": true
    }
  }
]
""",
// @formatter:on
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun shortestPathsByTitle(
        @Schema(
            description = "title of source page",
            example = "Gandalf",
            required = true,
        )
        @RequestParam
        @NotBlank(message = "sourceTitle must not be blank")
        @NotEmpty(message = "sourceTitle must not be empty")
        sourceTitle: String,
        @Schema(
            description = "title of target page",
            example = "Ubuntu",
            required = true,
        )
        @RequestParam
        @NotBlank(message = "targetTitle must not be blank")
        @NotEmpty(message = "targetTitle must not be empty")
        targetTitle: String,
        @Schema(description = "skip n initial paths", example = "0", minimum = "0")
        @RequestParam(required = false)
        @PositiveOrZero(message = "skip must be greater or eq to 0")
        skip: Int? = 0,
        @Schema(description = "limit to n paths", example = "5", minimum = "1")
        @RequestParam(required = false)
        @Positive(message = "limit must be greater than 0")
        limit: Int? = 5,
    ): Flux<RelationDto>

    @GetMapping("/paths/all")
    @Operation(
        summary = "find all shortest paths between two nodes",
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
                        array = ArraySchema(schema = Schema(implementation = RelationDto::class)),
                        examples = [
                            ExampleObject(
                                name = "example-3",
// @formatter:off
                                value =
"""
[
  {
    "source": {
      "id": "3",
      "title": "gandalf",
      "type": "PAGE",
      "isSourceTopParent": true,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "11",
      "title": "wizard",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    }
  },
  {
    "source": {
      "id": "11",
      "title": "wizard",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "15",
      "title": "wisdom",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    }
  },
  {
    "source": {
      "id": "15",
      "title": "wisdom",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "27",
      "title": "the bright lord",
      "type": "REDIRECT",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    }
  },
  {
    "source": {
      "id": "27",
      "title": "the bright lord",
      "type": "REDIRECT",
      "isSourceTopParent": false,
      "isTargetBottomChild": false
    },
    "target": {
      "id": "9",
      "title": "celebrimbor",
      "type": "PAGE",
      "isSourceTopParent": false,
      "isTargetBottomChild": true
    }
  }
]
""",
// @formatter:on
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun allShortestPathsByTitle(
        @Schema(
            description = "title of source page",
            example = "Gandalf",
            required = true,
        )
        @RequestParam
        @NotBlank(message = "sourceTitle must not be blank")
        @NotEmpty(message = "sourceTitle must not be empty")
        sourceTitle: String,
        @Schema(
            description = "title of target page",
            example = "Ubuntu",
            required = true,
        )
        @RequestParam
        @NotBlank(message = "targetTitle must not be blank")
        @NotEmpty(message = "targetTitle must not be empty")
        targetTitle: String,
    ): Flux<RelationDto>
}
