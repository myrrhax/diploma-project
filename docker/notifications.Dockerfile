FROM eclipse-temurin:21
EXPOSE 8001
COPY ./notification-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]