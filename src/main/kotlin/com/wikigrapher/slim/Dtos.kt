package com.wikigrapher.slim

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

data class CategorySubDto(
    val id: String,
    val title: String,
)

data class NodeDto(
    val id: String,
    val title: String,
    val type: TYPE,
    val outgoing: Set<NodeSubDto>? = emptySet(),
    val incoming: Set<NodeSubDto>? = emptySet(),
    val categories: Set<CategorySubDto>? = emptySet(),
) {
    fun toSubDto() =
        NodeSubDto(
            this.id,
            this.title,
            this.type,
        )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NodeSubDto(
    val id: String,
    var title: String,
    val type: TYPE,
    val isTopParent: Boolean? = null,
    val isBottomChild: Boolean? = null,
) {
    init {
        title = Commons.sanitizeOutput(title)
    }
}

enum class TYPE {
    PAGE,
    REDIRECT,
}

// simple flat DTO to avoid any calculation in browser/js
data class RelationDto(
    val source: NodeSubDto,
    val target: NodeSubDto,
)

data class DumpMetaDto(
    val lang: String,
    val date: String,
    val url: String,
    val nodes: DumpNodes,
    val relations: DumpRelations,
)

data class DumpNodes(
    val pages: Long,
    val redirects: Long,
    val categories: Long,
)

data class DumpRelations(
    @field:JsonProperty("link_to") val linkTo: Long,
    @field:JsonProperty("redirect_to") val redirectTo: Long,
    @field:JsonProperty("belong_to") val belongTo: Long,
)

data class WikipediaPageImageDto(
    @field:JsonProperty("batchcomplete")
    val batchComplete: String?,
    val query: QueryDto?,
)

data class QueryDto(
    val normalized: List<NormalizedDto?>?,
    val pages: Map<String?, PageDto?>?,
)

data class NormalizedDto(
    val from: String?,
    val to: String?,
)

data class PageDto(
    @field:JsonProperty("pageid")
    val pageId: Int?,
    val ns: Int?,
    val title: String?,
    val thumbnail: ThumbnailDto?,
    @field:JsonProperty("pageimage")
    val pageImage: String?,
)

@JsonDeserialize(using = ThumbnailDtoDeserializer::class)
data class ThumbnailDto(
    val source: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

class ThumbnailDtoDeserializer : JsonDeserializer<ThumbnailDto>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext?,
    ): ThumbnailDto {
        val node: JsonNode = p.codec.readTree(p)
        val source = node.get("source")?.asText() ?: node.get("url")?.asText()
        val width = node.get("width")?.asInt()
        val height = node.get("height")?.asInt()
        return ThumbnailDto(source = source, width = width, height = height)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class SearchSuggestion(
    val id: String,
    val key: String,
    val title: String? = null,
    val description: String? = null,
    val thumbnail: ThumbnailDto? = null,
)

data class SearchSuggestionsDto(
    val pages: Set<SearchSuggestion>,
)
