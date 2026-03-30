# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080

# Create the uploads directory and set permissions (optional but good practice)
RUN mkdir -p /app/uploads/documents/
RUN chmod -R 777 /app/uploads/

ENTRYPOINT ["java", "-jar", "app.jar"]
