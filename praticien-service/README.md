# MediRDV - Praticien Service

Microservice Spring Boot qui gère les praticiens et leurs créneaux. Il expose une API REST sous `/api/v1/praticiens` et utilise PostgreSQL.

La réservation et la libération d'un créneau ne passent pas par REST : elles seront exposées en gRPC (Suivi 2) avec le contrat `contracts/medirdv.praticien.v1.proto`. La logique atomique correspondante (`CreneauService.reserver` / `liberer`) existe déjà côté service.

## Configuration

La connexion à la base doit être fournie par variables d'environnement. Aucun identifiant n'est fourni par défaut.

| Variable | Valeur locale indicative | Usage |
| --- | --- | --- |
| `SERVER_PORT` | `8082` | Port HTTP (défaut : 8082) |
| `DB_URL` | `jdbc:postgresql://localhost:5432/praticien_db` | URL JDBC ; dans Docker, utiliser le nom DNS du conteneur PostgreSQL défini par l'équipe |
| `DB_USERNAME` | à définir | Utilisateur PostgreSQL |
| `DB_PASSWORD` | à définir | Mot de passe PostgreSQL |
| `JPA_DDL_AUTO` | `update` | Optionnel : stratégie Hibernate de création du schéma |

Dans PowerShell, pour un lancement local :

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/praticien_db'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = '<votre-mot-de-passe-local>'
.\mvnw.cmd spring-boot:run
```

Ne commitez pas de vrais secrets. L'équipe doit injecter ces mêmes variables dans `docker-compose.yml` depuis son fichier `.env` et aligner `DB_URL` sur le nom réel de sa base et du service PostgreSQL.

## Routes

| Méthode | Route | Résultat |
| --- | --- | --- |
| GET | `/api/v1/praticiens` | Liste des praticiens (filtre optionnel `?specialite=`) |
| GET | `/api/v1/praticiens/{id}` | Recherche par identifiant |
| POST | `/api/v1/praticiens` | Création (201, en-tête `Location`) |
| PUT | `/api/v1/praticiens/{id}` | Modification |
| DELETE | `/api/v1/praticiens/{id}` | Suppression (204) |
| GET | `/api/v1/praticiens/{id}/creneaux` | Créneaux d'un praticien (filtres optionnels `statut`, `du`, `au`) |
| POST | `/api/v1/praticiens/{id}/creneaux` | Création d'un créneau (201, en-tête `Location`) |
| GET | `/api/v1/praticiens/{id}/creneaux/{creneauId}` | Détail d'un créneau |
| PUT | `/api/v1/praticiens/{id}/creneaux/{creneauId}` | Modification d'un créneau libre |
| DELETE | `/api/v1/praticiens/{id}/creneaux/{creneauId}` | Suppression d'un créneau libre (204) |
| PUT | `/api/v1/praticiens/{id}/creneaux/{creneauId}/accepter` | Accepter une réservation en attente |
| PUT | `/api/v1/praticiens/{id}/creneaux/{creneauId}/refuser` | Refuser une réservation en attente |

`nom` et `specialite` sont obligatoires pour un praticien ; `debut` et `fin` sont obligatoires pour un créneau et `fin` doit être postérieure à `debut`. Les dates sont au format ISO-8601 sans fuseau (ex. `2026-12-02T09:00:00`).

Les erreurs sont renvoyées dans un corps JSON unique (`timestamp`, `status`, `error`, `message`, `path`, `details`) :

- `400` : validation, JSON mal formé ou paramètre invalide ;
- `404` : praticien ou créneau absent ;
- `409` : créneau qui chevauche un créneau existant du même praticien, ou violation de contrainte.

Exemple de création d'un praticien :

```json
{
  "nom": "Dr Sami Ben Ali",
  "specialite": "Cardiologie"
}
```

Exemple de création d'un créneau :

```json
{
  "debut": "2026-12-02T09:00:00",
  "fin": "2026-12-02T09:30:00"
}
```

Renvoyer ce même créneau pour le même praticien renvoie 409. Ces valeurs sont des exemples de démonstration, pas des données réelles.

Un créneau a le statut `LIBRE` à la création. La réservation gRPC le fait passer à `EN_ATTENTE`. Le praticien peut alors l'accepter (`RESERVE`) ou le refuser (retour à `LIBRE`). Seuls les créneaux `LIBRE` peuvent être modifiés ou supprimés par REST. La libération gRPC remet à `LIBRE` une demande en attente ou un créneau réservé.

## Documentation et supervision

- Swagger UI : `/swagger-ui.html`
- Spécification OpenAPI : `/v3/api-docs`
- Santé : `/actuator/health`
- Informations : `/actuator/info`

Actuator n'expose pas les détails de santé publiquement.

## Tests

```bash
./mvnw verify
```

Les tests utilisent une base H2 en mémoire : ni Docker ni PostgreSQL ne sont nécessaires. Ils couvrent notamment la réservation atomique (20 réservations simultanées d'un même créneau : une seule réussit) et les codes HTTP 201, 400, 404 et 409.

## Docker

```bash
docker build -t praticien-service .
```

Le `Dockerfile` est multi-stage (JDK 17 pour le build, JRE 17 pour l'exécution, utilisateur non-root). Le démarrage complet se fait via le `docker-compose.yml` de l'équipe.
