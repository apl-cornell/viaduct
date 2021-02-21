# Viaduct

[![Build Status](https://github.com/apl-cornell/viaduct/workflows/CI/badge.svg)](https://github.com/apl-cornell/viaduct/actions?query=workflow%3ACI)
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

### ABY

We use [ABY](https://github.com/encryptogroup/ABY) for our multiparty
computation back end.
We have provided a JNI wrapper for ABY and included it as a build dependency
in Gradle, so you don't need to do anything else besides build the compiler
to get it to work.
We have only tested the wrapper for Linux, however.

### libsnark

We use [libsnark](https://github.com/scipr-lab/libsnark) for our zero-knowledge
proofs back end.
You have to manually build the JNI wrapper for libsnark to use the ZKP back end.

First, clone `libfqfft`: https://github.com/scipr-lab/libfqfft.
`libsnark` actually depends on this library, but we have to build it ourselves
because we need to set the `-fPIC` flag manually to get the wrapper to build.
To build, navigate to the root of the repository and run this on your shell:
`mkdir build && cd build && cmake -DCMAKE_POSITION_INDEPENDENT_CODE=ON ..`.
Then run `make` and then `make install` to install the libraries and header
files.

Next, clone `libsnark`: https://github.com/scipr-lab/libsnark.
Run this command in the root of the repository:
`mkdir build && cd build && cmake -DUSE_LINKED_LIBRARIES=ON ..`.
The `cmake` flag makes the build use the install version of `libfqfft` instead
of building it again in the `depends` subrepository.
Then run `make` and then `make install` to install the libraries and header
files.

Finally, build the `libsnark` wrapper.
Navigate to this directory of the `viaduct` repository:
`runtime/src/main/cwrapper`.
Next, inspect the `Makefile` and make sure that the `INCLUDES` variable
points to the JVM directories.
Then inspect the `libsnarkwrapper.so` target and make sure the link directory
(`-L`) points to the directory where `libff` and `libsnark` were installed.
Run `make` to build `libsnarkwrapper.`
Finally, run `make install` to install the wrapper in a directory known by
the JVM to include JNI libraries.
By default the `install` target will copy the wrapper in `/usr/lib/`.


### Logging

We use the [kotlin-logging](https://github.com/MicroUtils/kotlin-logging)
library for showing additional information to the user.
Logs go to standard error in accordance with Unix conventions, and the user can
control the granularity of logs using the `--verbose` flag.

As a general rule, *never* use `print()` or `println()` to display information
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

