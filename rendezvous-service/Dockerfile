# syntax=docker/dockerfile:1

# ---- Build stage (JDK + Maven wrapper) ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Copy Maven wrapper and pom.xml first for dependency caching
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Copy source and build
COPY src src
RUN ./mvnw -B -q package -DskipTests

# ---- Runtime stage (JRE only, non-root user) ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 app
COPY --from=build /workspace/target/rdv-core-*.jar app.jar
USER app

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]