plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.spring"
description = "OAS stub mongodb storage starter"

tasks.withType<GenerateModuleMetadata> {
    suppressedValidationErrors.add("enforced-platform")
}

dependencies {
    annotationProcessor(enforcedPlatform(libs.spring.boot.dependencies))
    implementation(enforcedPlatform(libs.spring.boot.dependencies))
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.jackson.bom))
    api(project(":lib:storages:oas-stub-mongodb-storage"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
