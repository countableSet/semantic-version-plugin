package dev.poolside.gradle.semanticversion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PomParserTest {

    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `parses dependencies`() {
        val pomFile = File(testProjectDir, "pom.xml")
        pomFile.writeText(
            """
            <project>
                <groupId>dev.poolside.test</groupId>
                <artifactId>my-library</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.commons</groupId>
                        <artifactId>commons-math3</artifactId>
                        <version>3.6.1</version>
                    </dependency>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.1.1-jre</version>
                    </dependency>
                </dependencies>
            </project>
            """.trimIndent()
        )

        val pom = PomParser.parse(pomFile.absolutePath)

        assertEquals(
            listOf(
                Dependency("org.apache.commons", "commons-math3", "3.6.1"),
                Dependency("com.google.guava", "guava", "30.1.1-jre")
            ),
            pom.dependencies?.dependency
        )
    }
}
