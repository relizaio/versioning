FROM eclipse-temurin:17 as jre-build
# Create a custom Java runtime
RUN $JAVA_HOME/bin/jlink \
         --add-modules java.base,java.desktop \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /javaruntime


FROM maven:3.8.5-eclipse-temurin-17 as build-stage
RUN mkdir /workdir
WORKDIR /workdir
COPY ./ .
RUN mvn clean compile assembly:single

FROM debian:bullseye-slim as artifact-stage
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME

ARG CI_ENV=noci
ARG GIT_COMMIT=git_commit_undefined
ARG GIT_BRANCH=git_branch_undefined
ARG VERSION=not_versioned

LABEL git_commit $GIT_COMMIT
LABEL git_branch $GIT_BRANCH
LABEL ci_environment $CI_ENV
LABEL version $VERSION

RUN mkdir /app

RUN useradd apprunner && chown apprunner:apprunner -R /app
USER apprunner

COPY --from=build-stage /workdir/target/*-jar-with-dependencies.jar /app/versioning.jar

ENTRYPOINT ["java", "-jar", "/app/versioning.jar"]