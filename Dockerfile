FROM maven:3.6.3-openjdk-15 as build-stage
RUN mkdir /workdir
WORKDIR /workdir
COPY ./ .
RUN mvn clean compile assembly:single

FROM adoptopenjdk:15-jre-openj9
ARG VERSION=not_versioned
RUN mkdir /app
LABEL version $VERSION
COPY --from=build-stage /workdir/target/*-jar-with-dependencies.jar /app/versioning.jar
ENTRYPOINT ["java", "-jar", "/app/versioning.jar"]