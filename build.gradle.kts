plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.0"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.14.2")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
}

gradlePlugin {
    website.set("https://semantic-version.gradle.poolside.dev")
    vcsUrl.set("https://github.com/countableSet/semantic-version-plugin")

    plugins {
        create("semanticVersionPlugin") {
            id = "dev.poolside.gradle.semantic-version"
            group = "dev.poolside.gradle.semanticversion"
            implementationClass = "dev.poolside.gradle.semanticversion.SemanticVersionPlugin"
            version = "0.2.0"
            displayName = "Poolside Semantic Version Plugin"
            description = "Based on a given major.minor version, plugin determines patch version based on what is already " +
                    "maven repository by auto incrementing it to produce the next version number. Major or minor versions " +
                    "must be manually changed"
            tags.set(listOf("semantic version", "maven", "publish", "auto increment"))
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    }
}
