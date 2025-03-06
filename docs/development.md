<!-- Generated with https://github.com/thlorenz/doctoc -->
<!-- doctoc docs/development.md >

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [Development](#development)
- [Gradle Plugin Publishing](#gradle-plugin-publishing)
- [References](#references)
  - [General](#general)
  - [pom.withXml](#pomwithxml)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Development

[Gradle docs](https://docs.gradle.org/current/userguide/writing_plugins.html) on writing plugins.

Publish to mavenLocal

```sh
❯ gr publishToMavenLocal

BUILD SUCCESSFUL in 5s
11 actionable tasks: 8 executed, 3 up-to-date
```

Add to `settings.gradle` file in client project and change the version `id 'dev.poolside.gradle.semantic-version' version '1.0.0'`

```groovy
pluginManagement {
  repositories {
    mavenLocal()
  }
}
```

## Gradle Plugin Publishing

* [plugin docs](https://plugins.gradle.org/docs/publish-plugin-new)
* [user account](https://plugins.gradle.org/u/poolside)


It's easier to set up credentials via the login task then doing it manually in the gradle.properties file

```sh
❯ gr login
```

Then run the publishing task:

```sh
❯ gr publishPlugins

> Task :publishPlugins
Publishing plugin dev.poolside.gradle.semantic-version version 0.1.0
Thank you. Your new plugin dev.poolside.gradle.semantic-version has been submitted for approval by Gradle engineers. The request should be processed within the next few days, at which point you will be contacted via email.
Publishing artifact build/publications/pluginMaven/module.json
Publishing artifact build/publications/pluginMaven/pom-default.xml
Publishing artifact build/libs/semantic-version-plugin-0.1.0.jar
Publishing artifact build/libs/semantic-version-plugin-0.1.0-sources.jar
Publishing artifact build/libs/semantic-version-plugin-0.1.0-javadoc.jar
Activating plugin dev.poolside.gradle.semantic-version version 0.1.0

Deprecated Gradle features were used in this build, making it incompatible with Gradle 8.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

See https://docs.gradle.org/7.4.2/userguide/command_line_interface.html#sec:command_line_warnings

BUILD SUCCESSFUL in 5s
10 actionable tasks: 7 executed, 3 up-to-date
```

## References

Information that I found helpful when writing this plugin

### General

* https://github.com/nebula-plugins/nebula-publishing-plugin
  * [MavenResolvedDependenciesPlugin](https://github.com/nebula-plugins/nebula-publishing-plugin/blob/aee3fb093c622e7b7c9eb75cb6dc2838c2bcf340/src/main/groovy/nebula/plugin/publishing/maven/MavenResolvedDependenciesPlugin.groovy)
* [editing pom info](https://stackoverflow.com/questions/20959558/in-gradle-how-can-i-generate-a-pom-file-with-dynamic-dependencies-resolved-to-t)

### pom.withXml

Editing groovy dom nodes/nodelists/etc is a pain in kotlin. I found these links helpful.

* [kotlinx.dom](https://github.com/Kotlin/kotlinx.dom/blob/0fe219d942047468b361dc0594f1c443ebcf26c3/src/main/kotlin/Dom.kt)
* [kotlin-dsl-sample issue](https://github.com/gradle/kotlin-dsl-samples/issues/225)
* [groovy example](https://github.com/nebula-plugins/nebula-publishing-plugin/blob/575b55c72151e0fae35c4aea69ff77ae8db57455/src/main/groovy/nebula/plugin/publishing/maven/MavenRemoveInvalidDependenciesPlugin.groovy)
