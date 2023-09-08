plugins {
    `java-library`
    kotlin("kapt")
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.spring"
description = "OAS stub Spring Boot starter"

dependencies {
    api(project(":lib:oas-stub-web"))
    api(project(":app:spring:oas-stub-inmemory-storage-autoconfigure"))
    implementation(libs.spring.boot.starter.core)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.aop)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.jersey) {
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "com.fasterxml.jackson.module")
    }
    implementation(libs.jackson.module.jakarta.xmlbind.annotations) {
        exclude(group = "jakarta.xml.bind")
    }
    implementation(libs.xml.bind.api)
    implementation(libs.kotlin.stdlib)
    implementation(libs.projectreactor.reactor.core)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.swagger.core.annotations.jakarta)
    implementation(libs.swagger.core.jaxrs2.jakarta)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    kapt(libs.spring.boot.configuration.processor)
}
