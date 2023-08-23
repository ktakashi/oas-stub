package io.github.ktakashi.oas.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlatformPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.signing.SigningPlugin

const val MAVEN_PUBLICATION_NAME = "maven"

// Idea from Spring Boot
class DeployedPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(MavenPublishPlugin::class.java)
        project.plugins.apply(SigningPlugin::class.java)
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        val mavenPublication = publishing.publications.create(MAVEN_PUBLICATION_NAME, MavenPublication::class.java)
        project.afterEvaluate {
            project.plugins.withType(JavaPlugin::class.java).all {
                if ((project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar).enabled) {
                    project.components.matching { component -> component.name == "java" }
                            .all(mavenPublication::from)
                }
            }
            project.plugins.withType(JavaPlatformPlugin::class.java).all {
                project.components.matching { component -> component.name == "javaPlatform" }
                        .all(mavenPublication::from)
            }
        }
    }

}
