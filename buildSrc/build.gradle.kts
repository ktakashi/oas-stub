plugins {
    // https://github.com/gradle/gradle/issues/20084
    id(libs.plugins.kotlin.jvm.get().pluginId) version libs.versions.kotlin
    id(libs.plugins.dokka.get().pluginId) version libs.versions.dokka
    id("java-gradle-plugin")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val kotlinVersion = libs.versions.kotlin.get()

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
    implementation(libs.dokka.gradle.plugin)
}

gradlePlugin {
    plugins {
        create("conventionsPlugin") {
            id = "io.github.ktakashi.oas.conventions"
            implementationClass = "io.github.ktakashi.oas.build.ConventionsPlugin"
        }
        create("deployedPlugin") {
            id = "io.github.ktakashi.oas.deployed"
            implementationClass = "io.github.ktakashi.oas.build.DeployedPlugin"
        }
    }
}
