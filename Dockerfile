# syntax=docker/dockerfile:1
FROM node:24-bookworm-slim AS frontend
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY tsconfig.json app.ts index.html style.css orbit-avatar.svg orbit-avatar-purple.svg THIRD-PARTY-NOTICES.txt ./
COPY scripts/ ./scripts/
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-21 AS backend
WORKDIR /build/server
COPY server/pom.xml ./
COPY server/src/ ./src/
COPY --from=frontend /build/dist/ /build/dist/
RUN mvn -B -ntp package

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 portfolio \
    && useradd --uid 10001 --gid portfolio --no-create-home --shell /usr/sbin/nologin portfolio \
    && mkdir -p /app /data/gallery \
    && chown -R portfolio:portfolio /app /data
WORKDIR /app
COPY --from=backend --chown=portfolio:portfolio /build/server/target/portfolio-server-1.0.0.jar ./portfolio.jar
ENV SPRING_PROFILES_ACTIVE=prod \
    GALLERY_DIRECTORY=/data/gallery \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+ExitOnOutOfMemoryError"
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl --fail --silent --max-time 3 http://127.0.0.1:8080/api/health > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/portfolio.jar"]
