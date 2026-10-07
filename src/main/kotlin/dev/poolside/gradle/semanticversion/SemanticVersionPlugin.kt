package dev.poolside.gradle.semanticversion

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType
import java.io.File

class SemanticVersionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("semanticVersion", SemanticVersionExtension::class.java)
        val pubExtension = project.extensions.getByType(PublishingExtension::class.java)
        val pubTasks = project.tasks.withType(PublishToMavenRepository::class.java)
        project.tasks.register("semanticVersion", SemanticVersionTask::class.java) {
            this.description = "Determines and sets the semantic version"
            this.group = "publishing"
            this.manual = extension.manual
            this.extension = pubExtension
            this.tasks = pubTasks
        }
        project.tasks.withType<JavaCompile> {
            this.dependsOn("semanticVersion")
        }
        project.tasks.withType<GenerateMavenPom> {
            this.dependsOn("semanticVersion")
        }
        project.tasks.withType<GenerateModuleMetadata> {
            this.dependsOn("semanticVersion")
            doLast {
                rewriteModuleMetadata(outputFile.get().asFile, project)
            }
        }
        project.gradle.projectsEvaluated {
            val semanticVersionTasks = project.rootProject.allprojects.flatMap {
                it.tasks.withType(SemanticVersionTask::class.java).toList()
            }
            project.tasks.withType<GenerateMavenPom>().configureEach {
                dependsOn(semanticVersionTasks)
            }
            project.tasks.withType<GenerateModuleMetadata>().configureEach {
                dependsOn(semanticVersionTasks)
            }
        }
    }

    private fun rewriteModuleMetadata(file: File, project: Project) {
        val versions = project.rootProject.allprojects
            .asSequence()
            .mapNotNull { it.extensions.findByType(PublishingExtension::class.java) }
            .flatMap { it.publications.withType(MavenPublication::class.java).asSequence() }
            .associate { "${it.groupId}:${it.artifactId}" to it.version }
        val module = JsonSlurper().parse(file) as? Map<*, *> ?: return
        val rewritten = module.mapValues { (key, value) ->
            if (key == "variants") rewriteVariants(value, versions) else value
        }

        file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(rewritten)))
    }

    private fun rewriteVariants(variants: Any?, versions: Map<String, String>): Any? =
        (variants as? List<*>)?.map { variant ->
            if (variant !is Map<*, *>) return@map variant
            variant.mapValues { (key, value) ->
                if (key == "dependencies" || key == "dependencyConstraints") {
                    rewriteVersions(value, versions)
                } else {
                    value
                }
            }
        } ?: variants

    private fun rewriteVersions(entries: Any?, versions: Map<String, String>): Any? =
        (entries as? List<*>)?.map { entry ->
            if (entry !is Map<*, *>) return@map entry
            val key = "${entry["group"]}:${entry["module"]}"
            val publishedVersion = versions[key] ?: return@map entry
            entry.mapValues { (key, value) ->
                if (key == "version" && value is Map<*, *>) {
                    value + ("requires" to publishedVersion)
                } else {
                    value
                }
            }
        } ?: entries
}
