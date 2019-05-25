FROM debian:stable-20190506-slim
RUN mkdir -p /usr/share/man/man1 && apt-get update && apt-get install -y --no-install-recommends openjdk-8-jre-headless && mkdir /app
COPY target/*-jar-with-dependencies.jar /app/versioning.jar
ENTRYPOINT ["/usr/bin/java", "-jar", "/app/versioning.jar"]