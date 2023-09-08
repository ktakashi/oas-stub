package io.github.ktakashi.oas.build

import java.net.URI
import java.util.TreeMap
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask

class ConventionsPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        configureJavaConventions(project)
        configureKotlinConventions(project)
        configureMavenPublishingConventions(project)
        configureSigningConventions(project)
    }
}

internal fun configureJavaConventions(project: Project) {
    project.plugins.withType(JavaLibraryPlugin::class.java) { _ ->
        configureJavaManifestConventions(project)
        project.tasks.named("test", Test::class.java) {
            it.useJUnitPlatform()
        }
    }
}

internal fun configureKotlinConventions(project: Project) {
    project.plugins.withId("org.jetbrains.kotlin.jvm") { _ ->
        project.plugins.apply(DokkaPlugin::class.java)
        project.tasks.register("dokkaJavadocJar", Jar::class.java) { jar ->
            jar.archiveClassifier.set("javadoc")
            val dokkaJavadoc = project.tasks.named("dokkaJavadoc")
            jar.dependsOn(dokkaJavadoc)
            jar.from(dokkaJavadoc.flatMap { task -> (task as DokkaTask).outputDirectory })
        }
        project.plugins.withId("org.jetbrains.kotlin.kapt") {
            project.tasks.withType(KaptWithoutKotlincTask::class.java) { kaptKotlin ->
                val dokkaJavadoc = project.tasks.named("dokkaJavadoc")
                dokkaJavadoc.configure { it.dependsOn(kaptKotlin) }
            }
        }

        project.tasks.withType(KotlinCompile::class.java) { task ->
            (task.kotlinOptions as KotlinJvmOptions).apply {
                freeCompilerArgs += listOf("-Xjvm-default=all", "-Xjsr305=strict")
                apiVersion = "1.9"
                languageVersion = "1.9"
                jvmTarget = "17"
            }
        }
        project.extensions.getByType(KotlinJvmProjectExtension::class.java).jvmToolchain(17)
    }
}

internal fun configureMavenPublishingConventions(project: Project) {
    project.plugins.withType(MavenPublishPlugin::class.java) { _ ->
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        if (project.hasProperty("deployment.repository")) {
            publishing.repositories.maven { mavenRepository ->
                mavenRepository.url = URI.create(project.property("deployment.repository") as String)
                mavenRepository.name = "deployment"
                mavenRepository.credentials.apply {
                    if (project.hasProperty("deployment.username")) {
                        username = project.property("deployment.username") as String
                    }
                    if (project.hasProperty("deployment.password")) {
                        password = project.property("deployment.password") as String
                    }
                }
            }
        }
        publishing.publications.withType(MavenPublication::class.java).all { publication ->
            publication.artifact(project.tasks.getByName("dokkaJavadocJar"))
            publication.pom.apply {
                name.set(project.provider(project::getName))
                description.set(project.provider(project::getDescription))
                url.set("https://github.com/ktakashi/oas-stub")
                licenses { licence ->
                    licence.license {
                        it.name.set("Apache License, Version 2.0");
                        it.url.set("https://www.apache.org/licenses/LICENSE-2.0");
                    }
                }
                developers { developer ->
                    developer.developer {
                        it.id.set("ktakashi")
                        it.name.set("Takashi Kato")
                        it.email.set("ktakashi@ymail.com")
                    }
                }
                scm { scm ->
                    scm.url.set("https://github.com/ktakashi/oas-stub")
                    scm.connection.set("scm:git:https://github.com/ktakashi/oas-stub")
                }
                issueManagement { issueManagement ->
                    issueManagement.system.set("GitHub")
                    issueManagement.url.set("https://github.com/ktakashi/oas-stub/issues")
                }
            }

            project.plugins.withType(JavaLibraryPlugin::class.java) { _ ->
                project.extensions.getByType(JavaPluginExtension::class.java).apply {
                    withSourcesJar()
                }
            }
        }
    }
}

private fun configureSigningConventions(project: Project) {
    project.plugins.withType(SigningPlugin::class.java) {
        if (project.hasProperty("signing.key") && project.hasProperty("signing.password")) {
            val signing = project.extensions.getByType(SigningExtension::class.java)
            if (project.hasProperty("signing.keyId")) {
                val keyId = project.property("signing.keyId") as String
                val key = project.property("signing.key") as String
                val password = project.property("signing.password") as String
                signing.useInMemoryPgpKeys(keyId, key, password)
            }
            val publishing = project.extensions.getByType(PublishingExtension::class.java)
            publishing.publications.matching { p -> p.name == MAVEN_PUBLICATION_NAME }
                    .all(signing::sign)
        }
    }
}

private fun configureJavaManifestConventions(project: Project) {
    fun determineImplementationTitle(project: Project, sourceJarTaskNames: Set<String>, javadocJarTaskNames: Set<String>, jar: Jar): String {
        if (sourceJarTaskNames.contains(jar.name)) {
            return "Source for ${project.name}"
        }
        if (javadocJarTaskNames.contains(jar.name)) {
            return "Javadoc for ${project.name}"
        }
        return project.description ?: "OAS stub"
    }
    val sourceSet = project.extensions.getByType(SourceSetContainer::class.java)
    val sourceJarTaskNames = sourceSet.map(SourceSet::getSourcesJarTaskName).toSet()
    val javadocJarTaskNames = sourceSet.map(SourceSet::getJavadocJarTaskName).toSet()
    project.tasks.withType(Jar::class.java) { jar ->
        project.afterEvaluate { _ ->
            jar.manifest { manifest ->
                val attributes = TreeMap<String, Any>()
                attributes["Created-By"] = "${System.getProperties()["java.version"]} (${System.getProperties()["java.vendor"]} ${System.getProperties()["java.vm.version"]})"
                attributes["Implementation-Title"] = determineImplementationTitle(project, sourceJarTaskNames, javadocJarTaskNames, jar)
                attributes["Implementation-Version"] = project.version
                manifest.attributes(attributes)
            }
        }
    }
}
