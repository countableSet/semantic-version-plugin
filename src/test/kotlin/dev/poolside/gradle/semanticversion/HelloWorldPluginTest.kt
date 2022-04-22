package dev.poolside.gradle.semanticversion

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class HelloWorldPluginTest {

    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `check test setup`() {
        val build = """
            plugins {
                id("dev.poolside.gradle.semantic-version")
            }
        """.trimIndent()
        val settings = """rootProject.name = "testing""""
        File(testProjectDir, "build.gradle.kts").writeText(build)
        File(testProjectDir, "settings.gradle.kts").writeText(settings)
        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir)
            .withArguments("hello")
            .build()
        println(result.output)
    }
}
