import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.github.gradle.node.npm.task.NpmTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import org.owasp.dependencycheck.reporting.ReportGenerator.Format.HTML

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"

    id("com.autonomousapps.dependency-analysis") version "3.5.1"
    id("com.bmuschko.docker-remote-api") version "10.0.0"
    id("com.github.ben-manes.versions") version "0.53.0"
    id("com.github.node-gradle.node") version "7.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("org.owasp.dependencycheck") version "12.1.9"
    jacoco
}

group = "com.wikigrapher"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

val blockhoundVersion = "1.0.15.RELEASE"
val mockitoAgent: Configuration = configurations.create("mockitoAgent")
val mockitoCoreVersion = "5.21.0"
val mockitoKotlinVersion = "6.1.0"
val openapiVersion = "3.0.1"
val springCloudVersion = "2025.1.0"
val wiremockSpringBootVersion = "4.0.8"

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:$openapiVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-neo4j")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-webclient")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("tools.jackson.module:jackson-module-kotlin")

    mockitoAgent("org.mockito:mockito-core:$mockitoCoreVersion") { isTransitive = false }

    // testImplementation("io.projectreactor.tools:blockhound-junit-platform:$blockhoundVersion")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.htmlunit:htmlunit")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-neo4j")
    testImplementation("org.wiremock.integrations:wiremock-spring-boot:$wiremockSpringBootVersion")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
    options.isIncremental = true
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "-javaagent:${mockitoAgent.asPath}",
        "-XX:+EnableDynamicAgentLoading",
    )
    if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_13)) {
        jvmArgs("-XX:+AllowRedefinitionToAddDeleteMethods")
    }
    finalizedBy(tasks.jacocoTestReport)
    configure<JacocoTaskExtension> {
        excludes = listOf("org/htmlunit/**", "jdk.internal.*")
        isIncludeNoLocationClasses = true
    }
    testLogging {
        events = setOf(FAILED)
        exceptionFormat = FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
        showStandardStreams = false
    }
}

tasks.withType<Test>().configureEach {
    maxParallelForks = 2
//    forkEvery = 100
    reports {
        html.required = false
        junitXml.required = false
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    exclude("**/ApplicationKt.class")
                }
            },
        ),
    )
    reports {
        csv.required = false
        html.outputLocation = layout.buildDirectory.dir("jacocoHtml")
        xml.required = false
    }
}

configure<KtlintExtension> {
    android.set(false)
    coloredOutput.set(true)
    debug.set(true)
    verbose.set(true)
    version.set("1.8.0")
}

configure<DependencyCheckExtension> {
    format = HTML.toString()
}

node {
    download = true
}

tasks.processResources {
    dependsOn("npm_run_build")
}

tasks.register("npm_run_build", NpmTask::class) {
    val isDev = project.hasProperty("mode") && project.property("mode") == "development"
    inputs.files(
        fileTree("$projectDir/src/main/resources/static/ts"),
        fileTree("$projectDir/src/main/resources/static/scss"),
    )
    outputs.files(fileTree("$projectDir/src/main/resources/static/dist"))
    args = listOf("run", if (isDev) "build:dev" else "build")
}

tasks.jar {
    enabled = false
}

val prepareDockerContext by tasks.registering(Sync::class) {
    dependsOn(tasks.bootJar)
    project.tasks.findByName("npm_run_build")?.let {
        dependsOn(it)
    }
    inputs.files(
        fileTree("src") {
            exclude("main/resources/static/dist/**")
        },
    )
    outputs.dir(layout.buildDirectory.dir("docker-context"))
    from("Dockerfile")
    from("build/libs") {
        into("build/libs")
    }
    into(layout.buildDirectory.dir("docker-context"))
}

tasks.register<DockerBuildImage>("buildLocalDockerImage") {
    dependsOn(prepareDockerContext)
    inputDir.set(layout.buildDirectory.dir("docker-context"))
    images.set(listOf("${project.name}:latest"))
    outputs.cacheIf { true }
}

// tasks.check {
//    finalizedBy("buildLocalDockerImage")
// }

dependencyCheck {
    // https://nvd.nist.gov/developers/request-an-api-key
    nvd.apiKey = System.getenv("NVD_APIKEY") ?: ""
}
