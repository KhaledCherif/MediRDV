# Build the Spring Boot application with Java 17.
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /workspace

# Copy build descriptors first so Docker can reuse dependency layers.
COPY pom.xml mvnw ./
COPY .mvn/ .mvn/
RUN chmod +x mvnw

COPY src/ src/
COPY openapi.yaml openapi.yaml
RUN ./mvnw -B -DskipTests package

# Run the application with a smaller Java 17 image and a non-root user.
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app
RUN apk add --no-cache curl
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build --chown=spring:spring /workspace/target/*.jar /app/app.jar

USER spring
EXPOSE 8081
ENV SERVER_PORT=8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
