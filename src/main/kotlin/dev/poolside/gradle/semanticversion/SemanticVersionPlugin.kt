package dev.poolside.gradle.semanticversion

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType

class SemanticVersionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("semanticVersion", SemanticVersionTask::class.java) {
            this.description = "Determines and sets the semantic version"
        }
        project.tasks.withType<JavaCompile> {
            this.dependsOn("semanticVersion")
        }
        project.tasks.withType<GenerateMavenPom> {
            this.dependsOn("semanticVersion")
        }
    }
}