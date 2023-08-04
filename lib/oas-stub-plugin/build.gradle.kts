plugins {
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.plugins"
description = "OAS stub plugin APIs"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("jakarta.servlet:jakarta.servlet-api")
}
