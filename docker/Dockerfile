FROM eclipse-temurin:21
EXPOSE 8000
COPY ./build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]