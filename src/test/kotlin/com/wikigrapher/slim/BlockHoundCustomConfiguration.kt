package com.wikigrapher.slim

/*
import org.thymeleaf.templateparser.reader.PrototypeOnlyCommentMarkupReader
import reactor.blockhound.BlockHound
import reactor.blockhound.integration.BlockHoundIntegration
import java.util.zip.InflaterInputStream

class BlockHoundCustomConfiguration: BlockHoundIntegration {
    override fun applyTo(builder: BlockHound.Builder) {
        builder
            .allowBlockingCallsInside(InflaterInputStream::class.java.name, "read")
            .allowBlockingCallsInside(PrototypeOnlyCommentMarkupReader::class.java.name, "read")
    }
}
*/

class BlockHoundCustomConfiguration
