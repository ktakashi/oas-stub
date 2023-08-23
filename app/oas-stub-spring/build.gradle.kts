import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
    alias(libs.plugins.spring.boot)
}

group = "$group.spring"
description = "OAS stub Spring Boot application"

tasks.named<BootJar>("bootJar") {
    archiveClassifier.set("standalone")
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("")
}

tasks.withType<GenerateModuleMetadata> {
     suppressedValidationErrors.add("enforced-platform")
}

dependencies {
    annotationProcessor(enforcedPlatform(libs.spring.boot.dependencies))
    implementation(enforcedPlatform(libs.spring.boot.dependencies))
    implementation(enforcedPlatform(libs.spring.cloud.dependencies))
    implementation(enforcedPlatform(libs.projectreactor.bom))
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.jackson.bom))

    api(project(":lib:oas-stub-engine"))
    implementation(project(":lib:storages:inmemory:oas-stub-inmemory-storage-autoconfigure"))
    implementation(project(":lib:storages:hazelcast:oas-stub-hazelcast-storage-autoconfigure"))
    implementation(project(":lib:storages:mongodb:oas-stub-mongodb-storage-autoconfigure"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("io.projectreactor:reactor-core")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation(libs.swagger.core.annotations.jakarta)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.mongodb.driver.sync)

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation(enforcedPlatform(libs.cucumber.bom))
    testImplementation(platform(libs.junit.bom))
    testImplementation(project(":lib:storages:oas-stub-storage-api"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("io.cucumber:cucumber-spring")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.rest.assured)
    testImplementation(libs.awaitility)
    testImplementation(libs.flapdoodle.embed.mongo)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
