plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "1.6.21"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.13.2")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
}

gradlePlugin {
    plugins {
        create("semanticVersionPlugin") {
            id = "dev.poolside.gradle.semantic-version"
            implementationClass = "dev.poolside.gradle.semanticversion.SemanticVersionPlugin"
            version = "0.1.0"
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}
