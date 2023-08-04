plugins {
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.storage"
description = "OAS stub in-memory storage"

dependencies {
    implementation(project(":lib:oas-stub-plugin"))
    implementation(project(":lib:storages:oas-stub-storage-api"))
    implementation("jakarta.inject:jakarta.inject-api")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}
