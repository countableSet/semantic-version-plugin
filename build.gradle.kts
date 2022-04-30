plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.9.0"
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
            group = "dev.poolside.gradle.semanticversion"
            implementationClass = "dev.poolside.gradle.semanticversion.SemanticVersionPlugin"
            version = "0.1.0"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

pluginBundle {
    website = "https://github.com/johndoe/GradlePlugins"
    vcsUrl = "https://github.com/countableSet/semantic-version-plugin"
    description = "Based on a given major.minor version, plugin determines patch version based on what is already " +
            "maven repository by auto incrementing it to produce the next version number. Major or minor versions " +
            "must be manually changed"
    tags = listOf("semantic version", "maven", "publish", "auto increment")
    (plugins) {
        getByName("semanticVersionPlugin") {
            displayName = "Poolside Semantic Version"
        }
    }
}
