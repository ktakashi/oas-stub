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
    api(project(":app:spring:oas-stub-spring-boot-starter-web"))
    implementation(libs.slf4j.api)
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.spring.cloud.starter.bootstrap) {
        exclude(group = "org.springframework.boot")
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.fasterxml.jackson.module")
    }
    implementation(libs.spring.cloud.starter.config) {
        exclude(group = "org.springframework.boot")
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.fasterxml.jackson.module")
    }

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
