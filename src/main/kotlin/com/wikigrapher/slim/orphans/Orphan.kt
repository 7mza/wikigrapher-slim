package com.wikigrapher.slim.orphans

import com.wikigrapher.slim.NodeSubDto
import com.wikigrapher.slim.TYPE
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Property

@Node("orphan")
data class Orphan(
    @Id @Property val id: String,
    @Property val title: String,
    @Property val type: String,
)

interface OrphanProjection {
    fun getId(): String

    fun getTitle(): String

    fun getType(): String

    fun toSubDto(): NodeSubDto =
        NodeSubDto(
            this.getId(),
            this.getTitle(),
            this.getType().let { if (it.equals("page", true)) TYPE.PAGE else TYPE.REDIRECT },
            null,
            null,
        )
}
