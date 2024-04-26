plugins {
    `java-library`
    kotlin("kapt")
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.spring"
description = "OAS stub Spring Boot autoconfigure"

tasks.dokkaHtml {
    dependsOn(tasks.findByName("kaptKotlin"))
}

dependencies {
    api(project(":lib:oas-stub-web-api"))
    api(project(":app:spring:oas-stub-inmemory-storage-autoconfigure"))
    api(project(":app:spring:oas-stub-hazelcast-storage-autoconfigure"))
    api(project(":app:spring:oas-stub-mongodb-storage-autoconfigure"))
    implementation(libs.spring.boot.starter.core)
    implementation(libs.spring.boot.starter.aop)
    implementation(libs.spring.boot.starter.actuator)

    implementation(libs.xml.bind.api)
    implementation(libs.kotlin.stdlib)
    implementation(libs.projectreactor.reactor.core)
    implementation(libs.swagger.core.annotations.jakarta)

    kapt(libs.spring.boot.configuration.processor)
}