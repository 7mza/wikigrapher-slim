package com.wikigrapher.slim

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import jakarta.annotation.PreDestroy
import org.apache.commons.lang3.RandomStringUtils
import org.neo4j.cypherdsl.core.renderer.Dialect
import org.neo4j.driver.Driver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager
import org.springframework.data.neo4j.repository.config.ReactiveNeo4jRepositoryConfigurationExtension
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.charset.StandardCharsets

@Configuration
class DefaultConfigs {
    private val logger = LoggerFactory.getLogger(this::class.java)

    // FIXME: https://spring.io/blog/2025/10/07/introducing-jackson-3-support-in-spring
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

    @Bean(name = ["wikipedia-web-client"])
    fun wikipediaWebClient(
        builder: WebClient.Builder,
        clientsProperties: ClientsProperties,
        objectMapper: ObjectMapper,
    ): WebClient {
        val baseUrl = clientsProperties.wikipediaWeb!!.getBaseUrl()
        logger.debug("wikipedia web baseUrl: {}", baseUrl)
        return builder
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .baseUrl(baseUrl)
            .codecs {
                it.defaultCodecs().jacksonJsonDecoder(Jackson2JsonDecoder(objectMapper))
                it.defaultCodecs().jacksonJsonEncoder(Jackson2JsonEncoder(objectMapper))
            }.build()
    }

    @Bean(name = ["wikipedia-api-client"])
    fun wikipediaApiClient(
        builder: WebClient.Builder,
        clientsProperties: ClientsProperties,
        objectMapper: ObjectMapper,
    ): WebClient {
        val baseUrl = clientsProperties.wikipediaApi!!.getBaseUrl()
        logger.debug("wikipedia api baseUrl: {}", baseUrl)
        return builder
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Api-User-Agent", RandomStringUtils.secure().nextAlphabetic(5))
            .defaultHeader("Authorization", clientsProperties.wikipediaApi?.accessToken ?: "")
            .baseUrl(baseUrl)
            .codecs {
                it.defaultCodecs().jacksonJsonDecoder(Jackson2JsonDecoder(objectMapper))
                it.defaultCodecs().jacksonJsonEncoder(Jackson2JsonEncoder(objectMapper))
            }.build()
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
    var wikipediaApi: Client? = null
    var wikipediaWeb: Client? = null

    data class Client(
        var host: String? = null,
        var port: String? = null,
        var proto: String? = null,
        var accessToken: String? = null,
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
                .withDialect(Dialect.NEO4J_5_DEFAULT_CYPHER)
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
            .readFileAsString("classpath:/static/dist/asset-manifest.json")
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
    private fun readFileAsBytes(
        path: String,
        bufferSize: Int = 4096,
    ): Mono<ByteArray> =
        Mono
            .fromCallable { resourceLoader.getResource(path) }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { resource -> readFully(resource, bufferSize) }

    fun readFileAsString(
        path: String,
        bufferSize: Int = 4096,
    ): Mono<String> =
        readFileAsBytes(path, bufferSize)
            .map { String(it, StandardCharsets.UTF_8) }

    private fun readFully(
        resource: Resource,
        bufferSize: Int,
    ): Mono<ByteArray> =
        DataBufferUtils
            .join(DataBufferUtils.read(resource, DefaultDataBufferFactory(), bufferSize))
            .map {
                val bytes = ByteArray(it.readableByteCount())
                it.read(bytes)
                DataBufferUtils.release(it)
                bytes
            }
}
