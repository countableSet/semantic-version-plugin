package dev.poolside.gradle.semanticversion

import org.gradle.api.DefaultTask
import org.gradle.api.internal.artifacts.DependencyManagementServices
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.Version
import org.gradle.api.internal.artifacts.repositories.ResolutionAwareRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskCollection
import org.w3c.dom.Element
import javax.inject.Inject

abstract class SemanticVersionTask : DefaultTask() {

    private val versionRegex = "^\\d+\\.\\d+\$".toRegex()
    private val versions = mutableMapOf<String, String>()

    @Input
    lateinit var extension: PublishingExtension
    @Input
    lateinit var tasks: TaskCollection<PublishToMavenRepository>
    @Input
    var manual: Boolean = false

    @get:Inject
    abstract val dependencyService: DependencyManagementServices

    @TaskAction
    fun setVersion() {
        if (manual) {
            manual()
        } else {
            automatic()
        }
    }

    private fun automatic() {
        extension.publications.forEach { publication ->
            val pub = publication as MavenPublication
            checkVersion(pub.version)
            val fetchedVersions = mutableListOf<Version>()
            extension.repositories.forEach {
                if (it is ResolutionAwareRepository) {
                    val resolver = it.createResolver()
                    val version = VersionFinder.findVersion(logger, dependencyService, resolver, pub)
                    fetchedVersions.add(version)
                }
            }
            val max = VersionFinder.findMaxVersion(fetchedVersions)
            val key = "${pub.groupId}:${pub.artifactId}"
            pub.version = max.toString()
            versions[key] = max.toString()
        }
        extension.publications.forEach { publication ->
            val pub = publication as MavenPublication
            rewrite(pub)
        }
    }

    private fun manual() {
        extension.repositories.forEach {
            if (it is ResolutionAwareRepository) {
                val resolver = it.createResolver()
                extension.publications.forEach { publication ->
                    val pub = publication as MavenPublication
                    val exists = VersionFinder.versionExists(logger, dependencyService, resolver, pub)
                    tasks.configureEach {
                        onlyIf {
                            if (exists) {
                                logger.lifecycle("Resolved published version of '${publication.groupId}:${publication.artifactId}:${publication.version}' already exists")
                            }
                            !exists
                        }
                    }
                }
            }
        }
    }

    private fun checkVersion(version: String) {
        if (!versionRegex.matches(version)) {
            throw IllegalArgumentException("Invalid version, must be in format $versionRegex")
        }
    }

    private fun rewrite(pub: MavenPublication) {
        pub.pom.withXml {
            val root = this.asElement()
            val dependencies = root.getElementsByTagName("dependencies")
            var outerCount = 0
            while(outerCount < dependencies.length) {
                val children = dependencies.item(outerCount).childNodes
                var innerCount = 0
                while(innerCount < children.length) {
                    if (children.item(innerCount) is Element) {
                        val dep = children.item(innerCount) as Element
                        val group = dep.getElementsByTagName("groupId").item(0).textContent
                        val artifact = dep.getElementsByTagName("artifactId").item(0).textContent
                        val key = "${group}:${artifact}"
                        if (versions.containsKey(key)) {
                            val version = versions[key]
                            dep.getElementsByTagName("version").item(0).textContent = version
                        }
                    }
                    innerCount++
                }
                outerCount++
            }
        }
    }
}
