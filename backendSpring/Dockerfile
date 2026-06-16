# syntax=docker/dockerfile:1

# ---------- Stage 1: build the jar ----------
# Uses Java 17 to match <java.version>17</java.version> in pom.xml.
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom first and pre-download dependencies so this layer is cached
# and rebuilds only run when pom.xml changes (faster incremental builds).
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Now copy sources and build. Tests run in CI, skipped here for a fast image.
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---------- Stage 2: slim runtime ----------
# JRE only (no Maven, no build tools) => much smaller, more secure image.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as a non-root user.
RUN useradd -r -u 1001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

# 8080 = REST API, 5000 = Socket.IO (netty-socketio)
EXPOSE 8080 5000

ENTRYPOINT ["java", "-jar", "app.jar"]
