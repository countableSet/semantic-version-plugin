package dev.poolside.gradle.semanticversion

import org.gradle.api.Project
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ConfiguredModuleComponentRepository
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.DefaultVersionComparator
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.Version
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser
import org.gradle.api.logging.Logger
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.internal.component.external.model.DefaultModuleComponentSelector
import org.gradle.internal.component.external.model.GradleDependencyMetadata
import org.gradle.internal.resolve.result.DefaultBuildableModuleVersionListingResolveResult
import org.gradle.kotlin.dsl.create
import java.util.Collections

object VersionFinder {

    private val versionParser = VersionParser()
    private val versionComparator = DefaultVersionComparator().asVersionComparator()

    fun findVersion(logger: Logger, project: Project, resolver: ConfiguredModuleComponentRepository, publication: MavenPublication): Pair<String, String> {
        val remote = resolver.remoteAccess
        val local = resolver.localAccess

        val dep = project.dependencies.create(group = publication.groupId, name = publication.artifactId, version = "${publication.version}+")
        val selector = DefaultModuleComponentSelector.newSelector(dep.module, dep.versionConstraint)
        val metadata = GradleDependencyMetadata(selector, Collections.emptyList(), false, false, null, false, null)

        val result = DefaultBuildableModuleVersionListingResolveResult()
        remote.listModuleVersions(metadata, result)
        local.listModuleVersions(metadata, result)

        logger.debug("Resolved versions ${result.versions}")

        var latestVersion: Version? = null
        result.versions.map { version -> versionParser.transform(version) }.forEach { version ->
            if (latestVersion == null || versionComparator.compare(version, latestVersion) > 0) {
                latestVersion = version
            }
        }

        val version = if (latestVersion == null) {
            val newVersion = "${publication.version}.0"
            logger.lifecycle("No published version of '${publication.groupId}:${publication.artifactId}:${publication.version}' resolved to '$newVersion'")
            newVersion
        } else {
            val newVersion = incrementVersion(latestVersion!!)
            logger.lifecycle("Resolved published version of '${publication.groupId}:${publication.artifactId}:${publication.version}' to '$newVersion'")
            newVersion
        }
        return "${publication.groupId}:${publication.artifactId}" to version
    }

    private fun incrementVersion(version: Version): String {
        val parts = version.numericParts.filterNotNull()
        val last = parts.last() + 1
        return parts.dropLast(1).joinToString(".") + ".$last"
    }
}