package com.wikigrapher.slim

import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.neo4j.Neo4jContainer
import org.testcontainers.utility.DockerImageName

object TestContainersFactory {
    fun neo4jContainer(): Neo4jContainer =
        Neo4jContainer(DockerImageName.parse("neo4j:community"))
            .withoutAuthentication()
            .withPlugins("apoc")
            .withNeo4jConfig("client.allow_telemetry", "false")
            .withNeo4jConfig("server.bolt.telemetry.enabled", "false")
            .withNeo4jConfig("dbms.usage_report.enabled", "false")
            .withNeo4jConfig("dbms.security.procedures.allowlist", "apoc.*")
            .withNeo4jConfig("dbms.security.procedures.unrestricted", "apoc.*")
            .waitingFor(Wait.forSuccessfulCommand("wget --no-verbose --tries=1 --spider localhost:7474 || exit 1"))
}
