####
# This Dockerfile is used to build a Docker image for the Bluesky Raffle application
# It uses a multi-stage build to minimize the final image size
####

## Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the project files
COPY pom.xml ./
COPY src ./src

# Build the application (Maven will download dependencies during build)
RUN mvn clean package -DskipTests -B

## Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built application from the build stage
COPY --from=build /app/target/quarkus-app /app

# Expose the default Quarkus port
EXPOSE 8080

# Set the entrypoint to run the application
# Environment variables can be passed at runtime:
# - bluesky.identifier: Your Bluesky handle or email
# - bluesky.password: Your Bluesky app password
ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
