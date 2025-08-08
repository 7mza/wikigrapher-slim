package com.wikigrapher.slim.pages

import com.wikigrapher.slim.NodeDto
import com.wikigrapher.slim.NodeSubDto
import com.wikigrapher.slim.TYPE
import com.wikigrapher.slim.categories.Category
import com.wikigrapher.slim.categories.CategorySubProjection
import com.wikigrapher.slim.redirects.Redirect
import com.wikigrapher.slim.redirects.RedirectProjection
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Property
import org.springframework.data.neo4j.core.schema.Relationship

@Node("page")
data class Page(
    @Id @Property val pageId: String,
    @Property val title: String,
    @Relationship(type = "link_to", direction = Relationship.Direction.OUTGOING) val pages: Set<Page>? = emptySet(),
    @Relationship(type = "link_to", direction = Relationship.Direction.OUTGOING) val redirects: Set<Redirect>? =
        emptySet(),
    @Relationship(
        type = "belong_to",
        direction = Relationship.Direction.OUTGOING,
    ) val categories: Set<Category>? = emptySet(),
)

interface PageProjection {
    fun getPageId(): String

    fun getTitle(): String

    fun getPages(): Set<PageProjection>?

    fun getRedirects(): Set<RedirectProjection>?

    fun getCategories(): Set<CategorySubProjection>?

    fun toNode(): TmpNode {
        val pages = this.getPages()?.map { it.toNode() }
        val redirects = this.getRedirects()?.map { it.toNode() }
        val nodes = ((pages ?: emptyList()) + (redirects ?: emptyList())).toSet()
        return TmpNode(
            this.getPageId(),
            this.getTitle(),
            TYPE.PAGE,
            nodes,
        )
    }

    fun toDto(): NodeDto {
        val pages = this.getPages()?.map { it.toDto() }
        val redirects = this.getRedirects()?.map { it.toDto() }
        val nodes = ((pages ?: emptyList()) + (redirects ?: emptyList())).map { it.toSubDto() }.toSet()
        return NodeDto(
            this.getPageId(),
            this.getTitle(),
            TYPE.PAGE,
            nodes,
            emptySet(),
            this.getCategories()?.map { it.toDto() }?.toSet(),
        )
    }
}

data class TmpNode(
    val id: String,
    val title: String,
    val type: TYPE,
    val outgoing: Set<TmpNode>? = emptySet(),
) {
    fun toSubDto() = NodeSubDto(id, title, type)
}
