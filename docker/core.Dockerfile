FROM eclipse-temurin:21
EXPOSE 8000
COPY ./core/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]