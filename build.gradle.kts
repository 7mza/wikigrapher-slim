import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.github.gradle.node.npm.task.NpmTask
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import org.owasp.dependencycheck.reporting.ReportGenerator.Format.HTML

plugins {
    id("com.autonomousapps.dependency-analysis") version "2.19.0"
    id("com.bmuschko.docker-remote-api") version "9.4.0"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("com.github.node-gradle.node") version "7.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0"
    id("org.owasp.dependencycheck") version "12.1.3"
    id("org.springframework.boot") version "3.5.4"
    jacoco
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.jpa") version "2.2.0"
    kotlin("plugin.noarg") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
}

group = "com.wikigrapher"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
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

val blockhoundVersion = "1.0.13.RELEASE"
val htmlunitVersion = "4.14.0"
val mockitoAgent = configurations.create("mockitoAgent")
val mockitoCoreVersion = "5.18.0"
val mockitoKotlinVersion = "6.0.0"
val openapiVersion = "2.8.9"
val springCloudVersion = "2025.0.0"

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:$openapiVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-neo4j")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    mockitoAgent("org.mockito:mockito-core:$mockitoCoreVersion") { isTransitive = false }

    testImplementation("io.projectreactor:reactor-test")
    // testImplementation("io.projectreactor.tools:blockhound-junit-platform:$blockhoundVersion")
    testImplementation("org.htmlunit:htmlunit:$htmlunitVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:neo4j")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
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
//    if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_13)) {
//        jvmArgs("-XX:+AllowRedefinitionToAddDeleteMethods")
//    }
    finalizedBy(tasks.jacocoTestReport)
    configure<JacocoTaskExtension> {
        excludes = listOf("org/htmlunit/**", "jdk.internal.*")
        isIncludeNoLocationClasses = true
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
    version.set("1.7.1")
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
    images.set(listOf("wikigrapher-slim:latest"))
    outputs.cacheIf { true }
}

tasks.check {
    finalizedBy("buildLocalDockerImage")
}

dependencyCheck {
    // https://nvd.nist.gov/developers/request-an-api-key
    nvd.apiKey = System.getenv("NVD_APIKEY") ?: ""
}
