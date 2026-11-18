# MediRDV - Patient Service

Microservice Spring Boot qui gère les dossiers patients. Il expose une API REST sous `/api/v1/patients` et utilise PostgreSQL.

## Configuration

La connexion à la base doit être fournie par variables d'environnement. Aucun identifiant n'est fourni par défaut.

| Variable | Valeur locale indicative | Usage |
| --- | --- | --- |
| `SERVER_PORT` | `8081` | Port HTTP |
| `DB_URL` | `jdbc:postgresql://localhost:5432/patient_db` | URL JDBC ; dans Docker, utiliser le nom DNS du conteneur PostgreSQL défini par l'équipe |
| `DB_USERNAME` | à définir | Utilisateur PostgreSQL |
| `DB_PASSWORD` | à définir | Mot de passe PostgreSQL |

Dans PowerShell, pour un lancement local :

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/patient_db'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = '<votre-mot-de-passe-local>'
.\mvnw.cmd spring-boot:run
```

Ne commitez pas de vrais secrets. L'équipe doit injecter ces mêmes variables dans `docker-compose.yml` depuis son fichier `.env` et aligner `DB_URL` sur le nom réel de sa base et du service PostgreSQL.

## Routes

| Méthode | Route | Résultat |
| --- | --- | --- |
| GET | `/api/v1/patients` | Liste des patients |
| GET | `/api/v1/patients/{id}` | Recherche par identifiant |
| GET | `/api/v1/patients/numero-assure/{numeroAssure}` | Recherche par numéro d'assuré |
| POST | `/api/v1/patients` | Création (201, en-tête `Location`) |
| PUT | `/api/v1/patients/{id}` | Modification |
| DELETE | `/api/v1/patients/{id}` | Suppression (204) |

`nom`, `contact` et `numeroAssure` sont obligatoires ; `numeroAssure` doit être unique. Les erreurs de validation renvoient 400, un patient absent 404 et un numéro déjà utilisé 409.

Exemple de création :

```json
{
  "nom": "Amina Ben Ali",
  "contact": "+216 20 000 000",
  "numeroAssure": "ASSURE-DEMO-001"
}
```

Pour illustrer les erreurs, un champ obligatoire vide renvoie 400 ; réutiliser `ASSURE-DEMO-001` sur un autre dossier renvoie 409. Ces valeurs sont des exemples de démonstration, pas des données réelles ni une validation de l'éligibilité auprès de la caisse.

## Documentation et supervision

- Swagger UI : `/swagger-ui.html`
- Spécification OpenAPI : `/v3/api-docs`
- Santé : `/actuator/health`
- Informations : `/actuator/info`

Actuator n'expose pas les détails de santé publiquement.
