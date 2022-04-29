package dev.poolside.gradle.semanticversion

import org.gradle.api.DefaultTask
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.create
import org.w3c.dom.Element
import java.util.concurrent.TimeUnit

abstract class SemanticVersionTask : DefaultTask() {

    private val versions = mutableMapOf<String, String>()

    @TaskAction
    fun setVersion() {
        project.allprojects.forEach { p ->
            p.extensions.getByType(PublishingExtension::class.java).publications.forEach { publication ->
                val pub = publication as MavenPublication
                val (key, version) = findVersion(pub)
                pub.version = version
                versions[key] = version
            }
            p.extensions.getByType(PublishingExtension::class.java).publications.forEach { publication ->
                val pub = publication as MavenPublication
                rewrite(pub)
            }
        }
    }

    private fun findVersion(publication: MavenPublication): Pair<String, String> {
        val dep = project.dependencies.create(group = publication.groupId, name = publication.artifactId, version = "${publication.version}+")
        val conf = project.rootProject.configurations.detachedConfiguration(dep)
        conf.isTransitive = false
        conf.resolutionStrategy.cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
        conf.resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
        val lenientConfiguration = conf.resolvedConfiguration.lenientConfiguration
        val version = if (lenientConfiguration.unresolvedModuleDependencies.isEmpty()) {
            val resolved = conf.resolvedConfiguration.firstLevelModuleDependencies.first()
            val version = resolved.moduleVersion.split(".")
            val patch = version[2].toInt()+1
            val newVersion = "${version[0]}.${version[1]}.$patch"
            logger.lifecycle("Resolved published version of '${resolved.moduleGroup}:${resolved.moduleName}:${resolved.moduleVersion}' to '$newVersion'")
            newVersion
        } else {
            val newVersion = "${publication.version}.0"
            logger.lifecycle("No published version of '${publication.groupId}:${publication.artifactId}:${publication.version}' resolved to '$newVersion'")
            newVersion
        }
        return "${publication.groupId}:${publication.artifactId}" to version
    }

    private fun rewrite(pub: MavenPublication) {
        pub.pom.withXml {
            val root = this.asElement()
            val dependencies = root.getElementsByTagName("dependencies").item(0).childNodes
            var count = 0
            while(count < dependencies.length) {
                if (dependencies.item(count) is Element) {
                    val dep = dependencies.item(count) as Element
                    val group = dep.getElementsByTagName("groupId").item(0).textContent
                    val artifact = dep.getElementsByTagName("artifactId").item(0).textContent
                    val key = "${group}:${artifact}"
                    if (versions.containsKey(key)) {
                        val version = versions[key]
                        dep.getElementsByTagName("version").item(0).textContent = version
                    }
                }
                count++
            }
        }
    }
}