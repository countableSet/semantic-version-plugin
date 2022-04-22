package dev.poolside.gradle.semanticversion

import org.gradle.api.Plugin
import org.gradle.api.Project

class HelloWorldPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.task("hello") {
            println("Hello from the GreetingPlugin")
//            doLast {
//                println("Hello from the GreetingPlugin")
//            }
        }
    }
}