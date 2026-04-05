# Build JAR inside image (Railway has no pre-built build/libs)
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

# Tạm dùng H2 (không cần MySQL) — bật railway profile khi đã kết nối MySQL
# ENV SPRING_PROFILES_ACTIVE=railway

# Railway injects PORT at runtime; Spring reads it via application.properties
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
