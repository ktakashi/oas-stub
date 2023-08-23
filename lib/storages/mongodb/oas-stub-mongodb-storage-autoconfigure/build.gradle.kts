plugins {
    `java-library`
    `maven-publish`
    signing
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.storage"
description = "OAS stub mongodb storage starter"

dependencies {
    annotationProcessor(enforcedPlatform(libs.spring.boot.dependencies))
    implementation(enforcedPlatform(libs.spring.boot.dependencies))
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.jackson.bom))
    implementation(project(":lib:oas-stub-plugin"))
    api(project(":lib:storages:oas-stub-storage-api"))
    api(project(":lib:storages:mongodb:oas-stub-mongodb-storage"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
