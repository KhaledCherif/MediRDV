# =====================================================================
# Dockerfile - service assurance-legacy (MediRDV)
# Multi-stage build with non-root user
# =====================================================================

# ---- Build stage (JDK + Maven) ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy sources and build (includes JAXB generation from XSD)
COPY src ./src
RUN mvn package -DskipTests -B -q

# ---- Runtime stage (JRE only, non-root user) ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 app
COPY --from=build /workspace/target/*.jar app.jar
USER app

# Port exposed by Spring Boot (Spring-WS servlet on /ws/*)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
