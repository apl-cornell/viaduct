# Viaduct

[![Build Status](https://travis-ci.com/apl-cornell/viaduct.svg?branch=master)](https://travis-ci.com/apl-cornell/viaduct)
[![Code Coverage](https://codecov.io/gh/apl-cornell/viaduct/branch/master/graph/badge.svg)](https://codecov.io/gh/apl-cornell/viaduct)
[![Docker Build Status](https://img.shields.io/docker/cloud/build/cacay/viaduct)](https://hub.docker.com/repository/docker/cacay/viaduct)

Secure program partitioning.

## Development

We use [Gradle](https://gradle.org/) for builds.
You do not have to install Gradle manually; you only need to have
[Java](https://www.oracle.com/technetwork/java/javase/downloads/index.html)
available.

Once you have Java installed, just run

```shell
./gradlew build
```

to build the code. This will also run all tests, so if this command works,
you are good to go.

On Unix environments, you can run the compiler using `./viaduct` from project
root. This will use Gradle to automatically rebuild the application as
necessary, so you do not have to worry about calling `./gradlew build` each
time you change something. To start, try

```shell
./viaduct --help
```

### Logging

We use the [kotlin-logging](https://github.com/MicroUtils/kotlin-logging)
library for showing additional information to the user.
Logs go to standard error in accordance with Unix conventions, and the user can
control the granularity of logs using the `--verbose` flag.

As a general rule, _never_ use `print()` or `println()` to display information
to the user. This includes showing information to yourself for debugging.
All logging frameworks have a `DEGUB` level, and if you found this information
useful, chances are it will be relevant later.

Logging is extremely easy to use.
See [this section](https://github.com/MicroUtils/kotlin-logging#getting-started).

### Updating Dependencies

We use a Gradle [plugin](https://github.com/ben-manes/gradle-versions-plugin)
for managing dependencies. Running

```shell
./gradlew dependencyUpdates
```

will give you a report listing outdated dependencies.
You can now either manually change the Gradle
[configuration file](build.gradle.kts), or run

```shell
./gradlew useLatestVersions
```

to automatically update all dependencies.
To update Gradle itself, run

```shell
./gradlew wrapper --gradle-version <version>
```

where `<version>` comes from the above report.
