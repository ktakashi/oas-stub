plugins {
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.storage"
description = "OAS stub mongodb storage starter"

val springBootVersion by extra(property("spring-boot.version") as String)
val mongoDriverVersion by extra(property("mongodb.driver.version") as String)

dependencyManagement {
    dependencies {
        dependency("org.mongodb:mongodb-driver-sync:${mongoDriverVersion}")
    }
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${springBootVersion}") {
            bomProperties(mapOf("mongodb.version" to mongoDriverVersion))
        }
    }
}

dependencies {
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
