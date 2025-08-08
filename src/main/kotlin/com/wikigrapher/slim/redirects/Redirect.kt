package com.wikigrapher.slim.redirects

import com.wikigrapher.slim.NodeDto
import com.wikigrapher.slim.TYPE
import com.wikigrapher.slim.categories.Category
import com.wikigrapher.slim.categories.CategorySubProjection
import com.wikigrapher.slim.pages.Page
import com.wikigrapher.slim.pages.PageProjection
import com.wikigrapher.slim.pages.TmpNode
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Property
import org.springframework.data.neo4j.core.schema.Relationship

@Node("redirect")
data class Redirect(
    @Id @Property val pageId: String,
    @Property val title: String,
    @Relationship(type = "redirect_to", direction = Relationship.Direction.OUTGOING) val page: Page? = null,
    @Relationship(type = "redirect_to", direction = Relationship.Direction.OUTGOING) val redirect: Redirect? = null,
    @Relationship(
        type = "belong_to",
        direction = Relationship.Direction.OUTGOING,
    ) val categories: Set<Category>? = emptySet(),
)

interface RedirectProjection {
    fun getPageId(): String

    fun getTitle(): String

    fun getPage(): PageProjection?

    fun getRedirect(): RedirectProjection?

    fun getCategories(): Set<CategorySubProjection>?

    fun toNode(): TmpNode =
        TmpNode(
            this.getPageId(),
            this.getTitle(),
            TYPE.REDIRECT,
            listOfNotNull(this.getPage()?.toNode(), this.getRedirect()?.toNode()).toSet(),
        )

    fun toDto(): NodeDto =
        NodeDto(
            this.getPageId(),
            this.getTitle(),
            TYPE.REDIRECT,
            listOfNotNull(this.getPage()?.toDto(), this.getRedirect()?.toDto()).map { it.toSubDto() }.toSet(),
            emptySet(),
            this.getCategories()?.map { it.toDto() }?.toSet(),
        )
}
