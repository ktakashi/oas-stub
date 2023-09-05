plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.spring"
description = "OAS stub Spring Boot application"

dependencies {
    api(project(":app:spring:oas-stub-starter-web"))
    api(project(":app:spring:oas-stub-hazelcast-storage-autoconfigure"))
    api(project(":app:spring:oas-stub-mongodb-storage-autoconfigure"))
    implementation(libs.spring.boot.autoconfigure)

    testImplementation(enforcedPlatform(libs.cucumber.bom))
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("io.cucumber:cucumber-spring")
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.web)
    testImplementation(libs.projectreactor.reactor.core)
    testImplementation(libs.rest.assured) {
        exclude(group = "org.apache.groovy")
    }
    testImplementation(libs.groovy.xml)
    testImplementation(libs.groovy.json)
    testImplementation(libs.awaitility)
    testImplementation(libs.flapdoodle.embed.mongo)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
