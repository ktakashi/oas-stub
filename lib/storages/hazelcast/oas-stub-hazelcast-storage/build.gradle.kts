plugins {
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.storage"
description = "OAS stub hazelcast storage"

val hazelcastVersion by extra(property("hazelcast.version") as String)

dependencyManagement {
    dependencies {
        dependency("com.hazelcast:hazelcast:${hazelcastVersion}")
    }
}

dependencies {
    implementation(project(":lib:oas-stub-model"))
    implementation(project(":lib:oas-stub-plugin"))
    implementation(project(":lib:storages:oas-stub-storage-api"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    api("com.hazelcast:hazelcast")
}
