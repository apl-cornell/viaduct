ARG JDK_VERSION=11.0.3

# Stage 1 (the build container)
FROM openjdk:${JDK_VERSION}-jdk-slim AS builder
CMD ["/bin/bash"]
WORKDIR /app

## Have Gradle Wrapper download the Gradle binary
COPY gradlew .
COPY gradle gradle
RUN ./gradlew --version

## Have Gradle download all dependencies
COPY *.gradle.kts ./
COPY cli/*.gradle.kts cli/
COPY compiler/*.gradle.kts compiler/
COPY runtime/*.gradle.kts runtime/
COPY shared/*.gradle.kts shared/
COPY test-utilities/*.gradle.kts test-utilities/
RUN ./gradlew assemble

## Build the app
COPY cli cli
COPY compiler compiler
COPY runtime runtime
COPY shared shared
COPY test-utilities test-utilities
RUN ./gradlew :cli:installDist


# Stage 2 (the distribution container)
FROM openjdk:${JDK_VERSION}-jre-slim
WORKDIR /root

RUN apt-get update && apt-get install -y --no-install-recommends \
    bash-completion \
    less \
    make \
    && rm -rf /var/lib/apt/lists/*

## Enable Bash completion
RUN echo source /etc/profile.d/bash_completion.sh >> .bashrc

## Copy application binary
COPY --from=builder /app/cli/build/install /usr/local/
RUN ["ln", "-s", "/usr/local/cli/bin/cli", "/usr/local/bin/viaduct" ]

## Add command-line completion (i.e. tab to autocomplete)
RUN _VIADUCT_COMPLETE=bash viaduct > viaduct-completion.sh
RUN echo source viaduct-completion.sh >> .bashrc

## Copy artifact evaluation
COPY artifact-eval ./
