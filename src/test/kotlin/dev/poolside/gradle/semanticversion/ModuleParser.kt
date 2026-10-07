package dev.poolside.gradle.semanticversion

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

object ModuleParser {
    fun parse(filePath: String): Module {
        val mapper = ObjectMapper()
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return mapper.readValue(File(filePath))
    }
}

data class Module(val component: Component, val variants: List<Variant>)

data class Component(val group: String, val module: String, val version: String)

data class Variant(val name: String, val dependencies: List<ModuleDependency>?)

data class ModuleDependency(val group: String, val module: String, val version: Version)

data class Version(val requires: String)
