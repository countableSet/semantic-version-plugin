# Semantic Version Gradle Plugin

* [docs](https://github.com/countableSet/semantic-version-plugin/tree/main/docs)
* [gradle plugin portal](https://plugins.gradle.org/plugin/dev.poolside.gradle.semantic-version)

Based on a given `major.minor` version, plugin determines patch version based on what is already maven repository by auto incrementing it to produce the next version number. Major or minor versions must be manually changed.

```groovy
plugins {
    id 'maven-publish'
    id 'dev.poolside.gradle.semantic-version' version '<version>'
}
```
