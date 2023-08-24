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
    api(project(":lib:oas-stub-engine"))
    api(project(":app:spring:oas-stub-inmemory-storage-autoconfigure"))
    api(project(":app:spring:oas-stub-hazelcast-storage-autoconfigure"))
    api(project(":app:spring:oas-stub-mongodb-storage-autoconfigure"))
    implementation(libs.spring.boot.starter.core)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.aop)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.cloud.starter.bootstrap)
    implementation(libs.spring.cloud.starter.config)
    implementation(libs.kotlin.stdlib)
    implementation(libs.projectreactor.reactor.core)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.swagger.core.annotations.jakarta)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.mongodb.driver.sync)

    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation(enforcedPlatform(libs.cucumber.bom))
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("io.cucumber:cucumber-spring")
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.rest.assured)
    testImplementation(libs.awaitility)
    testImplementation(libs.flapdoodle.embed.mongo)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
