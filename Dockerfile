# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
COPY .mvn/settings.xml .mvn/settings.xml
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -s .mvn/settings.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 wemove \
    && useradd --uid 10001 --gid wemove --no-create-home --shell /usr/sbin/nologin wemove \
    && mkdir -p /app/data/uploads \
    && chown -R wemove:wemove /app
WORKDIR /app
COPY --from=build --chown=wemove:wemove /build/target/building-block-web-1.0.0.jar app.jar
USER wemove
ENV SERVER_ADDRESS=0.0.0.0 \
    SERVER_PORT=8080 \
    UPLOAD_DIR=/app/data/uploads \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.awt.headless=true"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
    CMD curl --fail --silent --show-error http://127.0.0.1:8080/api/v1/health >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
