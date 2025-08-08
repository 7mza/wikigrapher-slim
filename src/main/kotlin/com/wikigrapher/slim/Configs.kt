package com.wikigrapher.slim

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import jakarta.annotation.PreDestroy
import org.neo4j.cypherdsl.core.renderer.Dialect
import org.neo4j.driver.Driver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager
import org.springframework.data.neo4j.repository.config.ReactiveNeo4jRepositoryConfigurationExtension
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.charset.StandardCharsets

@Configuration
class DefaultConfigs {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun objectMapper(): ObjectMapper =
        ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
        }

    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("wikigrapher-slim-api")
                .description("TODO")
                .version("0.0.1")
                .contact(Contact().name("dev").email("alias.ducky891@passinbox.com")),
        )

    @Bean
    fun wikipediaWebClient(
        builder: WebClient.Builder,
        clientsProperties: ClientsProperties,
    ): WebClient {
        val baseUrl = clientsProperties.wikipedia!!.getBaseUrl()
        logger.debug("wikipedia baseUrl: {}", baseUrl)
        return builder
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .baseUrl(baseUrl)
            .build()
    }

    @Bean
    fun reactiveFileReader(resourceLoader: ResourceLoader) = ReactiveFileReader(resourceLoader)

    @Bean
    fun assetManifestReader(
        reactiveFileReader: ReactiveFileReader,
        objectMapper: ObjectMapper,
    ) = ReactiveAssetManifestReader(
        reactiveFileReader = reactiveFileReader,
        objectMapper = objectMapper,
    )
}

@Configuration
@ConfigurationProperties(prefix = "init")
class InitProperties {
    var paths: List<PathProp>? = null

    data class PathProp(
        var source: String?,
        var target: String?,
    )

    fun getRandomPath(): PathProp = paths!!.random()
}

@Configuration
@ConfigurationProperties(prefix = "clients")
class ClientsProperties {
    var wikipedia: Client? = null

    data class Client(
        var host: String? = null,
        var port: String? = null,
        var proto: String? = null,
    ) {
        fun getBaseUrl() = "$proto://$host" + if (port.isNullOrBlank().not()) ":$port" else ""
    }
}

@Configuration
@Profile("!override-neo4j")
class Neo4jConfigs
    @Autowired
    constructor(
        private val driver: Driver?,
        private val databaseNameProvider: ReactiveDatabaseSelectionProvider?,
    ) {
        @Bean
        fun cypherDslConfiguration(): org.neo4j.cypherdsl.core.renderer.Configuration =
            org.neo4j.cypherdsl.core.renderer.Configuration
                .newConfig()
                .withDialect(Dialect.NEO4J_5_23)
                .build()

        @Bean(ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME)
        fun reactiveNeo4jTransactionManager() = ReactiveNeo4jTransactionManager(driver!!, databaseNameProvider!!)

        @PreDestroy
        fun shutdown() {
            driver?.close()
        }
    }

data class AssetNotFoundException(
    val name: String,
) : RuntimeException("asset $name not found at static/dist/asset-manifest.json")

class ReactiveAssetManifestReader(
    private val reactiveFileReader: ReactiveFileReader,
    private val objectMapper: ObjectMapper,
) {
    private val assetMapMono: Mono<Map<String, String>> by lazy {
        reactiveFileReader
            .readFileFromResources("classpath:/static/dist/asset-manifest.json")
            .map {
                objectMapper.readValue(it, object : TypeReference<Map<String, String>>() {})
            }.cache()
    }

    fun get(name: String): Mono<String> =
        assetMapMono.flatMap { map ->
            map[name]?.let { Mono.just(it) }
                ?: Mono.error(AssetNotFoundException(name = name))
        }

    fun getAll(): Mono<Map<String, String>> = assetMapMono
}

class ReactiveFileReader(
    private val resourceLoader: ResourceLoader,
) {
    fun readFileFromResources(path: String): Mono<String> =
        Mono
            .fromCallable {
                resourceLoader.getResource(path)
            }.subscribeOn(Schedulers.boundedElastic())
            .flatMap { resource ->
                DataBufferUtils
                    .read(resource, DefaultDataBufferFactory(), 4096)
                    .reduce { buf1, buf2 ->
                        val combined =
                            DefaultDataBufferFactory().allocateBuffer(
                                buf1.readableByteCount() + buf2.readableByteCount(),
                            )
                        combined.write(buf1)
                        combined.write(buf2)
                        DataBufferUtils.release(buf1)
                        DataBufferUtils.release(buf2)
                        combined
                    }.map { dataBuffer ->
                        val content =
                            dataBuffer
                                .readableByteBuffers()
                                .asSequence()
                                .map { StandardCharsets.UTF_8.decode(it).toString() }
                                .joinToString("")
                        DataBufferUtils.release(dataBuffer)
                        content
                    }.cache()
            }
}
