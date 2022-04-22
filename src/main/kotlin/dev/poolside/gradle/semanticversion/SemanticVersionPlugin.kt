package dev.poolside.gradle.semanticversion

import org.gradle.api.Plugin
import org.gradle.api.Project

class SemanticVersionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.create("semanticVersion") {
            println("Hello from the GreetingPlugin")
//            doLast {
//                println("Hello from the GreetingPlugin")
//            }
        }
    }
}