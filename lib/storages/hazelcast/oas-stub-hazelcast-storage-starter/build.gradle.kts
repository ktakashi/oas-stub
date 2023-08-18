plugins {
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.storage"
description = "OAS stub hazelcast storage starter"

val springBootVersion by extra(property("spring-boot.version") as String)
val hazelcastVersion by extra(property("hazelcast.version") as String)

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    }
    dependencies {
        dependency("com.hazelcast:hazelcast:${hazelcastVersion}")
    }
}

dependencies {
    implementation(project(":lib:oas-stub-plugin"))
    api(project(":lib:storages:oas-stub-storage-api"))
    api(project(":lib:storages:hazelcast:oas-stub-hazelcast-storage"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    api("com.hazelcast:hazelcast")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
