FROM relizaio/maven-postgresql as build-stage
RUN mkdir /workdir
WORKDIR /workdir
COPY ./ .
RUN mvn clean compile assembly:single

FROM debian:stable-20191224-slim
ARG VERSION=not_versioned
RUN mkdir -p /usr/share/man/man1 && apt-get update && apt-get install -y --no-install-recommends openjdk-11-jre-headless && mkdir /app
LABEL version $VERSION
COPY --from=build-stage /workdir/target/*-jar-with-dependencies.jar /app/versioning.jar
ENTRYPOINT ["/usr/bin/java", "-jar", "/app/versioning.jar"]