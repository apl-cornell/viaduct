# Viaduct

[![Build Status](https://travis-ci.com/apl-cornell/viaduct.svg?branch=master)](https://travis-ci.com/apl-cornell/viaduct)
[![Code Coverage](https://codecov.io/gh/apl-cornell/viaduct/branch/master/graph/badge.svg)](https://codecov.io/gh/apl-cornell/viaduct)
[![Docker Build Status](https://img.shields.io/docker/cloud/build/cacay/viaduct)](https://hub.docker.com/repository/docker/cacay/viaduct)

Secure program partitioning.

## Development

We use [Gradle](https://gradle.org/) for builds.
You do not have to install Gradle manually; you only need to have
[Java](https://www.oracle.com/technetwork/java/javase/downloads/index.html) available.

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
