plugins {
    `java-gradle-plugin`
//    id("org.gradle.kotlin.kotlin-dsl") version "2.3.3"
    kotlin("jvm") version "1.6.21"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
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
