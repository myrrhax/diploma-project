FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY shared shared
COPY core core

RUN chmod +x ./gradlew
RUN ./gradlew :core:bootJar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ARG PORT
COPY --from=builder /app/core/build/libs/*.jar app.jar
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]