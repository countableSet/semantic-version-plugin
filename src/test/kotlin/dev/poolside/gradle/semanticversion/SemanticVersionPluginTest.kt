package dev.poolside.gradle.semanticversion

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SemanticVersionPluginTest {

    @TempDir
    lateinit var testProjectDir: File
    @TempDir
    lateinit var mavenRepo: File

    @Test
    fun `project version set correctly`() {
        val build = """
            plugins {
                java
                `maven-publish`
                id("dev.poolside.gradle.semantic-version")
            }
            repositories {
                maven { url = uri("${mavenRepo.absolutePath}") }
            }
            group = "dev.poolside.test"
            version = "0.1"
            publishing {
                repositories {
                    maven { url = uri("${mavenRepo.absolutePath}") }
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
//            .withDebug(true)
            .build()
        val pomFile = testProjectDir.walk().filter { it.name.startsWith("pom") }.first()
        var pom = PomParser.parse(pomFile.absolutePath)
        assertEquals("0.1.0", pom.version)
        val jarFile = mavenRepo.walk().filter { it.name.endsWith("jar") }.first()
        assertTrue(jarFile.absolutePath.endsWith("/dev/poolside/test/my-library/0.1.0/my-library-0.1.0.jar"))
        val publishedPom = mavenRepo.walk().filter { it.name.equals("my-library-0.1.0.pom") }.first()
        pom = PomParser.parse(publishedPom.absolutePath)
        assertEquals("0.1.0", pom.version)
    }
}
