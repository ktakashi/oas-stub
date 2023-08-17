plugins {
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.model"
description = "OAS stub model"

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations")
}
