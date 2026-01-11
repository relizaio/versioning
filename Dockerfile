FROM ghcr.io/graalvm/graalvm-community:25.0.1@sha256:30bb7c24b18a4f1af194d3858847b16e97ab616ef40f19d552f116a834874aeb as builder

WORKDIR /app
COPY . /app

RUN ./gradlew nativeTest
RUN ./gradlew nativeCompile

FROM gcr.io/distroless/base-debian13:nonroot@sha256:c0d0c9c854a635e57be1d6635e066b076de3b217c7b971b213cea2e5641cc3a0

ARG CI_ENV=noci
ARG GIT_COMMIT=git_commit_undefined
ARG GIT_BRANCH=git_branch_undefined
ARG VERSION=not_versioned

LABEL git_commit $GIT_COMMIT
LABEL git_branch $GIT_BRANCH
LABEL ci_environment $CI_ENV
LABEL version $VERSION

COPY --from=builder /app/build/native/nativeCompile/versioning /versioning

ENTRYPOINT ["/versioning"]