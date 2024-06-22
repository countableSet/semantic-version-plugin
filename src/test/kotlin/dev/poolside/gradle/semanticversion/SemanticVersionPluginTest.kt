package dev.poolside.gradle.semanticversion

import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.DefaultVersionComparator
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.Version
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.util.stream.Stream

class SemanticVersionPluginTest {

    companion object {
        @JvmStatic
        private fun gradleVersions(): Stream<Arguments> = Stream.of(
            Arguments.of("7.4.2"),
            Arguments.of("7.5.1"),
            Arguments.of("7.6.1"),
            Arguments.of("8.1"),
            Arguments.of("8.8")
        )
    }

    @TempDir
    lateinit var testProjectDir: File
    @TempDir
    lateinit var mavenRepo: File

    @ParameterizedTest(name = "{index} gradle version {0}")
    @MethodSource("gradleVersions")
    fun `project version set correctly`(gradleVersion: String) {
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
            .withGradleVersion(gradleVersion)
            .withArguments("publish")
//            .withDebug(true)
            .build()
        var pomFile = testProjectDir.walk().filter { it.name.startsWith("pom") }.first()
        var pom = PomParser.parse(pomFile.absolutePath)
        assertEquals("0.1.0", pom.version)
        var jarFile = mavenRepo.walk().filter { it.name.endsWith("my-library-0.1.0.jar") }.first()
        assertTrue(jarFile.absolutePath.endsWith("/dev/poolside/test/my-library/0.1.0/my-library-0.1.0.jar"), jarFile.absolutePath)
        var publishedPom = mavenRepo.walk().filter { it.name.equals("my-library-0.1.0.pom") }.first()
        pom = PomParser.parse(publishedPom.absolutePath)
        assertEquals("0.1.0", pom.version)

        // should +1
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir)
            .withGradleVersion(gradleVersion)
            .withArguments("publish")
//            .withDebug(true)
            .build()
        pomFile = testProjectDir.walk().filter { it.name.startsWith("pom") }.last()
        pom = PomParser.parse(pomFile.absolutePath)
        assertEquals("0.1.1", pom.version)
        jarFile = mavenRepo.walk().filter { it.name.endsWith("my-library-0.1.1.jar") }.last()
        assertTrue(jarFile.absolutePath.endsWith("/dev/poolside/test/my-library/0.1.1/my-library-0.1.1.jar"), jarFile.absolutePath)
        publishedPom = mavenRepo.walk().filter { it.name.equals("my-library-0.1.1.pom") }.last()
        pom = PomParser.parse(publishedPom.absolutePath)
        assertEquals("0.1.1", pom.version)
    }

    @ParameterizedTest(name = "{index} gradle version {0}")
    @MethodSource("gradleVersions")
    fun `minor version bump`(gradleVersion: String) {
        var build = """
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
            .withGradleVersion(gradleVersion)
            .withArguments("publish")
//            .withDebug(true)
            .build()
        var version = "0.1.0"
        var pomFile = testProjectDir.walk().filter { it.name.startsWith("pom") }.first()
        var pom = PomParser.parse(pomFile.absolutePath)
        assertEquals(version, pom.version)
        var jarFile = mavenRepo.walk().filter { it.name.endsWith("my-library-$version.jar") }.first()
        assertTrue(jarFile.absolutePath.endsWith("/dev/poolside/test/my-library/$version/my-library-$version.jar"), jarFile.absolutePath)
        var publishedPom = mavenRepo.walk().filter { it.name.equals("my-library-$version.pom") }.first()
        pom = PomParser.parse(publishedPom.absolutePath)
        assertEquals(version, pom.version)

        build = """
            plugins {
                java
                `maven-publish`
                id("dev.poolside.gradle.semantic-version")
            }
            repositories {
                maven { url = uri("${mavenRepo.absolutePath}") }
            }
            group = "dev.poolside.test"
            version = "0.2"
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
        File(testProjectDir, "build.gradle.kts").writeText(build)

        // should +0
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir)
            .withGradleVersion(gradleVersion)
            .withArguments("publish")
