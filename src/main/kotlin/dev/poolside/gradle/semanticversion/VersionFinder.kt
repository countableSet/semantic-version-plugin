package dev.poolside.gradle.semanticversion

import org.gradle.api.internal.artifacts.DependencyManagementServices
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ConfiguredModuleComponentRepository
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.DefaultVersionComparator
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.Version
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser
import org.gradle.api.internal.initialization.StandaloneDomainObjectContext
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

    fun findVersion(logger: Logger, dependencyService: DependencyManagementServices, resolver: ConfiguredModuleComponentRepository, publication: MavenPublication): Pair<String, String> {
        val versions = listVersions(logger, dependencyService, resolver, publication, "${publication.version}+")
        var latestVersion: Version? = null
        versions.map { version -> versionParser.transform(version) }.forEach { version ->
            if (latestVersion == null || versionComparator.compare(version, latestVersion) > 0) {
                latestVersion = version
            }
        }

        val version = if (latestVersion == null) {
            val newVersion = "${publication.version}.0"
            logger.lifecycle("No published version of '${publication.groupId}:${publication.artifactId}:${publication.version}' resolved to '$newVersion'")
            newVersion
        } else {
            val newVersion = compareAndIncrementVersion(versionParser.transform(publication.version), latestVersion!!)
            logger.lifecycle("Resolved published version of '${publication.groupId}:${publication.artifactId}:${publication.version}' to '$newVersion'")
            newVersion
        }
        return "${publication.groupId}:${publication.artifactId}" to version
    }

    fun versionExists(logger: Logger, dependencyService: DependencyManagementServices, resolver: ConfiguredModuleComponentRepository, publication: MavenPublication): Boolean {
        val versions = listVersions(logger, dependencyService, resolver, publication, publication.version)
        return versions.contains(publication.version)
    }

    private fun listVersions(logger: Logger, dependencyService: DependencyManagementServices, resolver: ConfiguredModuleComponentRepository, publication: MavenPublication, versionSearch: String): Set<String> {
        val remote = resolver.remoteAccess
        val local = resolver.localAccess

        val handler = dependencyService.newDetachedResolver(StandaloneDomainObjectContext.ANONYMOUS).dependencyHandler
        val dep = handler.create(group = publication.groupId, name = publication.artifactId, version = versionSearch)
        val selector = DefaultModuleComponentSelector.newSelector(dep.module, dep.versionConstraint)
        val metadata = GradleDependencyMetadata(selector, Collections.emptyList(), false, false, null, false, null)

        val result = DefaultBuildableModuleVersionListingResolveResult()
        remote.listModuleVersions(metadata, result)
        local.listModuleVersions(metadata, result)

        logger.debug("Resolved versions ${result.versions}")

        return result.versions
    }

    private fun compareAndIncrementVersion(original: Version, found: Version): String {
        // major version bump
        if (original.numericParts[0] > found.numericParts[0]) {
            return original.source + ".0"
        }
        // minor version bump
        if (original.numericParts[1] > found.numericParts[1]) {
            return original.source + ".0"
        }
        val parts = found.numericParts.filterNotNull()
        val last = parts.last() + 1
        return parts.dropLast(1).joinToString(".") + ".$last"
    }
}
