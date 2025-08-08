package com.wikigrapher.slim.categories

import com.wikigrapher.slim.CategorySubDto
import com.wikigrapher.slim.pages.Page
import com.wikigrapher.slim.redirects.Redirect
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Property
import org.springframework.data.neo4j.core.schema.Relationship

@Node("category")
data class Category(
    @Id @Property val categoryId: String?,
    @Property val title: String,
    @Relationship(type = "contains", direction = Relationship.Direction.OUTGOING) val pages: Set<Page>? = emptySet(),
    @Relationship(
        type = "contains",
        direction = Relationship.Direction.OUTGOING,
    ) val redirects: Set<Redirect>? = emptySet(),
)

interface CategorySubProjection {
    fun getCategoryId(): String

    fun getTitle(): String

    fun toDto() = CategorySubDto(this.getCategoryId(), this.getTitle())
}
