FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

# Railway provides $PORT environment variable
ENV SERVER_PORT=$PORT

EXPOSE $PORT

ENTRYPOINT ["java", "-jar", "app.jar"]
