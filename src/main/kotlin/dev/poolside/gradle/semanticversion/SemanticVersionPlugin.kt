package dev.poolside.gradle.semanticversion

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

class SemanticVersionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.create("semanticVersion", PublishToMavenRepository::class.java) {
            println("Hello from the GreetingPlugin")
//            doLast {
//                println("Hello from the GreetingPlugin")
//            }
        }
    }
}