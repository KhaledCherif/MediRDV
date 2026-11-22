# assurance-legacy (MediRDV)

Service de gestion des assurances (legacy) du projet MediRDV.
Exposition **SOAP Contract-First** via Spring-WS. Base de données **PostgreSQL uniquement**.

## ⚠️ Base de données : PostgreSQL uniquement

Ce service utilise **PostgreSQL** comme tous les autres services MediRDV.
**Aucune base H2 n'est utilisée** (pas de fichier `.mv_db`, pas de volume H2, pas de propriété `spring.datasource.h2*`).

## Technologies

- Java 17 (Eclipse Temurin)
- Spring Boot 3.2.5
- Spring-WS (SOAP Contract-First)
- Spring Data JPA
- PostgreSQL (pilote JDBC)
- JAXB (génération des classes Java depuis le XSD)
- JUnit 5 + AssertJ (tests unitaires)
- Docker (Dockerfile multi-stage)

## Structure du projet

```
assurance-legacy/
├── Dockerfile                                  # Build multi-etapes (Maven + JRE Temurin 17)
├── pom.xml                                     # Dépendances Maven + plugin JAXB
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/medirdv/assurancelegacy/
│   │   │   ├── AssuranceLegacyApplication.java # @SpringBootApplication
│   │   │   ├── config/
│   │   │   │   └── WebServiceConfig.java       # DefaultWsdl11Definition, /ws/*, XSD
│   │   │   ├── endpoint/
│   │   │   │   └── AssuranceEndpoint.java      # @Endpoint SOAP
│   │   │   ├── model/
│   │   │   │   └── Assurance.java              # Entité JPA (PostgreSQL)
│   │   │   ├── repository/
│   │   │   │   └── AssuranceRepository.java
│   │   │   └── service/
│   │   │       ├── AssuranceService.java
│   │   │       └── AssureInconnuException.java # @SoapFault
│   │   └── resources/
│   │       ├── application.properties          # Config PostgreSQL + actuator
│   │       └── xsd/
│   │           └── assurance.xsd               # Schéma XSD contract-first
│   └── test/
│       └── java/com/medirdv/assurancelegacy/
│           └── AssuranceEndpointTest.java      # Test unitaire SOAP (4 tests)
```

## Schéma XSD (contract-first)

Fichier : `src/main/resources/xsd/assurance.xsd`
Espace de noms : `http://medirdv.com/assurance-legacy`

### Opération : VerifierDroits

**Requête :**
- `numeroAssure` (string)

**Réponse :**
- `couverture` (boolean)
- `taux` (decimal)

### Opération : DeclarerConsultation

**Requête :**
- `numeroAssure` (string)
- `date` (dateTime)
- `praticien` (string)

**Réponse :**
- `accuse` (string)

## Génération JAXB

Le plugin `jaxb2-maven-plugin` génère les classes Java dans le package `com.medirdv.assurancelegacy.gen` à partir du XSD lors de la phase `generate-sources`.

## Endpoint SOAP

L'endpoint est exposé via la servlet `MessageDispatcherServlet` sur l'URL `/ws/*`.

Une fois l'application démarrée, le WSDL est disponible à :

```
http://localhost:8080/ws/assurances.wsdl
```

## Configuration (`application.properties`)

La connexion PostgreSQL utilise des valeurs par défaut surchargeables par variables d'environnement :

| Propriété | Variable d'environnement | Valeur par défaut |
|-----------|-------------------------|-------------------|
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/assurance_legacy` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `assurance` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | `assurance123` |

L'endpoint Actuator `/actuator/health` est exposé pour le monitoring.

## Build et tests

Depuis le dossier `assurance-legacy` :

```bash
# Compilation + génération des sources JAXB
mvn clean compile

# Tests unitaires (4 tests SOAP, aucune base de données requise)
mvn clean test

# Package (jar)
mvn clean package
```

## Docker

Construction de l'image (Dockerfile multi-stage, aucune base de données intégrée) :

```bash
docker build -t assurance-legacy .
```

Le conteneur expose le port `8080` (SOAP sur `/ws/*`). La connexion à PostgreSQL se fait via les variables d'environnement `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

**Aucun `docker-compose.yml` n'est présent dans ce service.** L'orchestration avec PostgreSQL doit être gérée au niveau du projet global MediRDV.
