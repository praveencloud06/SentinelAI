FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/sentinelai-knowledge-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
