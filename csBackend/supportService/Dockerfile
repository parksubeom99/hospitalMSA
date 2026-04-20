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

RUN apk add --no-cache tzdata busybox-extras \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8184

# [MODIFIED] -Dspring.profiles.active=prod 제거
# 프로파일은 docker-compose의 SPRING_PROFILES_ACTIVE 환경변수로 주입받음
# (JVM -D 옵션이 환경변수보다 우선순위가 높아 oracle 프로파일이 무시되던 문제 수정)
# 로컬 직접 실행 시: java -Dspring.profiles.active=oracle -jar app.jar 로 명시 필요
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "/app/app.jar"]
