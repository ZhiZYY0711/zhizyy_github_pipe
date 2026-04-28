# syntax=docker/dockerfile:1.7
FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl server-web -am package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod \
    PIPELINE_LOG_DIR=/app/logs

RUN addgroup -S app && adduser -S app -G app && mkdir -p /app/logs && chown -R app:app /app
COPY --from=build /workspace/server-web/target/server-web-*.jar /app/app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
