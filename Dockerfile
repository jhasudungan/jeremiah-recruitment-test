# Standard Springboot App Dockerfile
# ---- Build stage: compile and package the jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy pom.xml first so dependencies are cached as their own layer:
# changing only source code won't re-download everything
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests run separately (Testcontainers needs Docker, which isn't available inside a build)
RUN mvn -B -q package -DskipTests

# ---- Runtime stage: only a JRE and the jar ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Don't run as root
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/target/*.jar app.jar
USER app

EXPOSE 8080
# Size the heap from the container's memory limit, not the host's
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]