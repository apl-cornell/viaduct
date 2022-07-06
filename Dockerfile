# syntax=docker/dockerfile:1.2

ARG APP=viaduct
ARG JDK_VERSION=11.0.10

# Build stage
FROM openjdk:${JDK_VERSION}-jdk-slim AS builder
CMD ["/bin/bash"]
WORKDIR /app

## Have Gradle Wrapper download the Gradle binary
COPY gradlew .
COPY gradle gradle
COPY gradle.properties .
RUN ./gradlew --version

## Create fake .git directory for palantir/gradle-git-version
RUN mkdir .git

## Build the app
COPY *.gradle.kts ./
COPY cli cli
COPY compiler compiler
COPY interpreter interpreter
COPY runtime runtime
COPY shared shared
COPY test-utilities test-utilities
RUN --mount=type=cache,target=/root/.gradle/caches ./gradlew :cli:installDist


# Collect source files
FROM busybox AS source
WORKDIR /root/source

## Copy source code and remove build files
COPY --from=builder /app .
RUN find . -name build -exec rm -rf {} +
RUN rm -r ./.gradle
COPY viaduct .


# Distribution stage
#FROM openjdk:${JDK_VERSION}-jre-slim
FROM openjdk:${JDK_VERSION}-jdk-slim
CMD ["/bin/bash"]
WORKDIR /root

ARG APP

RUN apt-get update && apt-get install -y --no-install-recommends \
    bash-completion \
    iproute2 \
    less \
    make \
    nano \
    python3 \
    tmux \
    && rm -rf /var/lib/apt/lists/*

## Enable Bash completion
RUN echo source /etc/profile.d/bash_completion.sh >> .bashrc

## Copy application binary
COPY --from=builder /app/cli/build/install/cli /usr/local/${APP}
RUN ln -s /usr/local/${APP}/bin/cli /usr/local/bin/${APP}

## Add command-line completion (i.e. tab to autocomplete)
RUN _VIADUCT_COMPLETE=bash ${APP} > /etc/profile.d/${APP}-completion.sh
RUN echo source /etc/profile.d/${APP}-completion.sh >> .bashrc

## Copy benchmarks
COPY benchmarks ./
COPY LICENSE .

## Copy Gradle cache to make build self contained
# COPY --from=builder /root/.gradle/caches /root/.gradle/caches
# COPY --from=builder /root/.gradle/wrapper /root/.gradle/wrapper

## Copy application code to allow building from source
COPY --from=source /root/source source
