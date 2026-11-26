# syntax=docker/dockerfile:1

# ---- Étape 1 : build (JDK + Maven wrapper) ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# On copie d'abord pom + wrapper : la couche de dépendances est mise en cache
# tant que le pom.xml ne change pas.
COPY praticien-service/.mvn .mvn
COPY praticien-service/mvnw praticien-service/pom.xml ./
COPY contracts /contracts
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY praticien-service/src src
RUN ./mvnw -B -q package -DskipTests

# ---- Étape 2 : exécution (JRE seul, utilisateur non-root) ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 app
COPY --from=build /workspace/target/praticien-service-*.jar app.jar
USER app

EXPOSE 8082 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
