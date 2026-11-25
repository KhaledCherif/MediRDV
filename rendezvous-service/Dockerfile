# syntax=docker/dockerfile:1
# Build context = repo root (see docker-compose.yml), so that contracts/ is available.

# ---- Build stage (JDK + Maven wrapper) ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Copy Maven wrapper and pom.xml first for dependency caching
COPY rendezvous-service/.mvn .mvn
COPY rendezvous-service/mvnw rendezvous-service/pom.xml ./
# pom.xml reads ../contracts (gRPC stubs), i.e. /contracts here
COPY contracts /contracts
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Copy source and build
COPY rendezvous-service/src src
RUN ./mvnw -B -q package -DskipTests

# ---- Runtime stage (JRE only, non-root user) ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 app
COPY --from=build /workspace/target/rdv-core-*.jar app.jar
USER app

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