//            .withDebug(true)
            .build()
        version = "0.2.0"
        pomFile = testProjectDir.walk().filter { it.name.startsWith("pom") }.first()
        pom = PomParser.parse(pomFile.absolutePath)
        assertEquals(version, pom.version)
        jarFile = mavenRepo.walk().filter { it.name.endsWith("my-library-$version.jar") }.first()
        assertTrue(jarFile.absolutePath.endsWith("/dev/poolside/test/my-library/$version/my-library-$version.jar"), jarFile.absolutePath)
        publishedPom = mavenRepo.walk().filter { it.name.equals("my-library-$version.pom") }.first()
        pom = PomParser.parse(publishedPom.absolutePath)
        assertEquals(version, pom.version)
    }

    @ParameterizedTest(name = "{index} gradle version {0}")
    @MethodSource("gradleVersions")
    fun `major version bump`(gradleVersion: String) {
        var build = """
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
            .withGradleVersion(gradleVersion)
            .withArguments("publish")
//            .withDebug(true)
            .build()
        var version = "0.1.0"
        var pomFile = testProjectDir.walk().filter { it.name.startsWith("pom") }.first()
        var pom = PomParser.parse(pomFile.absolutePath)
        assertEquals(version, pom.version)
        var jarFile = mavenRepo.walk().filter { it.name.endsWith("my-library-$version.jar") }.first()
        assertTrue(jarFile.absolutePath.endsWith("/dev/poolside/test/my-library/$version/my-library-$version.jar"), jarFile.absolutePath)
        var publishedPom = mavenRepo.walk().filter { it.name.equals("my-library-$version.pom") }.first()
        pom = PomParser.parse(publishedPom.absolutePath)
        assertEquals(version, pom.version)

        build = """
            plugins {
                java
                `maven-publish`
                id("dev.poolside.gradle.semantic-version")
            }
            repositories {
                maven { url = uri("${mavenRepo.absolutePath}") }
            }
            group = "dev.poolside.test"
            version = "1.0"
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
        File(testProjectDir, "build.gradle.kts").writeText(build)

        // should +0
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir)
            .withGradleVersion(gradleVersion)
            .withArguments("publish")
