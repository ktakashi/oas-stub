import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    `java-library`
    `maven-publish`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    alias(libs.plugins.spring.boot)
}

description = "OAS stub Spring Boot application"
val springBootVersion by extra(property("spring-boot.version") as String)

dependencyManagement {
    dependencies {
        dependency("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
    }
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2022.0.4")
        mavenBom("io.projectreactor:reactor-bom:2022.0.9")
    }
}

tasks.named<BootJar>("bootJar") {
    archiveClassifier.set("standalone")
}

dependencies {
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
    implementation("io.swagger.core.v3:swagger-annotations")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")
    implementation("org.mongodb:mongodb-driver-sync")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation(project(":lib:storages:oas-stub-storage-api"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("io.cucumber:cucumber-spring")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo:4.8.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
