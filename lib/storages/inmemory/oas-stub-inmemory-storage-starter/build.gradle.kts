plugins {
    alias(libs.plugins.kotlin.spring)
}

group = "$group.storage"
description = "OAS stub in-memory storage starter"

val springBootVersion by extra(property("spring-boot.version") as String)

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    }
}

dependencies {
    implementation(project(":lib:oas-stub-plugin"))
    implementation(project(":lib:storages:oas-stub-storage-api"))
    implementation(project(":lib:storages:inmemory:oas-stub-inmemory-storage"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}