//            .withDebug(true)
            .build()
        version = "1.0.0"
        pomFile = testProjectDir.walk().filter { it.name.startsWith("pom") }.first()
        pom = PomParser.parse(pomFile.absolutePath)
        assertEquals(version, pom.version)
        jarFile = mavenRepo.walk().filter { it.name.endsWith("my-library-$version.jar") }.first()
        assertTrue(jarFile.absolutePath.endsWith("/dev/poolside/test/my-library/$version/my-library-$version.jar"), jarFile.absolutePath)
        publishedPom = mavenRepo.walk().filter { it.name.equals("my-library-$version.pom") }.first()
        pom = PomParser.parse(publishedPom.absolutePath)
        assertEquals(version, pom.version)
    }

    @ParameterizedTest(name = "{index} gradle version {0}")
    @MethodSource("gradleVersions")
    fun `modules version is set correctly`(gradleVersion: String) {
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
            .withGradleVersion(gradleVersion)
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
        mavenRepo.walk().filter { it.name.endsWith(".jar") }.forEach { jarFile ->
            if (valid.contains(jarFile.absolutePath)) {
                valid.remove(jarFile.absolutePath)
            } else {
                fail("missing jarfile ${jarFile.absolutePath}")
            }
        }
        assertTrue(valid.isEmpty())
    }

    @ParameterizedTest(name = "{index} gradle version {0}")
    @MethodSource("gradleVersions")
    fun `subproject version is set correctly`(gradleVersion: String) {
        val build = """
            plugins {
                `java-library`
                `maven-publish`
                id("dev.poolside.gradle.semantic-version")
            }
            allprojects {
                apply(plugin = "java-library")
                apply(plugin = "maven-publish")
                repositories {
                    mavenCentral()
                    maven { url = uri("${mavenRepo.absolutePath}") }
                }
                java {
                    group = "dev.poolside.test"
                    version = "0.1"
                }
                publishing {
                    repositories {
                        maven { url = uri("${mavenRepo.absolutePath}") }
                    }
                }
            }
        """.trimIndent()
        FileManager.writeFile(folder = testProjectDir, filename = "build.gradle.kts", content = build)
        val settings = """
            rootProject.name = "testing"
            include("lib")
        """.trimIndent()
        FileManager.writeFile(folder = testProjectDir, filename = "settings.gradle.kts", content = settings)
        val libBuild = """
            publishing {
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
            .withGradleVersion(gradleVersion)
            .withArguments("publish")
//            .withDebug(true)
            .build()
        testProjectDir.walk().filter { it.name.startsWith("pom") }.forEach { pomFile ->
            pomFile.forEachLine { println(it) }
            val pom = PomParser.parse(pomFile.absolutePath)
            assertEquals("0.1.0", pom.version)
        }
        val valid = mutableListOf(
            "${mavenRepo.absolutePath}/dev/poolside/test/my-sublibrary/0.1.0/my-sublibrary-0.1.0.jar"
        )
        mavenRepo.walk().filter { it.name.endsWith(".jar") }.forEach { jarFile ->
            if (valid.contains(jarFile.absolutePath)) {
                valid.remove(jarFile.absolutePath)
            } else {
                fail("missing jarfile ${jarFile.absolutePath}")
            }
        }
        assertTrue(valid.isEmpty())
    }

    @ParameterizedTest(name = "{index} gradle version {0}")
    @MethodSource("gradleVersions")
    fun `two subprojects version is set correctly`(gradleVersion: String) {
        val build = """
            plugins {
                `java-library`
                `maven-publish`
                id("dev.poolside.gradle.semantic-version")
            }
            allprojects {
                apply(plugin = "java-library")
                apply(plugin = "maven-publish")
                repositories {
                    mavenCentral()
                    maven { url = uri("${mavenRepo.absolutePath}") }
                }
                java {
                    group = "dev.poolside.test"
                    version = "0.1"
                }
                publishing {
                    repositories {
                        maven { url = uri("${mavenRepo.absolutePath}") }
                    }
                }
            }
        """.trimIndent()
        FileManager.writeFile(folder = testProjectDir, filename = "build.gradle.kts", content = build)
        val settings = """
            rootProject.name = "testing"
            include("lib")
            include("lib2")
        """.trimIndent()
        FileManager.writeFile(folder = testProjectDir, filename = "settings.gradle.kts", content = settings)
        val libBuild = """
            publishing {
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
        val lib2Build = """
            publishing {
                publications {
                    create<MavenPublication>("mavenJava") {
                        artifactId = "my-sublibrary2"
                        from(components["java"])
                    }
                }
            }
            dependencies {
                api("org.apache.commons:commons-math3:3.6.1")
                implementation("com.google.guava:guava:30.1.1-jre")
            }
        """.trimIndent()
        FileManager.writeFile(folder = testProjectDir, path = "lib2", filename = "build.gradle.kts", content = lib2Build)
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir)
            .withGradleVersion(gradleVersion)
            .withArguments("publish")
//            .withDebug(true)
            .build()
        testProjectDir.walk().filter { it.name.startsWith("pom") }.forEach { pomFile ->
            pomFile.forEachLine { println(it) }
            val pom = PomParser.parse(pomFile.absolutePath)
            assertEquals("0.1.0", pom.version)
        }
        val valid = mutableListOf(
            "${mavenRepo.absolutePath}/dev/poolside/test/my-sublibrary/0.1.0/my-sublibrary-0.1.0.jar",
            "${mavenRepo.absolutePath}/dev/poolside/test/my-sublibrary2/0.1.0/my-sublibrary2-0.1.0.jar"
        )
        mavenRepo.walk().filter { it.name.endsWith(".jar") }.forEach { jarFile ->
            if (valid.contains(jarFile.absolutePath)) {
                valid.remove(jarFile.absolutePath)
            } else {
                fail("missing jarfile ${jarFile.absolutePath}")
            }
        }
        assertTrue(valid.isEmpty())
    }

    @ParameterizedTest(name = "{index} gradle version {0}")
    @MethodSource("gradleVersions")
    fun `bom version set correct`(gradleVersion: String) {
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
            .withGradleVersion(gradleVersion)
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
        mavenRepo.walk().filter { it.name.endsWith(".jar") }.forEach { jarFile ->
            if (valid.contains(jarFile.absolutePath)) {
                valid.remove(jarFile.absolutePath)
            } else {
                fail("missing jarfile ${jarFile.absolutePath}")
            }
        }
        assertTrue(valid.isEmpty())
    }

    @ParameterizedTest(name = "{index} invalid version {0}")
    @ValueSource(strings = ["0.1.0", "1", "10.0.0-SNAPSHOT", "1.0-SNAPSHOT", "SNAPSHOT"])
    fun `invalid version`(version: String) {
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
            version = "$version"
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
        val exception = assertThrows<UnexpectedBuildFailure> {
            GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir)
                .withArguments("publish")
//            .withDebug(true)
                .build()
        }
        val result = exception.buildResult
        assertTrue(result.output.contains("Invalid version, must be in format ^\\d+\\.\\d+\$"))
        assertEquals(FAILED, result.task(":semanticVersion")?.outcome)
    }

    @Test
    fun `version parser`() {
        val versionParser = VersionParser()
        val versionComparator = DefaultVersionComparator().asVersionComparator()
        val versions = listOf("0.1.0", "0.1.1", "0.1.2")
        var latestVersion: Version? = null
        versions.map { version -> versionParser.transform(version) }.forEach { version ->
            if (latestVersion == null || versionComparator.compare(version, latestVersion) > 0) {
                latestVersion = version
            }
        }
        assertEquals("0.1.2", latestVersion.toString())
    }

    @ParameterizedTest(name = "{index} gradle version {0}")
    @MethodSource("gradleVersions")
    fun `manual versioning`(gradleVersion: String) {
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
            version = "0.1.0"
            semanticVersion {
                manual = true
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
        """.trimIndent()
        val settings = """rootProject.name = "testing""""
        File(testProjectDir, "build.gradle.kts").writeText(build)
        File(testProjectDir, "settings.gradle.kts").writeText(settings)
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir)
            .withGradleVersion(gradleVersion)
            .withArguments("publish")
//            .withDebug(true)
            .build()
        var pomFile = testProjectDir.walk().filter { it.name.startsWith("pom") }.first()
        var pom = PomParser.parse(pomFile.absolutePath)
        assertEquals("0.1.0", pom.version)
        val jarFile = mavenRepo.walk().filter { it.name.endsWith("my-library-0.1.0.jar") }.first()
        assertTrue(jarFile.absolutePath.endsWith("/dev/poolside/test/my-library/0.1.0/my-library-0.1.0.jar"), jarFile.absolutePath)
        val publishedPom = mavenRepo.walk().filter { it.name.equals("my-library-0.1.0.pom") }.first()
        pom = PomParser.parse(publishedPom.absolutePath)
        assertEquals("0.1.0", pom.version)

        // should skip
        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir)
            .withGradleVersion(gradleVersion)
            .withArguments("publish")
//            .withDebug(true)
            .build()
        assertEquals(1, result.taskPaths(TaskOutcome.SKIPPED).size)
        pomFile = testProjectDir.walk().filter { it.name.startsWith("pom") }.last()
        pom = PomParser.parse(pomFile.absolutePath)
        assertEquals("0.1.0", pom.version)
        val jarFiles = mavenRepo.walk().filter { it.name.endsWith("my-library-0.1.1.jar") }.toList()
        assertTrue(jarFiles.isEmpty())
    }
}
