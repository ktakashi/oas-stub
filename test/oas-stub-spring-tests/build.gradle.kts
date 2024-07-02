plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.test.spring"
description = "OAS stub test server tests for Spring"

dependencies {
    testImplementation(project(":lib:spring:oas-stub-spring-boot-starter-test"))
    testImplementation(project(":lib:spring:oas-stub-storage-autoconfigure-api"))
    testImplementation(project(":test:cucumber-test"))
    testImplementation(libs.spring.web)
    testImplementation(libs.slf4j.api)
    testImplementation(libs.spring.boot.autoconfigure)
    testImplementation(libs.hazelcast)
    testImplementation(libs.mongodb.driver.sync)
    testImplementation(enforcedPlatform(libs.cucumber.bom))
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("io.cucumber:cucumber-spring")
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.projectreactor.reactor.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
