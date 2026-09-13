FROM eclipse-temurin:17-jdk AS dev

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl inotify-tools \
    && rm -rf /var/lib/apt/lists/*
COPY docker/dev-entrypoint.sh /usr/local/bin/dev-entrypoint.sh
RUN chmod +x /usr/local/bin/dev-entrypoint.sh
WORKDIR /workspace
ENTRYPOINT ["/usr/local/bin/dev-entrypoint.sh"]

FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw -B -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package \
    && cp target/backend-0.0.1-SNAPSHOT.jar /workspace/app.jar

FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home /app --shell /usr/sbin/nologin app \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar
RUN chown 10001:10001 /app/app.jar
EXPOSE 8080
USER 10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
