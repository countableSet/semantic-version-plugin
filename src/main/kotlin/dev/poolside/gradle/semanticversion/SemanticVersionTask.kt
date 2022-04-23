package dev.poolside.gradle.semanticversion

import org.gradle.api.DefaultTask
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.create
import java.util.concurrent.TimeUnit

abstract class SemanticVersionTask : DefaultTask() {

    @TaskAction
    fun setVersion() {
        val publication = project.extensions.getByType(PublishingExtension::class.java).publications.first() as MavenPublication
        val dep = project.dependencies.create(group = publication.groupId, name = publication.artifactId, version = "#{publication.version}+")
        val conf = project.rootProject.configurations.detachedConfiguration(dep)
        conf.isTransitive = false
        conf.resolutionStrategy.cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
        conf.resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
        val lenientConfiguration = conf.resolvedConfiguration.lenientConfiguration
        if (lenientConfiguration.unresolvedModuleDependencies.isEmpty()) {
            val resolved = conf.resolvedConfiguration.firstLevelModuleDependencies.first()
            logger.lifecycle("Resolved published version of '${resolved.moduleGroup}:${resolved.moduleName}' to '${resolved.moduleVersion}'")
            val version = resolved.moduleVersion.split(".")
            val patch = version[2].toInt()+1
            project.version = "${version[0]}.${version[1]}.$patch"
        } else {
            logger.lifecycle("No published version of '${publication.groupId}:${publication.artifactId}' resolved to '${publication.version}.0'")
            project.version = "${publication.version}.0"
        }
        logger.lifecycle("project version ${project.version}")
    }
}