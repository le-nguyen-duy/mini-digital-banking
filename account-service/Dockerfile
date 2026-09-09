# Multi-stage build for account-service.
# Build context MUST be the repository root (see docker-compose.yml),
# because this is a multi-module Gradle project: the root settings.gradle
# references all 4 modules (account-service, transaction-service,
# notification-service, common), and account-service depends on :common.

# ---- Stage 1: build with Gradle ----
FROM gradle:8.13-jdk21-alpine AS build
WORKDIR /workspace

# Optional build-time proxy for environments without direct internet access
# (e.g. corporate network). Pass with:
#   docker build --build-arg HTTP_PROXY_HOST=... --build-arg HTTP_PROXY_PORT=...
ARG HTTP_PROXY_HOST=""
ARG HTTP_PROXY_PORT=""
ENV GRADLE_OPTS="-Dhttp.proxyHost=${HTTP_PROXY_HOST} -Dhttp.proxyPort=${HTTP_PROXY_PORT} -Dhttps.proxyHost=${HTTP_PROXY_HOST} -Dhttps.proxyPort=${HTTP_PROXY_PORT}"

COPY . .

RUN gradle :account-service:bootJar --no-daemon -x test

# ---- Stage 2: lightweight runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /workspace/account-service/build/libs/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
