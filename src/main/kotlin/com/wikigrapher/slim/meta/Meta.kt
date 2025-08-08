package com.wikigrapher.slim.meta

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Property

@Node("meta")
data class Meta(
    @Id @Property val metaId: String,
    @Property val property: String,
    @Property val value: String,
)

interface MetaProjection {
    fun getMetaId(): String

    fun getProperty(): String

    fun getValue(): String
}
