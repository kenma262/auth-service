# ---- Build stage ----------------------------------------------------
FROM maven:3.9.7-eclipse-temurin-17 AS build
WORKDIR /app
# Leverage layer caching for dependencies
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
# Copy sources and build
COPY src ./src
RUN mvn -q -DskipTests package

# ---- Runtime stage --------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Non‑root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the Spring Boot fat jar (repackaged). Adjust name if version changes.
ARG JAR_FILE=/app/target/auth-service-1.0-SNAPSHOT.jar
COPY --from=build ${JAR_FILE} app.jar

LABEL org.opencontainers.image.source="https://github.com/kenma262/auth-service" \
      org.opencontainers.image.title="auth-service" \
      org.opencontainers.image.version="1.0-SNAPSHOT"

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0"
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]
