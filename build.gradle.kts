plugins {
    `java-gradle-plugin`
//    id("org.gradle.kotlin.kotlin-dsl") version "2.3.3"
    kotlin("jvm") version "1.6.21"
//    id("dev.poolside.gradle.semantic-version") version "0.1.0"
}

//apply<HelloWorldPlugin>()

//group = "dev.poolside.gradle.semantic-version"
//version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

gradlePlugin {
    plugins {
        create("semanticVersionPlugin") {
            id = "dev.poolside.gradle.semantic-version"
            implementationClass = "dev.poolside.gradle.semanticversion.HelloWorldPlugin"
            version = "0.1.0"
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}
