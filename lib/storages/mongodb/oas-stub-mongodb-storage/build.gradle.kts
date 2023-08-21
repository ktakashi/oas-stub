plugins {
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.storage"
description = "OAS stub mongodb storage"

val mongoDriverVersion by extra(property("mongodb.version") as String)

dependencyManagement {
    dependencies {
        dependency("org.mongodb:mongodb-driver-sync:${mongoDriverVersion}")
    }
}

dependencies {
    implementation(project(":lib:oas-stub-model"))
    implementation(project(":lib:oas-stub-plugin"))
    implementation(project(":lib:storages:oas-stub-storage-api"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    api("org.mongodb:mongodb-driver-sync")
}
