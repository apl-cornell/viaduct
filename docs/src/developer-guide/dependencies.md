# Updating Dependencies

We use a Gradle [plugin](https://github.com/ben-manes/gradle-versions-plugin)
for managing dependencies. Running

```shell
./gradlew dependencyUpdates
```

will give you a report listing outdated dependencies. You can now either manually change
`build.gradle.kts`, or run

```shell
./gradlew useLatestVersions
```

to automatically update all dependencies. To update Gradle itself, run

```shell
./gradlew wrapper --gradle-version <version>
```

where `<version>` comes from the above report.
