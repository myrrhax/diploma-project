FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY shared shared
COPY notification-service notification-service

RUN chmod +x ./gradlew
RUN ./gradlew :notification-service:bootJar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /app/notification-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]