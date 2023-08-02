plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "$group.storage"
description = "OAS stub storage APIs"

dependencies {
    implementation(project(":lib:oas-stub-plugin"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}
