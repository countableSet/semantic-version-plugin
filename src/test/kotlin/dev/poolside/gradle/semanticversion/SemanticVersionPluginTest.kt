package dev.poolside.gradle.semanticversion

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SemanticVersionPluginTest {

    @TempDir
    lateinit var testProjectDir: File
    @TempDir
    lateinit var mavenRepo: File

    @Test
    fun `check test setup`() {
        val build = """
            plugins {
                java
                `maven-publish`
                id("dev.poolside.gradle.semantic-version")
            }
            group = "dev.poolside.test"
            version = "0.1"
            publishing {
                repositories {
                    maven {
                        url = uri("${mavenRepo.absolutePath}")
                    }
                }
                publications {
                    create<MavenPublication>("mavenJava") {
                        artifactId = "my-library"
                        from(components["java"])
                    }
                }
            }
        """.trimIndent()
        val settings = """rootProject.name = "testing""""
        File(testProjectDir, "build.gradle.kts").writeText(build)
        File(testProjectDir, "settings.gradle.kts").writeText(settings)
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir)
            .withArguments("publish")
            .build()
        val pomFile = testProjectDir.walk().filter { it.name.startsWith("pom") }.first()
        val pom = PomParser.parse(pomFile.absolutePath)
        println(pom.version)
    }
}
