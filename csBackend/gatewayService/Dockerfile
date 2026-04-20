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

# [MODIFIED] -Dspring.profiles.active=prod 제거
# gatewayService는 DB 미사용으로 프로파일 영향 없으나 일관성을 위해 동일 패턴 적용
# 프로파일은 docker-compose의 SPRING_PROFILES_ACTIVE 환경변수로 주입받음
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "/app/app.jar"]
