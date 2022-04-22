package dev.poolside.gradle.semanticversion

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

object PomParser {
    fun parse(filePath: String): Pom {
        val mapper = XmlMapper()
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return mapper.readValue(File(filePath))
    }
}

data class Pom(val groupId: String, val artifactId: String, val version: String)
