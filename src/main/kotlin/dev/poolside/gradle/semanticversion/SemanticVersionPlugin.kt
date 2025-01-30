package dev.poolside.gradle.semanticversion

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType

class SemanticVersionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("semanticVersion", SemanticVersionExtension::class.java)
        val pubExtension = project.extensions.getByType(PublishingExtension::class.java)
        project.tasks.register("semanticVersion", SemanticVersionTask::class.java) {
            this.description = "Determines and sets the semantic version"
            this.group = "publishing"
            this.manual = extension.manual
            this.extension = pubExtension
        }
        project.tasks.withType<JavaCompile> {
            this.dependsOn("semanticVersion")
        }
        project.tasks.withType<GenerateMavenPom> {
            this.dependsOn("semanticVersion")
        }
    }
}
