# Use an OpenJDK base image
FROM eclipse-temurin:11-jre

WORKDIR /app

# Copy built jar (the jar will be created by mvn package)
COPY target/repo-risk-analyzer-backend.jar /app/repo-risk-analyzer-backend.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/repo-risk-analyzer-backend.jar"]
