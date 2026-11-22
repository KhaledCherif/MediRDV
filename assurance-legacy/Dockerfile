# =====================================================================
# Dockerfile - service assurance-legacy (MediRDV)

# =====================================================================

# --- Etape 1 : build de l'application ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache des dependances Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copie des sources et compilation (inclut la generation JAXB depuis le XSD)
COPY src ./src
RUN mvn package -DskipTests -B -q

# --- Etape 2 : execution (image finale allégée) ---
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copie du jar produit a l'etape de build
COPY --from=build /app/target/*.jar app.jar

# Port expose par Spring Boot (Servlet Spring-WS sur /ws/*)
EXPOSE 8080

# Point d'entree
ENTRYPOINT ["java", "-jar", "app.jar"]
