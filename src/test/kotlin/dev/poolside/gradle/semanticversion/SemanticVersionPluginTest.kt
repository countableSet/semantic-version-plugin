package dev.poolside.gradle.semanticversion

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
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
        var pomFile = testProjectDir.walk().filter { it.name.startsWith("pom") }.first()
        var pom = PomParser.parse(pomFile.absolutePath)
        assertEquals("0.1.0", pom.version)
        var jarFile = mavenRepo.walk().filter { it.name.endsWith("jar") }.first()
        assertTrue(jarFile.absolutePath.endsWith("/dev/poolside/test/my-library/0.1.0/my-library-0.1.0.jar"))
        var publishedPom = mavenRepo.walk().filter { it.name.equals("my-library-0.1.0.pom") }.first()
        pom = PomParser.parse(publishedPom.absolutePath)
        assertEquals("0.1.0", pom.version)

        // should +1
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir)
            .withArguments("publish")
//            .withDebug(true)
            .build()
        pomFile = testProjectDir.walk().filter { it.name.startsWith("pom") }.last()
        pom = PomParser.parse(pomFile.absolutePath)
        assertEquals("0.1.1", pom.version)
        jarFile = mavenRepo.walk().filter { it.name.endsWith("jar") }.last()
        assertTrue(jarFile.absolutePath.endsWith("/dev/poolside/test/my-library/0.1.1/my-library-0.1.1.jar"))
        publishedPom = mavenRepo.walk().filter { it.name.equals("my-library-0.1.1.pom") }.last()
        pom = PomParser.parse(publishedPom.absolutePath)
        assertEquals("0.1.1", pom.version)
    }

    @Test
    fun `modules version is set correctly`() {
        val build = """
            plugins {
                java
                `maven-publish`
                id("dev.poolside.gradle.semantic-version")
            }
            repositories {
                maven { url = uri("${mavenRepo.absolutePath}") }
            }
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
            dependencies {
                implementation(project(":lib"))
            }
        """.trimIndent()
        FileManager.writeFile(folder = testProjectDir, filename = "build.gradle.kts", content = build)
        val settings = """
            rootProject.name = "testing"
            include("lib")
        """.trimIndent()
        FileManager.writeFile(folder = testProjectDir, filename = "settings.gradle.kts", content = settings)
        val properties = """
            group=dev.poolside.test
            version=0.1
        """.trimIndent()
        FileManager.writeFile(folder = testProjectDir, filename = "gradle.properties", content = properties)
        val libBuild = """
            plugins {
                `java-library`
                `maven-publish`
            }
            repositories {
                mavenCentral()
            }
            publishing {
                repositories {
                    maven { url = uri("${mavenRepo.absolutePath}") }
                }
                publications {
                    create<MavenPublication>("mavenJava") {
                        artifactId = "my-sublibrary"
                        from(components["java"])
                    }
                }
            }
            dependencies {
                api("org.apache.commons:commons-math3:3.6.1")
                implementation("com.google.guava:guava:30.1.1-jre")
            }
        """.trimIndent()
        FileManager.writeFile(folder = testProjectDir, path = "lib", filename = "build.gradle.kts", content = libBuild)
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir)
            .withArguments("publish")
//            .withDebug(true)
            .build()
        testProjectDir.walk().filter { it.name.startsWith("pom") }.forEach { pomFile ->
            pomFile.forEachLine { println(it) }
            val pom = PomParser.parse(pomFile.absolutePath)
            assertEquals("0.1.0", pom.version)
        }
        val valid = mutableListOf(
            "${mavenRepo.absolutePath}/dev/poolside/test/my-library/0.1.0/my-library-0.1.0.jar",
            "${mavenRepo.absolutePath}/dev/poolside/test/my-sublibrary/0.1.0/my-sublibrary-0.1.0.jar"
        )
        mavenRepo.walk().filter { it.name.endsWith(".jar") }.forEach {jarFile ->
            if (valid.contains(jarFile.absolutePath)) {
                valid.remove(jarFile.absolutePath)
            } else {
                fail("missing jarfile ${jarFile.absolutePath}")
            }
        }
        assertTrue(valid.isEmpty())
    }

    @Test
    fun `bom version set correct`() {
        val build = """
            plugins {
                `java-library`
                `maven-publish`
                id("dev.poolside.gradle.semantic-version")
            }
            repositories {
                maven { url = uri("${mavenRepo.absolutePath}") }
            }
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
            dependencies {
                api(platform(project(":bom")))
                api(project(":lib"))
            }
        """.trimIndent()
        FileManager.writeFile(folder = testProjectDir, filename = "build.gradle.kts", content = build)
        val settings = """
            rootProject.name = "testing"
            include("lib")
            include("bom")
        """.trimIndent()
        FileManager.writeFile(folder = testProjectDir, filename = "settings.gradle.kts", content = settings)
        val properties = """
            group=dev.poolside.test
            version=0.1
        """.trimIndent()
        FileManager.writeFile(folder = testProjectDir, filename = "gradle.properties", content = properties)
        val libBuild = """
            plugins {
                `java-library`
                `maven-publish`
            }
            repositories {
                mavenCentral()
            }
            publishing {
                repositories {
                    maven { url = uri("${mavenRepo.absolutePath}") }
                }
                publications {
                    create<MavenPublication>("mavenJava") {
                        artifactId = "my-sublibrary"
                        from(components["java"])
                    }
                }
            }
            dependencies {
                api("org.apache.commons:commons-math3:3.6.1")
                implementation("com.google.guava:guava:30.1.1-jre")
            }
        """.trimIndent()
        FileManager.writeFile(folder = testProjectDir, path = "lib", filename = "build.gradle.kts", content = libBuild)
        val bomBuild = """
            plugins {
                `java-platform`
                `maven-publish`
            }
            repositories {
                mavenCentral()
            }
            javaPlatform {
                allowDependencies()
            }
            dependencies {
                constraints {
                    api("commons-httpclient:commons-httpclient:3.1")
                    runtime("org.postgresql:postgresql:42.2.5")
                }
            }
            publishing {
                repositories {
                    maven { url = uri("${mavenRepo.absolutePath}") }
                }
                publications {
                    create<MavenPublication>("myPlatform") {
                        from(components["javaPlatform"])
                    }
                }
            }
        """.trimIndent()
        FileManager.writeFile(folder = testProjectDir, path = "bom", filename = "build.gradle.kts", content = bomBuild)
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir)
            .withArguments("publish")
//            .withDebug(true)
            .build()
        testProjectDir.walk().filter { it.name.startsWith("pom") }.forEach { pomFile ->
            pomFile.forEachLine { println(it) }
            val pom = PomParser.parse(pomFile.absolutePath)
            assertEquals("0.1.0", pom.version)
        }
        val valid = mutableListOf(
            "${mavenRepo.absolutePath}/dev/poolside/test/my-library/0.1.0/my-library-0.1.0.jar",
            "${mavenRepo.absolutePath}/dev/poolside/test/my-sublibrary/0.1.0/my-sublibrary-0.1.0.jar"
        )
        mavenRepo.walk().filter { it.name.endsWith(".jar") }.forEach {jarFile ->
            if (valid.contains(jarFile.absolutePath)) {
                valid.remove(jarFile.absolutePath)
            } else {
                fail("missing jarfile ${jarFile.absolutePath}")
            }
        }
        assertTrue(valid.isEmpty())
    }
}
