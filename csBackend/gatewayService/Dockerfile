# ──────────────────────────────────────────
# Stage 1 : Build
# ──────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

# [ADDED] Gradle 래퍼 + 의존성 캐시 레이어 분리
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon 2>/dev/null || true

COPY src src
RUN ./gradlew bootJar -x test --no-daemon \
    -Pjar.plain=false

# ──────────────────────────────────────────
# Stage 2 : Runtime
# ──────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8180

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dspring.profiles.active=prod", \
  "-jar", "/app/app.jar"]