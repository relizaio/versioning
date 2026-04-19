FROM ghcr.io/graalvm/graalvm-community:25.0.2@sha256:7eeb80438dcda5edfcc58e804ce919018d2bf40ef61ddbb555936a8ba2a216aa AS builder

WORKDIR /app
COPY . /app

RUN ./gradlew nativeTest
RUN ./gradlew nativeCompile

FROM gcr.io/distroless/base-debian13:nonroot@sha256:fb282f8ed3057f71dbfe3ea0f5fa7e961415dafe4761c23948a9d4628c6166fe AS runner

ARG CI_ENV=noci
ARG GIT_COMMIT=git_commit_undefined
ARG GIT_BRANCH=git_branch_undefined
ARG VERSION=not_versioned

LABEL git_commit=$GIT_COMMIT
LABEL git_branch=$GIT_BRANCH
LABEL ci_environment=$CI_ENV
LABEL org.opencontainers.image.version=$VERSION
LABEL org.opencontainers.image.vendor="Reliza Incorporated"
LABEL org.opencontainers.image.title="Reliza Versioning"
LABEL org.opencontainers.image.source="https://github.com/relizaio/versioning"
LABEL org.opencontainers.image.license="MIT"
LABEL org.opencontainers.image.base.name="docker.io/relizaio/versioning"

COPY --from=builder /app/build/native/nativeCompile/versioning /versioning

ENTRYPOINT ["/versioning"]