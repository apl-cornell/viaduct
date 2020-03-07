ARG JDK_VERSION=11.0.3

# Stage 1: build container
FROM openjdk:${JDK_VERSION}-jdk-slim AS builder
WORKDIR /root

## Have Gradle Wrapper download the Gradle binary
COPY gradlew .
COPY gradle gradle
RUN ./gradlew --version

## Have Gradle download all dependencies
COPY *.gradle.kts ./
COPY prettyprinting/*.gradle.kts prettyprinting/
RUN ./gradlew --no-daemon assemble || return 0

## Build the app
COPY . .
RUN ./gradlew --no-daemon build installDist


# Stage 2: distribution container
FROM openjdk:${JDK_VERSION}-jre-slim
WORKDIR /root

## Copy example programs for testing
COPY examples examples

## Add viaduct binary to PATH
COPY --from=builder /root/build/install /usr/local/
RUN ["ln", "-s", "/usr/local/viaduct/bin/viaduct", "/usr/local/bin" ]
