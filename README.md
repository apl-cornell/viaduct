# Viaduct

[![Build Status](https://travis-ci.com/apl-cornell/viaduct.svg?branch=master)](https://travis-ci.com/apl-cornell/viaduct)
[![Codecov](https://codecov.io/gh/apl-cornell/viaduct/branch/master/graph/badge.svg)](https://codecov.io/gh/apl-cornell/viaduct)

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
./viaduct help
```

## Debugging

A common reason why Viaduct fails to build is problems with the classpath.
Viaduct uses several libraries (CUP parser, JFlex, Google AutoValue) that
generate source code, and these need to be added to classpath.

An example `.classpath` file (tested in VS Code) is listed below.

````
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
  <classpathentry kind="src" output="bin/main" path="build/generated/sources/annotationProcessor/java/main">
		<attributes>
			<attribute name="gradle_scope" value="main"/>
			<attribute name="gradle_used_by_scope" value="main,test"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="src" output="bin/main" path="build/generated-src/cup">
		<attributes>
			<attribute name="gradle_scope" value="main"/>
			<attribute name="gradle_used_by_scope" value="main,test"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="src" output="bin/main" path="build/generated-src/jflex">
		<attributes>
			<attribute name="gradle_scope" value="main"/>
			<attribute name="gradle_used_by_scope" value="main,test"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="src" output="bin/main" path="src/main/java">
		<attributes>
			<attribute name="gradle_scope" value="main"/>
			<attribute name="gradle_used_by_scope" value="main,test"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="src" output="bin/test" path="src/test/java">
		<attributes>
			<attribute name="gradle_scope" value="test"/>
			<attribute name="gradle_used_by_scope" value="test"/>
		</attributes>
	</classpathentry>
	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8/"/>
	<classpathentry kind="con" path="org.eclipse.buildship.core.gradleclasspathcontainer"/>
	<classpathentry kind="output" path="bin/default"/>
</classpath>
```
