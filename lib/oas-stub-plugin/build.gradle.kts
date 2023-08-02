plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "$group.plugins"
description = "OAS stub plugin APIs"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("jakarta.servlet:jakarta.servlet-api")
}
