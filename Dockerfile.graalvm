FROM ghcr.io/graalvm/graalvm-community:17 as builder

WORKDIR /app
COPY . /app

RUN ./gradlew nativeTest
RUN ./gradlew nativeCompile

FROM scratch

ARG CI_ENV=noci
ARG GIT_COMMIT=git_commit_undefined
ARG GIT_BRANCH=git_branch_undefined
ARG VERSION=not_versioned

LABEL git_commit $GIT_COMMIT
LABEL git_branch $GIT_BRANCH
LABEL ci_environment $CI_ENV
LABEL version $VERSION

COPY --from=builder /app/build/native/nativeCompile/versioning /versioning

ENTRYPOINT ["./versioning"]