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
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class ConventionsPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        configureJavaConventions(project)
        configureKotlinConventions(project)
        configureMavenPublishingConventions(project)
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
        project.tasks.register("dokkaJavadocJar", Jar::class.java) { jar ->
            jar.archiveClassifier.set("javadoc")
            val dokkaJavadoc = project.tasks.named("dokkaJavadoc")
            jar.dependsOn(dokkaJavadoc)
            jar.from(dokkaJavadoc.flatMap { task -> (task as DokkaTask).outputDirectory })
        }
        project.tasks.withType(KotlinCompile::class.java) { task ->
            task.kotlinOptions.apply {
                freeCompilerArgs = listOf("-Xjvm-default=all")
            }
        }
        project.extensions.getByType(KotlinJvmProjectExtension::class.java).jvmToolchain(17)
    }
}

internal fun configureMavenPublishingConventions(project: Project) {
    project.plugins.withType(MavenPublishPlugin::class.java) { _ ->
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        if (project.hasProperty("deploymentRepository")) {
            publishing.repositories.maven { mavenRepository ->
                mavenRepository.url = URI.create(project.property("deploymentRepository") as String)
                mavenRepository.name = "deployment"
            }
        }
        publishing.publications.create("mavenJava", MavenPublication::class.java)
        publishing.publications.withType(MavenPublication::class.java).all { publication ->
            val component = project.components.findByName("java")
            publication.from(component)
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
